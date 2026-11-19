-- Catalogue and versioning.
--   pool_snapshots   : immutable, hashed, one per faction (each faction has its own pool).
--   cards            : stable identity of a card (never changes across balance versions).
--   card_definitions : versioned content (stats / abilities) of a card inside one snapshot. Only what changes a match:
--                      no name, no text.
--   card_texts, card_sets, card_set_cards, card_set_texts : everything commercial or textual. Mutable, outside the
--                      snapshot hash: changing them never creates a snapshot and never touches a replay.
--   active_snapshots : which snapshot of each faction is the active one (not "the newest": a re-activated old one is).
--   catalogue_drafts, draft_items, draft_bases, snapshot_publications : the publishing pipeline (draft, validate,
--                      publish) and its audit trail. Nothing writes a snapshot any other way.
-- Abilities are opaque JSON for the backend: the engine owns their meaning. Every kind of card (creature, spell,
-- equipment) writes them the same way: an ordered list (2026-11-07-cards-share-abilities).
-- Suit and rank use the engine's own values (2026-11-08-db-uses-engine-names): suit is the name of the
-- quorma.gameengine.hand.Suit constant, rank is the number 1..13 (Ace = 1, Jack = 11, Queen = 12, King = 13).
-- Faction and subfaction are stored the same way: the constant names of the engine enums (RESISTENZA_UMANA, ADATTATI).

CREATE TABLE pool_snapshots
(
    id              uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    faction         varchar(24) NOT NULL CHECK (faction IN ('RESISTENZA_UMANA', 'POTENZE_ARTIFICIALI')),
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
    faction    varchar(24) NOT NULL CHECK (faction IN ('RESISTENZA_UMANA', 'POTENZE_ARTIFICIALI')),
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
        OR (faction = 'RESISTENZA_UMANA' AND subfaction IN ('ADATTATI', 'SPEZZASIGILLI', 'PORTANOME', 'SEGNATERRA'))
        OR (faction = 'POTENZE_ARTIFICIALI' AND subfaction IN ('SEKTAC', 'MEROZYN', 'EFFRIX', 'YLMOR'))),
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

-- ---------------------------------------------------------------------------------------------------------------------
-- Card text. Mutable, outside the snapshot hash. Italian ('it') is the fallback locale and is required for every card.
-- Locale is a language tag as the Accept-Language header writes it: 'it', 'en', 'pt-BR'.
-- ---------------------------------------------------------------------------------------------------------------------
CREATE TABLE card_texts
(
    card_id    uuid         NOT NULL REFERENCES cards (id) ON DELETE RESTRICT,
    locale     varchar(8)   NOT NULL CHECK (locale ~ '^[a-z]{2}(-[A-Z]{2})?$'),
    name       varchar(100) NOT NULL CHECK (btrim(name) <> ''),
    flavour    text CHECK (flavour IS NULL OR btrim(flavour) <> ''),
    updated_at timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (card_id, locale)
);
-- Two cards of one language never share a name (a deck list must tell them apart).
CREATE UNIQUE INDEX card_texts_name_uq ON card_texts (locale, lower(name));

-- A card cannot exist without its Italian text: checked when the transaction that creates the card ends, so the card
-- and its text may be inserted in either order.
CREATE FUNCTION require_italian_card_text() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM card_texts WHERE card_id = NEW.id AND locale = 'it') THEN
        RAISE EXCEPTION 'card % has no Italian text (card_texts, locale it)', NEW.card_code;
    END IF;
    RETURN NULL;
END
$$;
CREATE CONSTRAINT TRIGGER cards_require_italian_text
    AFTER INSERT
    ON cards
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
EXECUTE FUNCTION require_italian_card_text();

-- ---------------------------------------------------------------------------------------------------------------------
-- Card sets. status: BOZZA (being prepared and testable, not sold), IN_VENDITA (sold), CHIUSA (no longer sold, its cards
-- stay in the game). A card may belong to several sets (reprints). Taking a card out of sale means removing its
-- card_set_cards row: the card stays in the snapshot, in decks and in collections.
-- ---------------------------------------------------------------------------------------------------------------------
CREATE TABLE card_sets
(
    id           uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    code         varchar(32) NOT NULL UNIQUE CHECK (code ~ '^[A-Z0-9_]+$'),
    status       varchar(12) NOT NULL DEFAULT 'BOZZA' CHECK (status IN ('BOZZA', 'IN_VENDITA', 'CHIUSA')),
    release_date date,
    close_date   date,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    CHECK (close_date IS NULL OR release_date IS NULL OR close_date >= release_date)
);

CREATE TABLE card_set_texts
(
    set_id       uuid         NOT NULL REFERENCES card_sets (id) ON DELETE CASCADE,
    locale       varchar(8)   NOT NULL CHECK (locale ~ '^[a-z]{2}(-[A-Z]{2})?$'),
    name         varchar(100) NOT NULL CHECK (btrim(name) <> ''),
    description  text CHECK (description IS NULL OR btrim(description) <> ''),
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (set_id, locale)
);

CREATE TABLE card_set_cards
(
    card_id      uuid        NOT NULL REFERENCES cards (id) ON DELETE RESTRICT,
    set_id       uuid        NOT NULL REFERENCES card_sets (id) ON DELETE RESTRICT,
    created_at   timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (card_id, set_id)
);
CREATE INDEX card_set_cards_set_idx ON card_set_cards (set_id);

-- ---------------------------------------------------------------------------------------------------------------------
-- Active snapshot of each faction. The only mutable row of the snapshot machinery: publishing moves it (to a new
-- snapshot, or back to an old one with the same content hash).
-- ---------------------------------------------------------------------------------------------------------------------
CREATE TABLE active_snapshots
(
    faction      varchar(24) PRIMARY KEY CHECK (faction IN ('RESISTENZA_UMANA', 'POTENZE_ARTIFICIALI')),
    snapshot_id  uuid        NOT NULL,
    activated_at timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (snapshot_id, faction) REFERENCES pool_snapshots (id, faction) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------------------------------------------------------
-- Publishing pipeline. One open draft at a time (the partial unique index). A draft is filled by the API, a batch file
-- or the bootstrap from set files (source 'seed'), then validated, previewed and published.
--   draft_bases : the snapshot each faction had active when the draft was opened (null = none yet). Publishing fails if
--                 the active one has moved since: the draft is recomputed.
--   draft_items : what the draft changes. item_type CARD (engine fields + sets), TEXT (card or set text) or
--                 SET (a new set with its texts); item_key identifies the item inside the draft, so adding
--                 it twice replaces it. payload is JSON that the backend stores and passes on.
-- ---------------------------------------------------------------------------------------------------------------------
CREATE TABLE catalogue_drafts
(
    id         uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    status     varchar(12) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'PUBLISHED', 'DISCARDED')),
    source     varchar(8)  NOT NULL CHECK (source IN ('seed', 'api', 'batch')),
    note       text,
    created_by uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    closed_at  timestamptz,
    CHECK ((status = 'OPEN') = (closed_at IS NULL))
);
CREATE UNIQUE INDEX catalogue_drafts_one_open_uq ON catalogue_drafts ((true)) WHERE status = 'OPEN';

CREATE TABLE draft_bases
(
    draft_id    uuid        NOT NULL REFERENCES catalogue_drafts (id) ON DELETE CASCADE,
    faction     varchar(24) NOT NULL CHECK (faction IN ('RESISTENZA_UMANA', 'POTENZE_ARTIFICIALI')),
    snapshot_id uuid,
    PRIMARY KEY (draft_id, faction),
    FOREIGN KEY (snapshot_id, faction) REFERENCES pool_snapshots (id, faction) ON DELETE RESTRICT
);

CREATE TABLE draft_items
(
    id         uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    draft_id   uuid         NOT NULL REFERENCES catalogue_drafts (id) ON DELETE CASCADE,
    item_type  varchar(12)  NOT NULL CHECK (item_type IN ('CARD', 'TEXT', 'SET')),
    item_key   varchar(160) NOT NULL,
    payload    jsonb        NOT NULL CHECK (jsonb_typeof(payload) = 'object'),
    created_at timestamptz  NOT NULL DEFAULT now(),
    UNIQUE (draft_id, item_type, item_key)
);

-- Audit trail: one row per faction and publish, append-only. A re-activation (the candidate's hash already existed) is
-- a publish too: reactivated = true and snapshot_id is the old snapshot.
CREATE TABLE snapshot_publications
(
    id           uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    snapshot_id  uuid        NOT NULL REFERENCES pool_snapshots (id) ON DELETE RESTRICT,
    draft_id     uuid REFERENCES catalogue_drafts (id) ON DELETE RESTRICT,
    based_on_id  uuid REFERENCES pool_snapshots (id) ON DELETE RESTRICT,
    source       varchar(8)  NOT NULL CHECK (source IN ('seed', 'api', 'batch')),
    reactivated  boolean     NOT NULL DEFAULT false,
    published_by uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    note         text,
    published_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX snapshot_publications_snapshot_idx ON snapshot_publications (snapshot_id, published_at);

CREATE TRIGGER snapshot_publications_immutable
    BEFORE UPDATE OR DELETE
    ON snapshot_publications
    FOR EACH ROW
EXECUTE FUNCTION forbid_update_delete();
