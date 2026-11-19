-- Three ways to start a series (2026-11-19-three-ways-to-start): a room (online, at once), a challenge by
-- nickname (the other player may be offline) and matchmaking (a random opponent, at once, against the defense
-- decks). This migration adds what the first two need and what matchmaking needs to skip players who left.
--
-- Codes are shown to players: `rnd-XXXX-XXXX` for a room, `ch-XXXX-XXXX` for a challenge. The 8 characters come
-- from 31 symbols with no 0, 1, I, L or O. A code is only a name: it never gives access to anything, the player in
-- the JWT does.

-- Last time the player logged in, for the "not offline for more than 7 days" rule of matchmaking.
-- Updated by a plain UPDATE at login, without touching users.version (that one guards credits).
ALTER TABLE users
    ADD COLUMN last_login_at timestamptz;

-- A challenge now has a code, and the faction its challenger plays in game 1 (the opponent has the opposite one;
-- the factions swap every game). Both are filled by the backend: they are nullable only so that rows made before
-- this migration stay valid.
ALTER TABLE challenges
    ADD COLUMN code                varchar(13),
    ADD COLUMN challenger_faction  varchar(24),
    ADD CONSTRAINT challenges_code_format CHECK (code ~ '^(ch|rnd)-[2-9A-HJKMNP-Z]{4}-[2-9A-HJKMNP-Z]{4}$'),
    ADD CONSTRAINT challenges_challenger_faction_check CHECK (challenger_faction IN ('RESISTENZA_UMANA', 'POTENZE_ARTIFICIALI'));
CREATE UNIQUE INDEX challenges_code_uq ON challenges (code);

-- One pending challenge at a time from one player to another: no spam.
CREATE UNIQUE INDEX challenges_one_pending_per_pair_uq ON challenges (challenger_id, opponent_id) WHERE status = 'PENDING';

-- Open rooms. The creator locks one deck per faction when opening the room (`decks`: an array of
-- {faction, pool_snapshot_id, deck}, the same snapshots a challenge keeps in challenge_decks). Whoever joins
-- chooses own decks; at that moment a challenge is created already ACCEPTED (kind CHALLENGE), with a code
-- `rnd-...`, the decks are copied into challenge_decks, and the room becomes MATCHED.
CREATE TABLE rooms
(
    id               uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    code             varchar(13) NOT NULL UNIQUE CHECK (code ~ '^rnd-[2-9A-HJKMNP-Z]{4}-[2-9A-HJKMNP-Z]{4}$'),
    creator_id       uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    -- the faction the creator plays in game 1
    creator_faction  varchar(24) NOT NULL CHECK (creator_faction IN ('RESISTENZA_UMANA', 'POTENZE_ARTIFICIALI')),
    decks            jsonb       NOT NULL,
    status           varchar(10) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'MATCHED', 'EXPIRED', 'CANCELLED')),
    challenge_id     uuid UNIQUE REFERENCES challenges (id) ON DELETE RESTRICT,
    created_at       timestamptz NOT NULL DEFAULT now(),
    expires_at       timestamptz NOT NULL,
    resolved_at      timestamptz,
    version          bigint      NOT NULL DEFAULT 0,
    CHECK (expires_at > created_at),
    CHECK ((status = 'OPEN') = (resolved_at IS NULL)),
    CHECK ((status = 'MATCHED') = (challenge_id IS NOT NULL))
);
-- A player has at most one open room, and the list of open rooms reads by expiry.
CREATE UNIQUE INDEX rooms_one_open_per_creator_uq ON rooms (creator_id) WHERE status = 'OPEN';
CREATE INDEX rooms_open_idx ON rooms (expires_at) WHERE status = 'OPEN';
