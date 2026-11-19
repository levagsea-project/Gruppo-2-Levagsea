#!/usr/bin/env python3
"""Exports the base set (docs/cards/card-effects.json) as engine cards (match-input.schema.json#/$defs/card).

Usage (from the repo root): python3 tools/base-set/export_cards.py [then tools/base-set/export_card_texts.py]
Writes data/sets/BASE.json: the base set as a catalogue batch (one self-contained file per set), validated by
data/validate.py and read by BaseSetTest. It rewrites "set_definitions" and "cards" and keeps the "texts" already in the
file (export_card_texts.py fills the Italian ones, an English table is added by hand when it exists). The file is
imported into the database by an admin or when a deployment is bootstrapped; it is not bundled into the backend. Each card has the engine fields plus "sets" (the card sets it belongs to, here BASE);
whoever sends a card to the engine drops "sets", which the engine schema does not know. Card names and flavour
texts are card text, outside the snapshot: they live in the "texts" of the same file.

- An ability written on the card as a keyword or evolution that is live in data/ becomes {"id", "params"}.
- Any other ability (form "inline", or proposed abilities kept out of data/ by the form policy) is written out as
  expansions: $param.<name> takes the card's value (or the ability's default), "-$param" negates it, K_PARAM
  conditions are settled and dropped, and an Evolution keeps its T_EVO wrapper.
- Spells and equipment: their expansions as written, without the "role" key.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
doc = json.loads((ROOT / "docs/cards/card-effects.json").read_text(encoding="utf-8"))
kw = json.loads((ROOT / "data/keyword.json").read_text(encoding="utf-8"))
evo = json.loads((ROOT / "data/evolutions.json").read_text(encoding="utf-8"))
proposed = doc["abilities_proposed"]

SETS = ["BASE"]
# The sets the file defines: a set is created with this status when it does not exist yet (an existing set is never changed
# by an import). The base set is on sale from the start.
SET_DEFINITIONS = [{"code": "BASE", "status": "IN_VENDITA"}]
RANKS = {"A": 1, "J": 11, "Q": 12, "K": 13}
EXPANSION_KEYS = ("trigger_id", "trigger_params", "effect_id", "effect_params", "target_id", "target_params",
                  "duration", "conditions", "class", "max_triggers", "once_per_turn", "max_stacks", "stack_key")
REF = re.compile(r"^(-?)\$param\.([a-z_]+)$")


def code(name):
    return name.upper().replace(" ", "_")


def resolve(node, params, where):
    if isinstance(node, dict):
        return {k: resolve(v, params, where) for k, v in node.items()}
    if isinstance(node, list):
        return [resolve(v, params, where) for v in node]
    if isinstance(node, str):
        m = REF.match(node)
        if m:
            if m.group(2) not in params:
                sys.exit(f"{where}: no value for $param.{m.group(2)}")
            value = params[m.group(2)]
            return -value if m.group(1) else value
    return node


def clean(x):
    out = {}
    for k in EXPANSION_KEYS:
        if k in x and x[k] not in ({}, []):
            out[k] = x[k]
    out.setdefault("duration", "D_Istantanea")
    return out


def inline(ref, card_params, where):
    entry = proposed.get(ref) or kw.get(ref) or evo.get(ref)
    if entry is None:
        sys.exit(f"{where}: unknown ability {ref}")
    params = dict(entry.get("params", {}) if ref in proposed else {})
    params.update(card_params)
    out = []
    for x in entry["expands_to"]:
        conds = x.get("conditions", [])
        if any(c["id"] == "K_PARAM" and params.get(c["params"]["name"]) != c["params"]["equals"] for c in conds):
            continue
        y = dict(x)
        y["conditions"] = [c for c in conds if c["id"] != "K_PARAM"]
        y = resolve(y, params, where)
        if "$param" in json.dumps(y):
            sys.exit(f"{where}: unresolved reference in {y}")
        out.append(clean(y))
    return {"expansions": out}


cards = []
for c in doc["cards"]:
    where = c["id"]
    if c["kind"] == "creature":
        abilities = []
        for a in c["abilities"]:
            ref, params = a["ref"], a.get("params", {})
            if ref in kw or ref in evo:
                entry = {"id": ref}
                if params:
                    entry["params"] = params
                abilities.append(entry)
            else:
                abilities.append(inline(ref, params, where))
        cards.append({"id": c["id"], "sets": SETS, "kind": "CREATURE", "rank": RANKS.get(c["rank"]) or int(c["rank"]),
                      "suit": code(c["suit"]), "subfaction": code(c["subfaction"]),
                      "atk": c["stats"]["atk"], "def": c["stats"]["def"], "abilities": abilities})
    else:
        abilities = [{"expansions": [clean(x) for x in a["inline"]]} for a in c["abilities"]]
        cards.append({"id": c["id"], "sets": SETS, "kind": c["kind"].upper(), "faction": code(c["faction"]), "abilities": abilities})

out = ROOT / "data/sets/BASE.json"
out.parent.mkdir(parents=True, exist_ok=True)
previous = json.loads(out.read_text(encoding="utf-8")) if out.exists() else {}
out.write_text(json.dumps({"_readme": "Base set. Generated by tools/base-set/export_cards.py from docs/cards/card-effects.json; rerun it when that file changes. Checked by validate.py.",
                           "set_definitions": SET_DEFINITIONS, "cards": cards,
                           "texts": previous.get("texts", {})}, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
print(f"{len(cards)} cards -> {out.relative_to(ROOT)}")
