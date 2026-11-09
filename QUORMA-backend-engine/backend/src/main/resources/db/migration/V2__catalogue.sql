-- Catalogue and versioning.
--   pool_snapshots   : immutable, hashed, one per faction (each faction has its own pool).
--   cards            : stable identity of a card (never changes across balance versions).
--   card_definitions : versioned content (stats / abilities) of a card inside one snapshot.
-- Abilities are opaque JSON for the backend: the engine owns their meaning. Every kind of card (creature, spell,
-- equipment) writes them the same way: an ordered list (2026-11-07-cards-share-abilities).
-- Suit and rank use the engine's own values (2026-11-08-db-uses-engine-names): suit is the name of the
-- quorma.gameengine.hand.Suit constant, rank is the number 1..13 (Ace = 1, Jack = 11, Queen = 12, King = 13).

CREATE TABLE pool_snapshots
(
    id              uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    faction         varchar(24) NOT NULL CHECK (faction IN ('Resistenza Umana', 'Potenze Artificiali')),
    version_label   varchar(64) NOT NULL,
    ruleset_version varchar(32) NOT NULL,
    content_hash    varchar(64) NOT NULL UNIQUE CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    created_at      timestamptz NOT NULL DEFAULT now(),
    UNIQUE (faction, version_label),
    UNIQUE (id, faction)
);

CREATE TABLE cards
(
    id         uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    card_code  varchar(64) NOT NULL UNIQUE,
    faction    varchar(24) NOT NULL CHECK (faction IN ('Resistenza Umana', 'Potenze Artificiali')),
    kind       varchar(12) NOT NULL CHECK (kind IN ('CREATURE', 'SPELL', 'EQUIPMENT')),
    subfaction varchar(24),
    suit       varchar(16),
    rank       smallint,
    created_at timestamptz NOT NULL DEFAULT now(),
    -- Deck uniqueness key: one creature per suit and rank, one copy of each spell / equipment.
    deck_slot  varchar(96) GENERATED ALWAYS AS (
        CASE WHEN kind = 'CREATURE' THEN 'C:' || suit || '/' || rank::text ELSE 'N:' || card_code END) STORED NOT NULL,
    CONSTRAINT cards_kind_shape CHECK (
        (kind = 'CREATURE' AND subfaction IS NOT NULL AND suit IS NOT NULL AND rank IS NOT NULL)
            OR (kind <> 'CREATURE' AND subfaction IS NULL AND suit IS NULL AND rank IS NULL)),
    CONSTRAINT cards_suit_valid CHECK (suit IS NULL OR suit IN ('VINCOLO', 'FRATTURA', 'SPIRA', 'VESTIGIO')),
    CONSTRAINT cards_rank_valid CHECK (rank IS NULL OR rank BETWEEN 1 AND 13),
    CONSTRAINT cards_subfaction_matches_faction CHECK (subfaction IS NULL
        OR (faction = 'Resistenza Umana' AND subfaction IN ('Adattati', 'Spezzasigilli', 'Portanome', 'Segnaterra'))
        OR (faction = 'Potenze Artificiali' AND subfaction IN ('SEKTAC', 'MEROZYN', 'EFFRIX', 'YLMOR'))),
    UNIQUE (id, faction, kind),
    UNIQUE (id, faction, deck_slot)
);

CREATE TABLE card_definitions
(
    id          uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    snapshot_id uuid         NOT NULL,
    card_id     uuid         NOT NULL,
    faction     varchar(24)  NOT NULL,
    kind        varchar(12)  NOT NULL,
    name        varchar(100) NOT NULL,
    atk         integer,
    def         integer,
    abilities   jsonb        NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(abilities) = 'array'),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    FOREIGN KEY (snapshot_id, faction) REFERENCES pool_snapshots (id, faction),
    FOREIGN KEY (card_id, faction, kind) REFERENCES cards (id, faction, kind),
    UNIQUE (snapshot_id, card_id),
    CONSTRAINT card_definitions_kind_shape CHECK (
        (kind = 'CREATURE' AND atk IS NOT NULL AND atk >= 0 AND def IS NOT NULL AND def > 0)
            OR (kind <> 'CREATURE' AND atk IS NULL AND def IS NULL))
);
CREATE INDEX card_definitions_card_idx ON card_definitions (card_id);

CREATE TRIGGER pool_snapshots_immutable
    BEFORE UPDATE OR DELETE
    ON pool_snapshots
    FOR EACH ROW
EXECUTE FUNCTION forbid_update_delete();
CREATE TRIGGER cards_immutable
    BEFORE UPDATE OR DELETE
    ON cards
    FOR EACH ROW
EXECUTE FUNCTION forbid_update_delete();
CREATE TRIGGER card_definitions_immutable
    BEFORE UPDATE OR DELETE
    ON card_definitions
    FOR EACH ROW
EXECUTE FUNCTION forbid_update_delete();

-- A snapshot is sealed once the transaction that created it ends: its hash would no longer match.
CREATE FUNCTION forbid_late_definition() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF (SELECT created_at FROM pool_snapshots WHERE id = NEW.snapshot_id) <> now() THEN
        RAISE EXCEPTION 'pool snapshot % is sealed: definitions must be inserted in the transaction that creates it',
            NEW.snapshot_id;
    END IF;
    RETURN NEW;
END
$$;
CREATE TRIGGER card_definitions_sealed
    BEFORE INSERT
    ON card_definitions
    FOR EACH ROW
EXECUTE FUNCTION forbid_late_definition();
