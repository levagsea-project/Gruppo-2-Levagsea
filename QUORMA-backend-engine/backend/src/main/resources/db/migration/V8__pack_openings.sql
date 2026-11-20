-- Pack types: one row per kind of pack. A pack type has exactly one set; its pool is the definitions of the active
-- snapshot of the faction that belong to that set (card_set_cards). A pack type is on sale while its set is
-- IN_VENDITA. Price and slots are placeholders until balanced (pack-odds).
--   slots_tier_N : number of creatures of Tier N (1: ranks 2-6, 2: 7-10, 3: J/Q/K, 4: Ace); support_slots: extra cards
--   that are a spell or an equipment (no Tier). A pack holds at least one card.
CREATE TABLE pack_types
(
    code          varchar(32) PRIMARY KEY CHECK (code ~ '^[a-z0-9_]+$'),
    set_id        uuid        NOT NULL REFERENCES card_sets (id) ON DELETE RESTRICT,
    price         bigint      NOT NULL CHECK (price > 0),
    slots_tier_1  smallint    NOT NULL DEFAULT 0 CHECK (slots_tier_1 >= 0),
    slots_tier_2  smallint    NOT NULL DEFAULT 0 CHECK (slots_tier_2 >= 0),
    slots_tier_3  smallint    NOT NULL DEFAULT 0 CHECK (slots_tier_3 >= 0),
    slots_tier_4  smallint    NOT NULL DEFAULT 0 CHECK (slots_tier_4 >= 0),
    support_slots smallint    NOT NULL DEFAULT 0 CHECK (support_slots >= 0),
    featured      boolean     NOT NULL DEFAULT false,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    CHECK (slots_tier_1 + slots_tier_2 + slots_tier_3 + slots_tier_4 + support_slots >= 1),
    -- Lets pack_openings tie a purchase to the set the type had.
    UNIQUE (code, set_id)
);

-- Pack openings: one row per pack bought. Append-only.
-- id is the pack id: card_instances.origin_ref and credit_transactions.reference_id of that purchase point to it.
-- idempotency_key is chosen by the client and is only meaningful together with user_id: a retry of the same
-- purchase sends the same key and gets the same pack back instead of buying a second one.

CREATE TABLE pack_openings
(
    id              uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id         uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    idempotency_key uuid        NOT NULL,
    pack_type       varchar(32) NOT NULL,
    set_id          uuid        NOT NULL,
    faction         varchar(24) NOT NULL,
    snapshot_id     uuid        NOT NULL,
    price           bigint      NOT NULL CHECK (price > 0),
    credits_after   bigint      NOT NULL CHECK (credits_after >= 0),
    created_at      timestamptz NOT NULL DEFAULT now(),
    -- The snapshot must be of the pack's faction.
    FOREIGN KEY (snapshot_id, faction) REFERENCES pool_snapshots (id, faction) ON DELETE RESTRICT,
    -- The set is the pack type's own: a type cannot change set once packs were sold (create a new type).
    FOREIGN KEY (pack_type, set_id) REFERENCES pack_types (code, set_id) ON DELETE RESTRICT,
    -- One pack per user and key: defence against a double charge.
    CONSTRAINT pack_openings_user_key_uq UNIQUE (user_id, idempotency_key)
);

CREATE TRIGGER pack_openings_immutable
    BEFORE UPDATE OR DELETE
    ON pack_openings
    FOR EACH ROW
EXECUTE FUNCTION forbid_update_delete();
