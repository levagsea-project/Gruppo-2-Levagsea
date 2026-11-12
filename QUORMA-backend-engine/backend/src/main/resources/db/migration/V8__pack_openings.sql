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
    faction         varchar(24) NOT NULL,
    snapshot_id     uuid        NOT NULL,
    price           bigint      NOT NULL CHECK (price > 0),
    credits_after   bigint      NOT NULL CHECK (credits_after >= 0),
    created_at      timestamptz NOT NULL DEFAULT now(),
    -- The snapshot must be of the pack's faction.
    FOREIGN KEY (snapshot_id, faction) REFERENCES pool_snapshots (id, faction) ON DELETE RESTRICT,
    -- One pack per user and key: defence against a double charge.
    CONSTRAINT pack_openings_user_key_uq UNIQUE (user_id, idempotency_key)
);

CREATE TRIGGER pack_openings_immutable
    BEFORE UPDATE OR DELETE
    ON pack_openings
    FOR EACH ROW
EXECUTE FUNCTION forbid_update_delete();
