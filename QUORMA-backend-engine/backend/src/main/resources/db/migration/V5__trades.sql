-- Trades: a proposal between two users. Accepting is one atomic transaction in the service that swaps
-- card_instances.owner_id and writes card_ownership_history. trades.version guards against double accept.

CREATE TABLE trades
(
    id           uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    proposer_id  uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    recipient_id uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    status       varchar(12) NOT NULL DEFAULT 'PENDING' CHECK (status IN
                                                               ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    created_at   timestamptz NOT NULL DEFAULT now(),
    resolved_at  timestamptz,
    version      bigint      NOT NULL DEFAULT 0,
    CHECK (proposer_id <> recipient_id),
    CHECK ((status = 'PENDING') = (resolved_at IS NULL))
);
CREATE INDEX trades_proposer_idx ON trades (proposer_id, status);
CREATE INDEX trades_recipient_idx ON trades (recipient_id, status);

-- side OFFER: card given by the proposer; REQUEST: card asked from the recipient.
CREATE TABLE trade_items
(
    trade_id         uuid       NOT NULL REFERENCES trades (id) ON DELETE CASCADE,
    card_instance_id uuid       NOT NULL REFERENCES card_instances (id) ON DELETE RESTRICT,
    side             varchar(8) NOT NULL CHECK (side IN ('OFFER', 'REQUEST')),
    PRIMARY KEY (trade_id, card_instance_id)
);
CREATE INDEX trade_items_card_idx ON trade_items (card_instance_id);

ALTER TABLE card_ownership_history
    ADD CONSTRAINT card_ownership_history_trade_fk FOREIGN KEY (trade_id) REFERENCES trades (id) ON DELETE RESTRICT;
