-- Collections: every owned card is a unique instance row.
-- Only owner_id / acquired_at may change; origin is fixed, and each ownership change is logged.

CREATE TABLE card_instances
(
    id          uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    owner_id    uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    card_id     uuid        NOT NULL REFERENCES cards (id) ON DELETE RESTRICT,
    origin_type varchar(12) NOT NULL CHECK (origin_type IN ('PACK', 'REWARD', 'PURCHASE', 'GRANT', 'STARTER')),
    origin_ref  uuid,
    created_at  timestamptz NOT NULL DEFAULT now(),
    acquired_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (id, owner_id, card_id)
);
CREATE INDEX card_instances_owner_idx ON card_instances (owner_id);
CREATE INDEX card_instances_card_idx ON card_instances (card_id);

CREATE FUNCTION card_instances_guard() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'card instances can never be deleted';
    END IF;
    IF NEW.id <> OLD.id OR NEW.card_id <> OLD.card_id OR NEW.origin_type <> OLD.origin_type
        OR NEW.origin_ref IS DISTINCT FROM OLD.origin_ref OR NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'only owner_id and acquired_at of a card instance may change';
    END IF;
    RETURN NEW;
END
$$;
CREATE TRIGGER card_instances_guard
    BEFORE UPDATE OR DELETE
    ON card_instances
    FOR EACH ROW
EXECUTE FUNCTION card_instances_guard();

CREATE TABLE card_ownership_history
(
    id               uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    card_instance_id uuid        NOT NULL REFERENCES card_instances (id) ON DELETE RESTRICT,
    from_user_id     uuid REFERENCES users (id) ON DELETE RESTRICT,
    to_user_id       uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    reason           varchar(12) NOT NULL CHECK (reason IN ('PACK', 'REWARD', 'PURCHASE', 'GRANT', 'STARTER', 'TRADE')),
    trade_id         uuid, -- FK added in V5 (trades)
    created_at       timestamptz NOT NULL DEFAULT now(),
    CHECK (from_user_id IS DISTINCT FROM to_user_id),
    CHECK ((reason = 'TRADE') = (trade_id IS NOT NULL)),
    CHECK ((reason = 'TRADE') = (from_user_id IS NOT NULL))
);
CREATE INDEX card_ownership_history_card_idx ON card_ownership_history (card_instance_id, created_at);

CREATE TRIGGER card_ownership_history_immutable
    BEFORE UPDATE OR DELETE
    ON card_ownership_history
    FOR EACH ROW
EXECUTE FUNCTION forbid_update_delete();
