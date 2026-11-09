-- Users and economy.
-- users.version backs JPA @Version (optimistic locking). credit_transactions is an append-only ledger.

-- Shared guard for append-only tables (ledger, snapshots, matches, ...).
CREATE FUNCTION forbid_update_delete() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION '% on % is not allowed: the table is immutable', TG_OP, TG_TABLE_NAME;
END
$$;

CREATE TABLE users
(
    id            uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    username      varchar(32)  NOT NULL CHECK (username ~ '^[A-Za-z0-9_.-]{3,32}$'),
    email         varchar(254) NOT NULL CHECK (position('@' IN email) > 1),
    password_hash varchar(100) NOT NULL,
    role          varchar(16)  NOT NULL DEFAULT 'PLAYER' CHECK (role IN ('PLAYER', 'ADMIN')),
    credits       bigint       NOT NULL DEFAULT 0 CHECK (credits >= 0),
    version       bigint       NOT NULL DEFAULT 0,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    -- Admins manage the game but do not play it: they never hold a credit balance.
    CONSTRAINT users_admin_has_no_credits CHECK (role = 'PLAYER' OR credits = 0)
);
CREATE UNIQUE INDEX users_username_lower_uq ON users (lower(username));
CREATE UNIQUE INDEX users_email_lower_uq ON users (lower(email));

CREATE TABLE credit_transactions
(
    id             uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id        uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    amount         bigint      NOT NULL CHECK (amount <> 0),
    balance_after  bigint      NOT NULL CHECK (balance_after >= 0),
    type           varchar(24) NOT NULL CHECK (type IN
                                                ('REGISTRATION_BONUS', 'PACK_PURCHASE', 'TRADE', 'MATCH_REWARD',
                                                 'ADJUSTMENT')),
    reference_type varchar(32),
    reference_id   uuid,
    created_at     timestamptz NOT NULL DEFAULT now(),
    CHECK ((reference_type IS NULL) = (reference_id IS NULL))
);
CREATE INDEX credit_transactions_user_idx ON credit_transactions (user_id, created_at);

CREATE TRIGGER credit_transactions_immutable
    BEFORE UPDATE OR DELETE
    ON credit_transactions
    FOR EACH ROW
EXECUTE FUNCTION forbid_update_delete();
