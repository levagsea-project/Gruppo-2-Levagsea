-- Decks: a saved, swappable selection of owned card instances. Cards can be reused across decks.
-- Structural rules enforced here: one creature per suit and rank, one copy of each spell / equipment
-- (UNIQUE deck_id + deck_slot), cards must be owned by the deck owner, belong to the deck faction
-- and exist in the deck's pool snapshot. The 32-card total and per-faction limits live in the backend.

CREATE TABLE decks
(
    id          uuid PRIMARY KEY    DEFAULT gen_random_uuid(),
    owner_id    uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    name        varchar(64) NOT NULL CHECK (length(btrim(name)) > 0),
    faction     varchar(24) NOT NULL,
    snapshot_id uuid        NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    version     bigint      NOT NULL DEFAULT 0,
    FOREIGN KEY (snapshot_id, faction) REFERENCES pool_snapshots (id, faction),
    UNIQUE (owner_id, name),
    UNIQUE (id, owner_id, faction, snapshot_id)
);
CREATE INDEX decks_owner_idx ON decks (owner_id);

-- owner_id, card_id, faction, snapshot_id and deck_slot are copies kept consistent by the composite FKs.
CREATE TABLE deck_cards
(
    deck_id          uuid        NOT NULL,
    card_instance_id uuid        NOT NULL,
    owner_id         uuid        NOT NULL,
    card_id          uuid        NOT NULL,
    faction          varchar(24) NOT NULL,
    snapshot_id      uuid        NOT NULL,
    deck_slot        varchar(96) NOT NULL,
    PRIMARY KEY (deck_id, card_instance_id),
    UNIQUE (deck_id, deck_slot),
    FOREIGN KEY (deck_id, owner_id, faction, snapshot_id)
        REFERENCES decks (id, owner_id, faction, snapshot_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (card_instance_id, owner_id, card_id)
        REFERENCES card_instances (id, owner_id, card_id) ON DELETE RESTRICT,
    FOREIGN KEY (card_id, faction, deck_slot) REFERENCES cards (id, faction, deck_slot),
    FOREIGN KEY (snapshot_id, card_id) REFERENCES card_definitions (snapshot_id, card_id)
);
CREATE INDEX deck_cards_instance_idx ON deck_cards (card_instance_id);
