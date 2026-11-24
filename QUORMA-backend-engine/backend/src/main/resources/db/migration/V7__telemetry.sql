-- Telemetry: aggregation tables filled asynchronously from match logs.
-- telemetry_processed_matches keeps `matches` immutable while tracking what was already aggregated.

CREATE TABLE telemetry_processed_matches
(
    match_id     uuid PRIMARY KEY REFERENCES matches (id) ON DELETE RESTRICT,
    processed_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE user_stats
(
    user_id        uuid PRIMARY KEY REFERENCES users (id) ON DELETE RESTRICT,
    matches_played bigint      NOT NULL DEFAULT 0 CHECK (matches_played >= 0),
    wins           bigint      NOT NULL DEFAULT 0 CHECK (wins >= 0),
    losses         bigint      NOT NULL DEFAULT 0 CHECK (losses >= 0),
    draws          bigint      NOT NULL DEFAULT 0 CHECK (draws >= 0),
    damage_dealt   bigint      NOT NULL DEFAULT 0 CHECK (damage_dealt >= 0),
    spill_damage   bigint      NOT NULL DEFAULT 0 CHECK (spill_damage >= 0), -- overflow damage to the player after a kill
    kills          bigint      NOT NULL DEFAULT 0 CHECK (kills >= 0),
    -- A series is the best of three; matches_* above count single games.
    series_played  bigint      NOT NULL DEFAULT 0 CHECK (series_played >= 0),
    series_wins    bigint      NOT NULL DEFAULT 0 CHECK (series_wins >= 0),
    series_losses  bigint      NOT NULL DEFAULT 0 CHECK (series_losses >= 0),
    series_draws   bigint      NOT NULL DEFAULT 0 CHECK (series_draws >= 0),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    version        bigint      NOT NULL DEFAULT 0,
    CHECK (wins + losses + draws = matches_played),
    CHECK (series_wins + series_losses + series_draws = series_played)
);

-- Turns in which the player had each hand (turns[].evaluation.<side>.hand_type of the match log).
CREATE TABLE user_hand_stats
(
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    hand_type  varchar(24) NOT NULL CHECK (hand_type IN
                                           ('FIVE_OF_A_KIND', 'ROYAL_FLUSH', 'STRAIGHT_FLUSH', 'FOUR_OF_A_KIND',
                                            'FULL_HOUSE', 'FLUSH', 'STRAIGHT', 'THREE_OF_A_KIND', 'FOUR_OF_A_SUIT',
                                            'TWO_PAIR', 'PAIR', 'HIGH_CARD', 'NONE')),
    turns      bigint      NOT NULL DEFAULT 0 CHECK (turns >= 0),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version    bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, hand_type)
);

-- Games played and won with each faction (the faction swaps every game of a series).
CREATE TABLE user_faction_stats
(
    user_id      uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    faction      varchar(24) NOT NULL CHECK (faction IN ('RESISTENZA_UMANA', 'POTENZE_ARTIFICIALI')),
    games_played bigint      NOT NULL DEFAULT 0 CHECK (games_played >= 0),
    games_won    bigint      NOT NULL DEFAULT 0 CHECK (games_won >= 0),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    version      bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, faction),
    CHECK (games_won <= games_played)
);

-- Global per-card figures inside one pool snapshot (balance tuning).
CREATE TABLE card_stats
(
    snapshot_id    uuid        NOT NULL,
    card_id        uuid        NOT NULL,
    matches_played bigint      NOT NULL DEFAULT 0 CHECK (matches_played >= 0),
    matches_won    bigint      NOT NULL DEFAULT 0 CHECK (matches_won >= 0),
    times_drawn    bigint      NOT NULL DEFAULT 0 CHECK (times_drawn >= 0),
    damage_dealt   bigint      NOT NULL DEFAULT 0 CHECK (damage_dealt >= 0),
    spill_damage   bigint      NOT NULL DEFAULT 0 CHECK (spill_damage >= 0), -- overflow damage to the player after a kill
    kills          bigint      NOT NULL DEFAULT 0 CHECK (kills >= 0),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    version        bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (snapshot_id, card_id),
    FOREIGN KEY (snapshot_id, card_id) REFERENCES card_definitions (snapshot_id, card_id) ON DELETE RESTRICT
);

-- Per-user per-card figures (deckbuilding help for players).
CREATE TABLE user_card_stats
(
    user_id        uuid        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    card_id        uuid        NOT NULL REFERENCES cards (id) ON DELETE RESTRICT,
    matches_played bigint      NOT NULL DEFAULT 0 CHECK (matches_played >= 0),
    matches_won    bigint      NOT NULL DEFAULT 0 CHECK (matches_won >= 0),
    times_drawn    bigint      NOT NULL DEFAULT 0 CHECK (times_drawn >= 0),
    damage_dealt   bigint      NOT NULL DEFAULT 0 CHECK (damage_dealt >= 0),
    spill_damage   bigint      NOT NULL DEFAULT 0 CHECK (spill_damage >= 0), -- overflow damage to the player after a kill
    kills          bigint      NOT NULL DEFAULT 0 CHECK (kills >= 0),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    version        bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, card_id)
);

-- Synergy health (data/balance.json -> synergy_health): active share = turns_active / turns_total,
-- decisive share = decisive_turns / turns_active.
CREATE TABLE synergy_stats
(
    ruleset_version varchar(32) NOT NULL,
    synergy_id      varchar(32) NOT NULL, -- key of data/synergies.json, e.g. VINCOLO_3
    turns_total     bigint      NOT NULL DEFAULT 0 CHECK (turns_total >= 0),
    turns_active    bigint      NOT NULL DEFAULT 0 CHECK (turns_active >= 0),
    decisive_turns  bigint      NOT NULL DEFAULT 0 CHECK (decisive_turns >= 0),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (ruleset_version, synergy_id),
    CHECK (turns_active <= turns_total),
    CHECK (decisive_turns <= turns_active)
);
