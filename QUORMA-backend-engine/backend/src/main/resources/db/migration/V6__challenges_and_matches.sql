-- Challenges, match series and matches.
--
-- Ways to start: CHALLENGE (a player picks an opponent, who accepts, choosing decks, or declines) and
-- MATCHMAKING (the system picks an opponent and plays at once against that player's defense decks,
-- even if they are offline; there is no challenge row).
--
-- A series is the unit that is started. It holds one or more games (`matches`):
--   SINGLE       : one game.
--   FACTION_SWAP : one faction each (opposite factions), then swapped for game 2; further games
--                  swap again every time. How many games are needed, and which factions game 3 uses,
--                  is decided by the backend, so the schema only records each game.
-- In a series, player A is always the one who started it (challenger / attacker), player B the other.
-- The faction a player used in a game is the faction of that game's pool snapshot.
--
-- Matches store the replay recipe, not the log: deck snapshots, seed, ruleset / pool / engine versions,
-- PRNG + shuffle names and the SHA-256 of the result.

CREATE TABLE challenges
(
    id            uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    challenger_id uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    opponent_id   uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    format        varchar(16) NOT NULL CHECK (format IN ('SINGLE', 'FACTION_SWAP')),
    status        varchar(12) NOT NULL DEFAULT 'PENDING' CHECK (status IN
                                                                 ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED',
                                                                  'EXPIRED')),
    created_at    timestamptz NOT NULL DEFAULT now(),
    expires_at    timestamptz NOT NULL,
    resolved_at   timestamptz,
    version       bigint      NOT NULL DEFAULT 0,
    CHECK (challenger_id <> opponent_id),
    CHECK (expires_at > created_at),
    CHECK ((status = 'PENDING') = (resolved_at IS NULL))
);
CREATE INDEX challenges_challenger_idx ON challenges (challenger_id, status);
CREATE INDEX challenges_opponent_idx ON challenges (opponent_id, status);

-- Deck snapshots locked by a challenge: the challenger's at creation, the opponent's at acceptance.
-- SINGLE needs one deck per side, FACTION_SWAP one per side and faction (checked by the backend).
CREATE TABLE challenge_decks
(
    challenge_id     uuid        NOT NULL REFERENCES challenges (id) ON DELETE CASCADE,
    side             varchar(10) NOT NULL CHECK (side IN ('CHALLENGER', 'OPPONENT')),
    faction          varchar(24) NOT NULL,
    pool_snapshot_id uuid        NOT NULL,
    deck             jsonb       NOT NULL,
    PRIMARY KEY (challenge_id, side, faction),
    FOREIGN KEY (pool_snapshot_id, faction) REFERENCES pool_snapshots (id, faction)
);

-- A SINGLE challenge locks exactly one deck per side (FACTION_SWAP: one per side and faction).
CREATE FUNCTION challenge_decks_check_format() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF (SELECT format FROM challenges WHERE id = NEW.challenge_id) = 'SINGLE'
        AND EXISTS (SELECT 1 FROM challenge_decks WHERE challenge_id = NEW.challenge_id AND side = NEW.side) THEN
        RAISE EXCEPTION 'a SINGLE challenge takes one deck per side (challenge %, side %)', NEW.challenge_id, NEW.side;
    END IF;
    RETURN NEW;
END
$$;
CREATE TRIGGER challenge_decks_check_format
    BEFORE INSERT
    ON challenge_decks
    FOR EACH ROW
EXECUTE FUNCTION challenge_decks_check_format();

CREATE TABLE match_series
(
    id           uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    kind         varchar(12) NOT NULL CHECK (kind IN ('CHALLENGE', 'MATCHMAKING')),
    format       varchar(16) NOT NULL CHECK (format IN ('SINGLE', 'FACTION_SWAP')),
    challenge_id uuid UNIQUE REFERENCES challenges (id) ON DELETE RESTRICT,
    player_a_id  uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    player_b_id  uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    status       varchar(12) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'FINISHED')),
    outcome      varchar(8) CHECK (outcome IN ('A_WIN', 'B_WIN', 'DRAW')),
    created_at   timestamptz NOT NULL DEFAULT now(),
    finished_at  timestamptz,
    version      bigint      NOT NULL DEFAULT 0,
    CHECK (player_a_id <> player_b_id),
    CHECK ((kind = 'CHALLENGE') = (challenge_id IS NOT NULL)),
    CHECK ((status = 'FINISHED') = (outcome IS NOT NULL)),
    CHECK ((status = 'FINISHED') = (finished_at IS NOT NULL)),
    UNIQUE (id, player_a_id, player_b_id)
);
CREATE INDEX match_series_player_a_idx ON match_series (player_a_id, created_at);
CREATE INDEX match_series_player_b_idx ON match_series (player_b_id, created_at);

CREATE TABLE matches
(
    id                 uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    series_id          uuid        NOT NULL,
    game_number        integer     NOT NULL CHECK (game_number >= 1),
    player_a_id        uuid        NOT NULL,
    player_b_id        uuid        NOT NULL,
    deck_a             jsonb       NOT NULL,
    deck_b             jsonb       NOT NULL,
    pool_snapshot_a_id uuid        NOT NULL REFERENCES pool_snapshots (id),
    pool_snapshot_b_id uuid        NOT NULL REFERENCES pool_snapshots (id),
    seed               bigint      NOT NULL,
    ruleset_version    varchar(32) NOT NULL,
    ruleset_hash       varchar(64) NOT NULL CHECK (ruleset_hash ~ '^[0-9a-f]{64}$'),
    engine_version     varchar(32) NOT NULL,
    prng_name          varchar(32) NOT NULL,
    shuffle_algorithm  varchar(32) NOT NULL,
    result_hash        varchar(64) NOT NULL CHECK (result_hash ~ '^[0-9a-f]{64}$'),
    outcome            varchar(8)  NOT NULL CHECK (outcome IN ('A_WIN', 'B_WIN', 'DRAW')),
    played_at          timestamptz NOT NULL DEFAULT now(),
    UNIQUE (series_id, game_number),
    -- the players of a game are the players of its series
    FOREIGN KEY (series_id, player_a_id, player_b_id) REFERENCES match_series (id, player_a_id, player_b_id)
        ON DELETE RESTRICT
);
CREATE INDEX matches_player_a_idx ON matches (player_a_id, played_at);
CREATE INDEX matches_player_b_idx ON matches (player_b_id, played_at);

-- A SINGLE series has exactly one game. FACTION_SWAP has no fixed length (decided by the backend).
CREATE FUNCTION matches_check_series_format() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF NEW.game_number > 1 AND (SELECT format FROM match_series WHERE id = NEW.series_id) = 'SINGLE' THEN
        RAISE EXCEPTION 'series % is SINGLE: it has only game 1', NEW.series_id;
    END IF;
    RETURN NEW;
END
$$;
CREATE TRIGGER matches_check_series_format
    BEFORE INSERT
    ON matches
    FOR EACH ROW
EXECUTE FUNCTION matches_check_series_format();

CREATE TRIGGER matches_immutable
    BEFORE UPDATE OR DELETE
    ON matches
    FOR EACH ROW
EXECUTE FUNCTION forbid_update_delete();

-- Notification flag: one row per player of every match (both players, always).
-- A player who receives the result immediately (e.g. the attacker, in the HTTP response) gets the row
-- created already read (seen_at set); anyone else (offline defender, challenger whose challenge was
-- accepted later) gets it unread and receives it at the next login. Unread = seen_at IS NULL.
-- matches itself stays immutable, so the flag lives here.
CREATE TABLE match_notifications
(
    match_id   uuid        NOT NULL REFERENCES matches (id) ON DELETE RESTRICT,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    seen_at    timestamptz,
    PRIMARY KEY (match_id, user_id)
);
CREATE INDEX match_notifications_unseen_idx ON match_notifications (user_id, created_at) WHERE seen_at IS NULL;

-- Only the two players of a match can be notified about it.
CREATE FUNCTION match_notifications_check_player() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF NOT EXISTS (SELECT 1
                   FROM matches
                   WHERE id = NEW.match_id
                     AND NEW.user_id IN (player_a_id, player_b_id)) THEN
        RAISE EXCEPTION 'user % did not play match %', NEW.user_id, NEW.match_id;
    END IF;
    RETURN NEW;
END
$$;
CREATE TRIGGER match_notifications_check_player
    BEFORE INSERT
    ON match_notifications
    FOR EACH ROW
EXECUTE FUNCTION match_notifications_check_player();
