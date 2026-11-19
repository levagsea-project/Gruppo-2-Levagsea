package quorma.backend.match;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;
import quorma.backend.deck.DeckEligibility;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Rooms: a player opens one and waits online, another joins it and the series is played at once. The creator's decks
 * are locked when the room opens. Joining turns the room into a challenge that is already ACCEPTED (same code,
 * {@code rnd-...}), so the history shows it like any other series. Rooms that nobody joins expire.
 */
@Service
public class RoomService {

    /** The challenge made from a room never waits, but its row needs a future expiry. */
    private static final Duration ROOM_CHALLENGE_EXPIRY = Duration.ofDays(1);

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final RoomRepository rooms;
    private final ChallengeRepository challenges;
    private final ChallengeDeckRepository challengeDecks;
    private final DeckEligibility eligibility;
    private final DeckSnapshotService snapshots;
    private final SeriesRunner runner;
    private final SeriesViewService views;
    private final MatchSeriesRepository seriesRepository;
    private final MatchProperties props;

    public RoomService(RoomRepository rooms, ChallengeRepository challenges, ChallengeDeckRepository challengeDecks,
                       DeckEligibility eligibility, DeckSnapshotService snapshots, SeriesRunner runner,
                       SeriesViewService views, MatchSeriesRepository seriesRepository, MatchProperties props) {
        this.rooms = rooms;
        this.challenges = challenges;
        this.challengeDecks = challengeDecks;
        this.eligibility = eligibility;
        this.snapshots = snapshots;
        this.runner = runner;
        this.views = views;
        this.seriesRepository = seriesRepository;
        this.props = props;
    }

    /** Opens a room and locks the creator's decks. One open room per player; an expired one is closed first. */
    @Transactional(noRollbackFor = ChallengeException.class)
    public RoomResponse open(UUID creatorId, OpenRoomRequest req) {
        eligibility.requireCanPlay(creatorId);
        rooms.findByCreatorIdAndStatus(creatorId, RoomStatus.OPEN).ifPresent(existing -> {
            if (existing.getExpiresAt().isAfter(Instant.now())) {
                throw new ChallengeException(HttpStatus.CONFLICT, ErrorCodes.ROOM_ALREADY_OPEN);
            }
            close(existing, RoomStatus.EXPIRED);
        });
        Map<Faction, LockedDeck> mine = snapshots.lockChoices(creatorId, req.decks());

        String code = CodeGenerator.unique(CodeGenerator.ROOM_PREFIX, rooms::existsByCode);
        Room room = new Room(code, creatorId, req.startingFaction(), decksJson(mine), Instant.now().plus(props.roomTtl()));
        try {
            room = rooms.saveAndFlush(room); // the unique index allows one open room per creator
        } catch (DataIntegrityViolationException e) {
            throw new ChallengeException(HttpStatus.CONFLICT, ErrorCodes.ROOM_ALREADY_OPEN);
        }
        return response(room);
    }

    /** The open rooms I could join: not mine, not expired, oldest first. */
    @Transactional(readOnly = true)
    public List<RoomResponse> list(UUID userId) {
        List<Room> open = rooms.findJoinable(userId, Instant.now());
        Map<UUID, UserRef> refs = views.refs(open.stream().map(Room::getCreatorId).distinct().toList());
        return open.stream().map(r -> response(r, refs.get(r.getCreatorId()))).toList();
    }

    /**
     * The last room I opened, with what became of it: still open, expired, closed, or MATCHED with the series that
     * was played (the creator was waiting and did not get the response). 404 if I never opened one.
     */
    @Transactional(readOnly = true)
    public RoomResponse mine(UUID creatorId) {
        Room room = rooms.findFirstByCreatorIdOrderByCreatedAtDesc(creatorId)
                .orElseThrow(() -> new ChallengeException(HttpStatus.NOT_FOUND, ErrorCodes.ROOM_NOT_FOUND));
        RoomStatus shown = room.getStatus() == RoomStatus.OPEN && !room.getExpiresAt().isAfter(Instant.now())
                ? RoomStatus.EXPIRED : room.getStatus();
        UUID seriesId = room.getStatus() == RoomStatus.MATCHED && room.getChallengeId() != null
                ? seriesRepository.findByChallengeId(room.getChallengeId()).map(MatchSeries::getId).orElse(null) : null;
        UserRef creator = views.refs(List.of(creatorId)).get(creatorId);
        return new RoomResponse(room.getId(), room.getCode(), creator, room.getCreatorFaction(), shown,
                room.getCreatedAt(), room.getExpiresAt(), seriesId);
    }

    /** The creator closes their room. */
    @Transactional(noRollbackFor = ChallengeException.class)
    public RoomResponse cancel(UUID creatorId, String code) {
        Room room = rooms.findByCodeForUpdate(code)
                .filter(r -> r.getCreatorId().equals(creatorId))
                .orElseThrow(() -> new ChallengeException(HttpStatus.NOT_FOUND, ErrorCodes.ROOM_NOT_FOUND));
        requireOpen(room);
        return response(close(room, RoomStatus.CANCELLED));
    }

    /**
     * Joins a room: I pick my decks and the series is played now. The room row is locked, so two players joining at
     * the same moment are served one after the other and the second finds the room taken. An engine fault rolls
     * everything back and the room stays open.
     */
    @Transactional(noRollbackFor = ChallengeException.class)
    public SeriesResponse join(UUID joinerId, String code, List<DeckChoice> choices) {
        Room room = rooms.findByCodeForUpdate(code)
                .orElseThrow(() -> new ChallengeException(HttpStatus.NOT_FOUND, ErrorCodes.ROOM_NOT_FOUND));
        requireOpen(room);
        if (room.getCreatorId().equals(joinerId)) {
            throw new ChallengeException(HttpStatus.BAD_REQUEST, ErrorCodes.ROOM_OWN_ROOM);
        }
        eligibility.requireCanPlay(joinerId);
        Map<Faction, LockedDeck> mine = snapshots.lockChoices(joinerId, choices);
        Map<Faction, LockedDeck> theirs = decksOf(room);

        Challenge challenge = new Challenge(room.getCreatorId(), joinerId, room.getCode(), room.getCreatorFaction(),
                SeriesFormat.FACTION_SWAP, ChallengeStatus.ACCEPTED, Instant.now().plus(ROOM_CHALLENGE_EXPIRY));
        challenge.setResolvedAt(Instant.now());
        challenge = challenges.saveAndFlush(challenge);
        saveDecks(challenge.getId(), ChallengeSide.CHALLENGER, theirs);
        saveDecks(challenge.getId(), ChallengeSide.OPPONENT, mine);

        SeriesRunner.SeriesResult result = runner.run(SeriesKind.CHALLENGE, challenge.getId(), room.getCreatorId(),
                joinerId, Faction.valueOf(room.getCreatorFaction()), theirs, mine, joinerId);

        room.setStatus(RoomStatus.MATCHED);
        room.setChallengeId(challenge.getId());
        room.setResolvedAt(Instant.now());
        rooms.saveAndFlush(room);

        Map<UUID, JsonNode> logs = result.games().stream()
                .collect(Collectors.toMap(g -> g.match().getId(), SeriesRunner.PlayedMatch::log));
        return views.view(joinerId, result.series(), result.games().stream().map(SeriesRunner.PlayedMatch::match).toList(), logs);
    }

    // -- internals ---------------------------------------------------------------------------------------

    /** 409 unless OPEN; an open room past its expiry is marked EXPIRED (kept) and refused. */
    private void requireOpen(Room room) {
        if (room.getStatus() != RoomStatus.OPEN) {
            throw new ChallengeException(HttpStatus.CONFLICT, ErrorCodes.ROOM_NOT_OPEN);
        }
        if (!room.getExpiresAt().isAfter(Instant.now())) {
            close(room, RoomStatus.EXPIRED);
            throw new ChallengeException(HttpStatus.CONFLICT, ErrorCodes.ROOM_EXPIRED);
        }
    }

    private Room close(Room room, RoomStatus status) {
        room.setStatus(status);
        room.setResolvedAt(Instant.now());
        return rooms.saveAndFlush(room);
    }

    private void saveDecks(UUID challengeId, ChallengeSide side, Map<Faction, LockedDeck> decks) {
        decks.forEach((faction, d) -> challengeDecks.save(new ChallengeDeck(
                new ChallengeDeckId(challengeId, side.name(), faction.name()), d.poolSnapshotId(), d.deck())));
        challengeDecks.flush();
    }

    /** The room's {@code decks} column: an array of {@code {faction, pool_snapshot_id, deck}}. */
    private static String decksJson(Map<Faction, LockedDeck> decks) {
        ArrayNode array = MAPPER.createArrayNode();
        decks.forEach((faction, d) -> {
            ObjectNode node = array.addObject();
            node.put("faction", faction.name());
            node.put("pool_snapshot_id", d.poolSnapshotId().toString());
            node.set("deck", MAPPER.readTree(d.deck()));
        });
        return MAPPER.writeValueAsString(array);
    }

    private static Map<Faction, LockedDeck> decksOf(Room room) {
        Map<Faction, LockedDeck> map = new EnumMap<>(Faction.class);
        for (JsonNode node : MAPPER.readTree(room.getDecks())) {
            Faction faction = Faction.valueOf(node.path("faction").asString());
            map.put(faction, new LockedDeck(faction, UUID.fromString(node.path("pool_snapshot_id").asString()),
                    MAPPER.writeValueAsString(node.path("deck"))));
        }
        return map;
    }

    private RoomResponse response(Room room) {
        return response(room, views.refs(List.of(room.getCreatorId())).get(room.getCreatorId()));
    }

    private static RoomResponse response(Room r, UserRef creator) {
        return new RoomResponse(r.getId(), r.getCode(), creator, r.getCreatorFaction(), r.getStatus(), r.getCreatedAt(),
                r.getExpiresAt(), null);
    }
}
