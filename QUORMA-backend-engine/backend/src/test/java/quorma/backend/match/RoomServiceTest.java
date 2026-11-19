package quorma.backend.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;
import quorma.backend.deck.DeckEligibility;
import quorma.backend.engine.EngineUnavailableException;

/** Rooms: opening, joining, two players joining together, expiry and who may close a room. */
class RoomServiceTest {

    private final RoomRepository rooms = mock(RoomRepository.class);
    private final ChallengeRepository challenges = mock(ChallengeRepository.class);
    private final ChallengeDeckRepository challengeDecks = mock(ChallengeDeckRepository.class);
    private final DeckEligibility eligibility = mock(DeckEligibility.class);
    private final DeckSnapshotService snapshots = mock(DeckSnapshotService.class);
    private final SeriesRunner runner = mock(SeriesRunner.class);
    private final SeriesViewService views = mock(SeriesViewService.class);
    private final RoomService service = new RoomService(rooms, challenges, challengeDecks, eligibility, snapshots,
            runner, views, new MatchProperties(null, null, null));

    private final UUID creatorId = UUID.randomUUID();
    private final UUID joinerId = UUID.randomUUID();
    private final List<DeckChoice> choices = List.of(
            new DeckChoice("RESISTENZA_UMANA", UUID.randomUUID()), new DeckChoice("POTENZE_ARTIFICIALI", UUID.randomUUID()));
    private static final String CODE = "rnd-ABCD-EFGH";

    private static final String STORED_DECKS = "[{\"faction\":\"RESISTENZA_UMANA\",\"pool_snapshot_id\":\""
            + UUID.randomUUID() + "\",\"deck\":[{\"id\":\"x\"}]},{\"faction\":\"POTENZE_ARTIFICIALI\",\"pool_snapshot_id\":\""
            + UUID.randomUUID() + "\",\"deck\":[{\"id\":\"y\"}]}]";

    private Room room(Instant expiresAt) {
        return new Room(CODE, creatorId, "RESISTENZA_UMANA", STORED_DECKS, expiresAt);
    }

    @BeforeEach
    void wire() {
        when(rooms.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(challenges.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(views.refs(anyCollection())).thenReturn(Map.of(creatorId, new UserRef(creatorId, "luca")));
        Map<Faction, LockedDeck> decks = new EnumMap<>(Faction.class);
        for (Faction f : Faction.values()) {
            decks.put(f, new LockedDeck(f, UUID.randomUUID(), "[{\"id\":\"z\"}]"));
        }
        when(snapshots.lockChoices(any(), any())).thenReturn(decks);
    }

    private void roomExists(Room r) {
        when(rooms.findByCodeForUpdate(CODE)).thenReturn(Optional.of(r));
    }

    @Test
    void openingARoomLocksTheDecksAndGivesItACode() {
        RoomResponse r = service.open(creatorId, new OpenRoomRequest("RESISTENZA_UMANA", choices));

        assertEquals(RoomStatus.OPEN, r.status());
        assertNotNull(r.code());
        org.junit.jupiter.api.Assertions.assertTrue(r.code().matches("^rnd-[2-9A-HJKMNP-Z]{4}-[2-9A-HJKMNP-Z]{4}$"), r.code());
        assertEquals("RESISTENZA_UMANA", r.creatorFaction());
        verify(eligibility).requireCanPlay(creatorId);
    }

    @Test
    void aSecondOpenRoomIsRefusedButAnExpiredOneIsReplaced() {
        Room stillOpen = room(Instant.now().plus(Duration.ofMinutes(5)));
        when(rooms.findByCreatorIdAndStatus(creatorId, RoomStatus.OPEN)).thenReturn(Optional.of(stillOpen));
        ChallengeException e = assertThrows(ChallengeException.class,
                () -> service.open(creatorId, new OpenRoomRequest("RESISTENZA_UMANA", choices)));
        assertEquals(ErrorCodes.ROOM_ALREADY_OPEN, e.code());

        Room old = room(Instant.now().minusSeconds(30));
        when(rooms.findByCreatorIdAndStatus(creatorId, RoomStatus.OPEN)).thenReturn(Optional.of(old));
        service.open(creatorId, new OpenRoomRequest("RESISTENZA_UMANA", choices));
        assertEquals(RoomStatus.EXPIRED, old.getStatus());
    }

    @Test
    void joiningPlaysTheSeriesAndTurnsTheRoomIntoAnAcceptedChallenge() {
        Room r = room(Instant.now().plus(Duration.ofMinutes(5)));
        roomExists(r);
        when(runner.run(any(), any(), any(), any(), any(), anyMap(), anyMap(), any()))
                .thenReturn(new SeriesRunner.SeriesResult(mock(MatchSeries.class), List.of()));

        service.join(joinerId, CODE, choices);

        assertEquals(RoomStatus.MATCHED, r.getStatus());
        assertNotNull(r.getResolvedAt());
        verify(runner).run(eq(SeriesKind.CHALLENGE), any(), eq(creatorId), eq(joinerId), eq(Faction.RESISTENZA_UMANA),
                anyMap(), anyMap(), eq(joinerId));
        verify(challenges).saveAndFlush(org.mockito.ArgumentMatchers.argThat(c ->
                c.getStatus() == ChallengeStatus.ACCEPTED && CODE.equals(c.getCode()) && c.getResolvedAt() != null
                        && c.getChallengerId().equals(creatorId) && c.getOpponentId().equals(joinerId)));
    }

    @Test
    void theSecondPlayerWhoJoinsTheSameRoomFindsItTaken() {
        Room r = room(Instant.now().plus(Duration.ofMinutes(5)));
        r.setStatus(RoomStatus.MATCHED);
        roomExists(r);

        ChallengeException e = assertThrows(ChallengeException.class, () -> service.join(joinerId, CODE, choices));

        assertEquals(HttpStatus.CONFLICT, e.status());
        assertEquals(ErrorCodes.ROOM_NOT_OPEN, e.code());
        verify(runner, never()).run(any(), any(), any(), any(), any(), anyMap(), anyMap(), any());
    }

    @Test
    void youCannotJoinYourOwnRoom() {
        roomExists(room(Instant.now().plus(Duration.ofMinutes(5))));

        ChallengeException e = assertThrows(ChallengeException.class, () -> service.join(creatorId, CODE, choices));

        assertEquals(ErrorCodes.ROOM_OWN_ROOM, e.code());
    }

    @Test
    void anExpiredRoomIsMarkedExpiredAndRefused() {
        Room r = room(Instant.now().minusSeconds(1));
        roomExists(r);

        ChallengeException e = assertThrows(ChallengeException.class, () -> service.join(joinerId, CODE, choices));

        assertEquals(ErrorCodes.ROOM_EXPIRED, e.code());
        assertEquals(RoomStatus.EXPIRED, r.getStatus());
    }

    @Test
    void anUnknownCodeIsNotFound() {
        when(rooms.findByCodeForUpdate("rnd-NOPE-NOPE")).thenReturn(Optional.empty());

        ChallengeException e = assertThrows(ChallengeException.class, () -> service.join(joinerId, "rnd-NOPE-NOPE", choices));

        assertEquals(HttpStatus.NOT_FOUND, e.status());
        assertEquals(ErrorCodes.ROOM_NOT_FOUND, e.code());
    }

    @Test
    void anEngineFaultLeavesTheRoomOpen() {
        Room r = room(Instant.now().plus(Duration.ofMinutes(5)));
        roomExists(r);
        when(runner.run(any(), any(), any(), any(), any(), anyMap(), anyMap(), any()))
                .thenThrow(new EngineUnavailableException("down"));

        assertThrows(EngineUnavailableException.class, () -> service.join(joinerId, CODE, choices));

        assertEquals(RoomStatus.OPEN, r.getStatus());
        verify(rooms, never()).saveAndFlush(any());
    }

    @Test
    void onlyTheCreatorClosesTheRoom() {
        Room r = room(Instant.now().plus(Duration.ofMinutes(5)));
        roomExists(r);

        assertEquals(ErrorCodes.ROOM_NOT_FOUND,
                assertThrows(ChallengeException.class, () -> service.cancel(joinerId, CODE)).code());
        assertEquals(RoomStatus.CANCELLED, service.cancel(creatorId, CODE).status());
    }

    @Test
    void theListComesFromTheJoinableRoomsOnly() {
        when(rooms.findJoinable(eq(joinerId), any())).thenReturn(List.of(room(Instant.now().plus(Duration.ofMinutes(5)))));

        List<RoomResponse> list = service.list(joinerId);

        assertEquals(1, list.size());
        assertEquals("luca", list.get(0).creator().username());
    }
}
