#!/usr/bin/env python3
"""Validator for the QUORMA effect data files (agent.md section 7, checks 1-10)
plus the theme lint added with the suit-thematicity pass.

Usage: python3 validate.py [folder]   (default: current folder)
Exit code 0 = no errors. Warnings never fail the run.
"""
import json
import re
import sys
from pathlib import Path

root = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
load = lambda n: json.loads((root / n).read_text(encoding="utf-8"))
vocab, kw, evo = load("vocab.json"), load("keyword.json"), load("evolutions.json")
syn, bal, inv = load("synergies.json"), load("balance.json"), load("invalid-combinations.json")
sub = load("subfactions.json")
syn = {k: v for k, v in syn.items() if not k.startswith("_")}
ranks_doc = load("ranks.json") if (root / "ranks.json").exists() else None

errors, warnings = [], []
err = lambda w, m: errors.append(f"{w}: {m}")
warn = lambda w, m: warnings.append(f"{w}: {m}")

E, T, S = vocab["effects"], vocab["triggers"], vocab["targets"]
D, C = vocab["durations"], vocab["conditions"]
as_list = lambda x: x if isinstance(x, list) else [x]

# parked/: abilities, vocab and rules taken out of the live files until needed. Never loaded by the engine.
parked_dir = root / "parked"
load_parked = lambda n: json.loads((parked_dir / n).read_text(encoding="utf-8")) if (parked_dir / n).exists() else {}
p_vocab, p_kw, p_evo = load_parked("vocab.json"), load_parked("keyword.json"), load_parked("evolutions.json")
p_bal, p_inv = load_parked("balance.json"), load_parked("invalid-combinations.json")
VOCAB_SECTIONS = ("effects", "triggers", "targets", "conditions", "durations")
parked_ids = {i: s for s in VOCAB_SECTIONS for i in p_vocab.get(s, {})}
parked_abilities = set(p_kw) | set(p_evo)


def unknown(kind, key):
    """Error text for a missing id: says where to restore it from when it is parked."""
    if key in parked_ids:
        return f"{kind} {key} is parked: move it back from parked/vocab.json ({parked_ids[key]})"
    if key in parked_abilities:
        return f"{kind} {key} is parked: move it back from parked/ (see data/AGENTS.md section 13)"
    return f"unknown {kind} {key}"


def provides_of(trigger_id, trigger_params):
    if trigger_id == "T_EVO":
        return provides_of(trigger_params.get("trigger_id"), trigger_params.get("trigger_params", {}))
    return T.get(trigger_id, {}).get("provides", []) or []


def class_of_trigger(trigger_id, trigger_params):
    if trigger_id == "T_EVO":
        inner = trigger_params.get("trigger_id")
        if T.get(inner, {}).get("class") != "T":
            err("T_EVO", f"inner trigger {inner} is not class T")
        return "T"
    return T.get(trigger_id, {}).get("class")


def rule_matches(rule, x):
    for field in ("trigger_id", "effect_id", "target_id"):
        want = rule.get(field)
        if want is not None and x.get(field) not in as_list(want):
            return False
    if "with_param" in rule and rule["with_param"] not in x.get("effect_params", {}):
        return False
    if "allowed_triggers" in rule:
        # whitelist rule: matches (= invalid) when effect matches and trigger not allowed
        return x.get("trigger_id") not in rule["allowed_triggers"] and x["effect_id"] in as_list(rule["effect_id"])
    if "allowed_classes" in rule:
        # whitelist rule: matches (= invalid) when the other fields match and the class is not allowed
        return x.get("class") not in rule["allowed_classes"]
    if "allowed_targets" in rule:
        # whitelist rule: matches (= invalid) when the other fields match and the target is not allowed
        return x.get("target_id") not in rule["allowed_targets"]
    return True


def card_contexts(card_kind):
    """The contexts a spell or equipment gives its targets (vocab card_contexts, parked until those cards exist)."""
    section = vocab.get("card_contexts") or p_vocab.get("card_contexts") or {}
    return section.get(card_kind, [])


def check_expansion(where, x, origin, card_kind=None):
    """card_kind is "spell" or "equipment" for an ability of such a card, None for keywords, evolutions and synergies."""
    t, e, s, d = x.get("trigger_id"), x["effect_id"], x["target_id"], x["duration"]
    tp = x.get("trigger_params", {})
    # a spell or equipment has no trigger (2026-11-17-class-i-no-trigger): only class I, only on those cards
    if t is None:
        if card_kind is None:
            err(where, "only a spell or equipment expansion may have no trigger")
            return
        if x["class"] != "I":
            err(where, f"an expansion without trigger must be class I, got {x['class']}")
            return
    # 1 ids exist
    for kind, key, table in (("trigger", t, T), ("effect", e, E), ("target", s, S), ("duration", d, D)):
        if key is None:
            continue
        if key not in table:
            err(where, unknown(kind, key))
            return
    for c in x.get("conditions", []):
        if c["id"] not in C:
            err(where, unknown("condition", c["id"]))
    # 2 class
    cls = x["class"]
    if t is not None:
        class_of_trigger(t, tp)
        allowed = inv["class_rules"]["trigger_allowed_effect_classes"].get(t)
        if allowed is None:
            err(where, f"trigger {t} missing from class_rules")
        elif cls not in allowed:
            err(where, f"class {cls} not allowed for trigger {t} ({allowed})")
    if cls not in E[e]["class"]:
        err(where, f"class {cls} not in effect {e} classes {E[e]['class']}")
    # 3 target kind
    kinds = set(S[s]["kind"]) | ({"card"} if "creature" in S[s]["kind"] else set())
    if not set(E[e]["target_kind"]) & kinds:
        err(where, f"target {s} kinds {S[s]['kind']} do not fit effect {e} {E[e]['target_kind']}")
    # 4 trigger context
    ctx = S[s].get("requires_context")
    provided = card_contexts(card_kind) if t is None else provides_of(t, tp)
    # an ability written on a card is checked like the engine does: the context only for class T (a spell or equipment
    # takes the context of its targets from the card; a replacement reads the context its trigger implies)
    if ctx and ctx not in provided and (origin is not None and origin in kw or origin in evo or origin in syn or cls == "T"):
        err(where, f"target {s} needs context '{ctx}' not provided by " + (f"a {card_kind}" if t is None else t))
    # 5 duration
    mode = E[e]["duration_mode"]
    if cls == "I":
        mode = "any"  # the engine ignores the duration of an immediate effect: it applies for good
    if mode == "instant_only" and d != "D_Istantanea":
        err(where, f"effect {e} is instant_only, got {d}")
    if mode == "sustained_only" and d == "D_Istantanea":
        err(where, f"effect {e} is sustained_only, got {d}")
    # 11 reach: a creature target must be found in the class of the expansion (a continuous effect reaches it, a
    # triggered effect chooses it when it resolves); the other classes are checked elsewhere
    # (a continuous effect of a spell may aim at a selection, S33-S40, which the engine resolves when the spell is cast)
    chosen_by_spell = card_kind == "spell" and cls == "C" and s in {f"S{n}" for n in range(33, 41)}
    if cls in ("C", "T") and "creature" in S[s]["kind"] and not chosen_by_spell and cls not in S[s].get("reach_classes", []):
        err(where, f"target {s} is not found by a class {cls} effect (reach_classes {S[s].get('reach_classes', [])})")
    # 6 class I only
    if card_kind is None and set(E[e]["class"]) == {"I"}:
        err(where, f"class-I-only effect {e} in a creature ability")
    # 7 combination rules
    for r in inv["combination_rules"]:
        if rule_matches(r, x):
            err(where, f"matches {r['rule_id']}: {r['reason']}")
    # 8 keyword grants
    if e == "E14":
        p = x["effect_params"]
        k = kw.get(p.get("keyword"))
        if not k:
            err(where, "E14 grants " + unknown("keyword", p.get("keyword")))
        else:
            schema, given = k.get("params", {}), p.get("keyword_params", {})
            b_ = bal["keywords"].get(k.get("balance_ref"), {})
            scaled = list(b_.get("tier_scaled", [])) + list(b_.get("params", {}))  # as the engine allows them
            for n, spec in schema.items():
                if spec.get("required") and n not in given:
                    err(where, f"E14 missing required keyword param {n}")
            for n in given:
                if n not in schema and n not in scaled:
                    err(where, f"E14 unknown keyword param {n}")
    # 9 references
    for ref in re.findall(r"\$tune\.([A-Za-z0-9_.]+)", json.dumps(x)):
        parts = ref.split(".")
        if parts[0] == "synergies":
            ok = parts[1] in bal["synergies"] and parts[2] in bal["synergies"][parts[1]]
        else:
            ent = bal["keywords"].get(parts[0]) or bal["evolutions"].get(parts[0])
            ok = bool(ent) and (parts[1] in ent.get("params", {}) or parts[1] in ent)
        if not ok:
            err(where, f"unresolved $tune.{ref}" + (f" ({parts[0]} is parked)" if parts[0] in parked_abilities else ""))
    for ref in re.findall(r"\$param\.([a-z_]+)", json.dumps(x)):
        if ref not in kw.get(origin, {}).get("params", {}):
            err(where, f"$param.{ref} not declared by {origin}")


for k, v in kw.items():
    for i, x in enumerate(v["expands_to"]):
        if x.get("trigger_id") == "T_EVO":  # 10
            err(f"keyword {k}[{i}]", "keywords must not use T_EVO")
        check_expansion(f"keyword {k}[{i}]", x, k)
    if v.get("balance_ref") not in bal["keywords"]:
        err(f"keyword {k}", "balance_ref missing in balance.json")
for k, v in evo.items():
    for i, x in enumerate(v["expands_to"]):
        if x.get("trigger_id") != "T_EVO":
            err(f"evolution {k}[{i}]", "evolutions must use T_EVO")
        check_expansion(f"evolution {k}[{i}]", x, k)
    if v.get("balance_ref") not in bal["evolutions"]:
        err(f"evolution {k}", "balance_ref missing in balance.json")
for k, v in syn.items():
    for i, x in enumerate(v["expands_to"]):
        check_expansion(f"synergy {k}[{i}]", x, k)
    for r in v.get("replaces", []):
        if r not in syn:
            err(f"synergy {k}", f"replaces unknown {r}")

# 9 description placeholders (warnings: known to be loose in v1)
for k, v in kw.items():
    disp = bal["keywords"].get(k, {}).get("display", {})
    for ph in re.findall(r"\{([a-z_]+)\}", v["description"]):
        if ph not in disp and ph not in v.get("params", {}):
            warn(f"keyword {k}", f"placeholder {{{ph}}} not in display/params")
for k, v in evo.items():
    disp = bal["evolutions"].get(k, {}).get("display", {})
    for ph in re.findall(r"\{([a-z_]+)\}", v["description"]):
        if ph not in disp:
            warn(f"evolution {k}", f"placeholder {{{ph}}} not in display")


FACTIONS = vocab["constants"]["factions"]
for k, e_ in E.items():
    if e_.get("deprecated") and e_["class"]:
        err(f"effect {k}", "deprecated effects must have an empty class list")


# Cap model (balance.json): capped = single rank, open = any rank
ALL_RANKS = vocab["constants"]["ranks"]
for table, name in ((bal["keywords"], "keyword"), (bal["evolutions"], "evolution")):
    for k, v in table.items():
        cc = v.get("cap_class")
        if cc not in ("capped", "limited", "open"):
            err(f"{name} {k}", "cap_class must be 'capped', 'limited' or 'open'")
            continue
        rk = v.get("allowed_ranks", [])
        lim = bal["globals"]["limited_max_ranks"]
        if any(r not in ALL_RANKS for r in rk):
            err(f"{name} {k}", "unknown rank in allowed_ranks")
        if v.get("max_built_in") != 4 * len(rk):
            err(f"{name} {k}", f"max_built_in {v.get('max_built_in')} != 4 x {len(rk)} ranks")
        if cc == "capped" and len(rk) != 1:
            err(f"{name} {k}", "capped ability must have exactly one allowed rank")
        if cc == "limited" and not (2 <= len(rk) <= lim):
            err(f"{name} {k}", f"limited ability must have 2..{lim} ranks, has {len(rk)}")
        if cc == "open" and len(rk) <= lim:
            err(f"{name} {k}", f"open ability must have more than {lim} ranks")
        if cc in ("capped", "limited") and "cap_risk" not in v:
            err(f"{name} {k}", "capped/limited ability needs cap_risk")
        if cc == "limited":
            tiers_hit = {ranks_doc["ranks"][r]["tier"] for r in rk} if ranks_doc else set()
            if len(tiers_hit) > 1:
                warn(f"{name} {k}", "limited ranks span more than one Tier")
        if cc == "open" and "intrinsic_limit" not in v:
            warn(f"{name} {k}", "open ability without intrinsic_limit note")
        for p in v.get("tier_scaled", []):
            if p not in v.get("params", {}):
                err(f"{name} {k}", f"tier_scaled param '{p}' not in params")
        if cc != "open" and v.get("tier_scaled"):
            err(f"{name} {k}", "only open abilities are tier scaled")
# an evolution that grants a capped keyword may not be wider than that keyword
for ek, ev in evo.items():
    for x in ev["expands_to"]:
        if x["effect_id"] == "E14":
            kk = x["effect_params"]["keyword"]
            if bal["keywords"][kk]["cap_class"] != "open":
                extra = set(bal["evolutions"][ek]["allowed_ranks"]) - set(bal["keywords"][kk]["allowed_ranks"])
                if extra:
                    err(f"evolution {ek}", f"grants capped {kk} but is allowed on ranks {sorted(extra)}")
risk_ranks = {}
for table in (bal["keywords"], bal["evolutions"]):
    for k, v in table.items():
        if v["cap_class"] != "open":
            risk_ranks.setdefault(v["cap_risk"], set()).update(v["allowed_ranks"])
            print(f"INFO  {v['cap_class']:7} {k}: ranks {', '.join(v['allowed_ranks'])} (max {v['max_built_in']})")
for rk_, rs in risk_ranks.items():
    print(f"INFO  risk {rk_}: ranks {', '.join(sorted(rs))} -> at most {4 * len(rs)} cards per deck")

# ranks.json
if ranks_doc:
    tier_of = {**{r: 1 for r in ("2", "3", "4", "5", "6")}, **{r: 2 for r in ("7", "8", "9", "10")},
               **{r: 3 for r in ("J", "Q", "K")}, "A": 4}
    for r in ALL_RANKS:
        entry = ranks_doc["ranks"].get(r)
        if not entry:
            err(f"ranks.json {r}", "missing rank")
        elif entry["tier"] != tier_of[r]:
            err(f"ranks.json {r}", f"tier {entry['tier']} contradicts ruleset 3.2.2 ({tier_of[r]})")
    for t, ids in ranks_doc["role_affinity"].items():
        if t.startswith("_"):
            continue
        for i in ids:
            if i not in bal["keywords"] and i not in bal["evolutions"]:
                err(f"ranks.json role_affinity {t}", unknown("ability", i))

# Theme lint (balance.json -> suit_themes / themes)
suit_themes = bal.get("suit_themes", {})
if suit_themes:
    ranks = vocab["constants"]["ranks"]
    for table, name in ((bal["keywords"], "keyword"), (bal["evolutions"], "evolution")):
        for k, v in table.items():
            if "themes" not in v:
                warn(f"{name} {k}", "no themes tag")
    for suit, themes in suit_themes.items():
        if suit.startswith("_"):
            continue
        gaps = [r for r in ranks if not any(
            r in v.get("allowed_ranks", []) and set(v.get("themes", [])) & set(themes)
            for t in (bal["keywords"], bal["evolutions"]) for v in t.values())]
        if gaps:
            warn(f"suit {suit}", "no themed ability eligible at ranks " + ", ".join(gaps)
                 + " (generation falls back to unthemed abilities)")

# Expected suit/role match of the pool-generation draw (balance.json -> pool_generation)
def expected_fit(slot, suit, tier, tier_ranks):
    p, q = bal["pool_generation"]["themed_draw_pct"] / 100, bal["pool_generation"]["role_draw_pct"] / 100
    th = set(bal["suit_themes"][suit]); aff = set(ranks_doc["role_affinity"][str(tier)])
    s_acc, r_acc = [], []
    for r in tier_ranks:
        E = {k for k, v in bal[slot].items() if r in v["allowed_ranks"]}
        Tm = {k for k in E if set(bal[slot][k].get("themes", [])) & th}
        def role_share(C):
            R = C & aff
            return (q * len(R) / len(C) + (1 - q) * 0) if (R and C) else 0, bool(R)
        # branch 1: restricted to themed set (if any); branch 2: whole eligible set
        def branch(C):
            R = C & aff
            if R:  # with prob q restrict to R (all role match), else uniform over C
                return q + (1 - q) * len(R) / len(C)
            return 0.0
        if Tm:
            s_acc.append(p + (1 - p) * len(Tm) / len(E))
            r_acc.append(p * branch(Tm) + (1 - p) * branch(E))
        else:
            s_acc.append(0.0)
            r_acc.append(branch(E))
    return sum(s_acc) / len(s_acc), sum(r_acc) / len(r_acc)

if ranks_doc and "themed_draw_pct" in bal.get("pool_generation", {}):
    tr = {int(t): v["ranks"] for t, v in ranks_doc["tiers"].items()}
    ft = bal["pool_generation"]["fit_targets"]
    lo_s = lo_r = 100.0
    for slot in ("keywords", "evolutions"):
        if not bal[slot]:
            print(f"INFO  fit {slot}: skipped, no live {slot}")
            continue
        for suit in bal["suit_themes"]:
            if suit.startswith("_"):
                continue
            for tier, rs in tr.items():
                sm, rm = expected_fit(slot, suit, tier, rs)
                lo_s, lo_r = min(lo_s, sm * 100), min(lo_r, rm * 100)
                if sm * 100 < ft["min_suit_match_pct"]:
                    warn(f"fit {slot} {suit} tier {tier}", f"expected suit match {sm*100:.0f}% < {ft['min_suit_match_pct']}%")
                if rm * 100 < ft["min_role_match_pct"]:
                    warn(f"fit {slot} {suit} tier {tier}", f"expected role match {rm*100:.0f}% < {ft['min_role_match_pct']}%")
    print(f"INFO  lowest expected match: suit {lo_s:.0f}%, role {lo_r:.0f}%")

# Subfaction matrix (subfactions.json + balance.json -> subfaction_advantage_permille)
sf = sub.get("subfactions", {})
declared = {s: f for f, names in vocab["constants"]["subfactions"].items() for s in names}
if set(sf) != set(declared):
    err("subfactions.json", f"subfactions {sorted(set(sf) ^ set(declared))} differ from vocab.constants.subfactions")
for name, v in sf.items():
    where = f"subfactions.json {name}"
    if v.get("faction") != declared.get(name):
        err(where, f"faction '{v.get('faction')}' differs from vocab ('{declared.get(name)}')")
    for rel in ("strong_against", "weak_against"):
        other = v.get(rel)
        if other not in sf:
            err(where, f"{rel} names unknown subfaction '{other}'")
        elif sf[other].get("faction") == v.get("faction"):
            err(where, f"{rel} '{other}' is in the same faction (relations only exist between factions)")
    if v.get("strong_against") == v.get("weak_against"):
        err(where, "strong_against and weak_against are the same subfaction")
    s_, w_ = v.get("strong_against"), v.get("weak_against")
    if s_ in sf and sf[s_].get("weak_against") != name:
        err(where, f"strong against {s_}, but {s_} is not weak against {name} (relations are reciprocal)")
    if w_ in sf and sf[w_].get("strong_against") != name:
        err(where, f"weak against {w_}, but {w_} is not strong against {name} (relations are reciprocal)")
    if v.get("cycle") not in sub.get("cycles", {}):
        err(where, f"unknown cycle '{v.get('cycle')}'")
for cid, cyc in sub.get("cycles", {}).items():
    order = cyc.get("order", [])
    where = f"subfactions.json cycle {cid}"
    if len(order) != 4 or len(set(order)) != 4:
        err(where, "a cycle must list 4 distinct subfactions")
        continue
    for i, name in enumerate(order):
        nxt = order[(i + 1) % 4]
        if sf.get(name, {}).get("strong_against") != nxt:
            err(where, f"{name} should be strong against {nxt} (ring order)")
        if sf.get(name, {}).get("cycle") != cid:
            err(where, f"{name} is tagged with cycle '{sf.get(name, {}).get('cycle')}'")
adv = bal.get("subfaction_advantage_permille", {})
for key, ok in (("strong", lambda x: x > 1000), ("weak", lambda x: 0 < x < 1000)):
    val = adv.get(key)
    if not isinstance(val, int) or isinstance(val, bool) or not ok(val):
        err("balance.json subfaction_advantage_permille", f"'{key}' must be an integer per-mille ({'> 1000' if key == 'strong' else 'between 1 and 999'}), got {val!r}")

# Parked (parked/*.json): an id lives in exactly one place, and new ids never reuse a parked one
for name, live_t, park_t in [(s, vocab[s], p_vocab.get(s, {})) for s in VOCAB_SECTIONS] + [
        ("keyword.json", kw, p_kw), ("evolutions.json", evo, p_evo),
        ("balance.json keywords", bal["keywords"], p_bal.get("keywords", {})),
        ("balance.json evolutions", bal["evolutions"], p_bal.get("evolutions", {})),
        ("class_rules", inv["class_rules"]["trigger_allowed_effect_classes"],
         p_inv.get("class_rules", {}).get("trigger_allowed_effect_classes", {}))]:
    for i in sorted(set(live_t) & set(park_t)):
        err(f"parked {name}", f"{i} is both live and parked")
for k in p_kw:
    if k not in p_bal.get("keywords", {}):
        err(f"parked keyword {k}", "no matching entry in parked/balance.json keywords")
for k in p_evo:
    if k not in p_bal.get("evolutions", {}):
        err(f"parked evolution {k}", "no matching entry in parked/balance.json evolutions")
live_rules = {r["rule_id"] for r in inv["combination_rules"]}
for r in p_inv.get("combination_rules", []):
    if r["rule_id"] in live_rules:
        err("parked invalid-combinations.json", f"{r['rule_id']} is both live and parked")
for prefix, sections in (("E", ("effects",)), ("T", ("triggers",)), ("S", ("targets",)), ("C", ("conditions",))):
    nums = [int(i[1:]) for s in sections for i in list(vocab[s]) + list(p_vocab.get(s, {}))
            if i[1:].isdigit() and i.startswith(prefix)]
    print(f"INFO  next free {prefix} id: {prefix}{max(nums) + 1 if nums else 0} (live and parked counted)")

# Set files (sets/<CODE>.json, checks 12 and 13): one self-contained batch per set, in the format the catalogue imports:
# the set it defines, its cards as the engine reads them plus "sets", and the card text of every language. The files are
# not bundled into anything: an admin imports them, or a deployment bootstraps from them (initial release, or rebuilding
# the database from zero); after that the database is the only truth. Every ability is checked like a keyword or
# evolution of the same shape; ids, params, slots and rank eligibility are checked here.
set_files = sorted((root / "sets").glob("*.json")) if (root / "sets").is_dir() else []
all_card_defs, all_names, referenced_sets, all_defined_sets, all_pack_codes = {}, {}, set(), set(), {}
for set_path in set_files:
    file_label = f"sets/{set_path.name}"
    cards_doc = json.loads(set_path.read_text(encoding="utf-8"))
    # set_definitions: the sets the file creates when they do not exist yet (code, optional status and dates)
    defined_sets = set()
    for sd in cards_doc.get("set_definitions", []):
        sw = f"{file_label} set_definitions {sd.get('code', '?')}"
        if set(sd) - {"code", "status", "release_date", "close_date"}:
            err(sw, f"unknown fields {sorted(set(sd) - {'code', 'status', 'release_date', 'close_date'})}")
        if not isinstance(sd.get("code"), str) or not re.fullmatch(r"[A-Z0-9_]{1,32}", sd["code"]):
            err(sw, "code must be A-Z, 0-9, _ (1 to 32 characters)")
        elif sd["code"] in defined_sets:
            err(sw, "duplicate code")
        else:
            defined_sets.add(sd["code"])
        if sd.get("status", "BOZZA") not in ("BOZZA", "IN_VENDITA", "CHIUSA"):
            err(sw, "status must be BOZZA, IN_VENDITA or CHIUSA")
    if defined_sets != {set_path.stem}:
        err(file_label, f"a set file defines exactly its own set: set_definitions must be the one code {set_path.stem} (the file name)")
    all_defined_sets |= defined_sets
    # pack_types: the packs of this set (created with it when they do not exist yet): code, the file's own set, price, slots
    # per Tier, support slots. Every Tier with slots needs a creature of that Tier in the file, and support slots need a
    # spell or an equipment (a pack draws with repetition, so one card is enough): the same check the engine makes.
    tier_of_rank = lambda r_: 4 if r_ == 1 else 3 if r_ >= 11 else 2 if r_ >= 7 else 1
    file_tiers = {tier_of_rank(c_["rank"]) for c_ in cards_doc.get("cards", []) if c_.get("kind") == "CREATURE" and isinstance(c_.get("rank"), int)}
    file_has_support = any(c_.get("kind") in ("SPELL", "EQUIPMENT") for c_ in cards_doc.get("cards", []))
    for pt in cards_doc.get("pack_types", []):
        pw = f"{file_label} pack_types {pt.get('code', '?')}"
        if set(pt) - {"code", "set", "price", "slots", "support_slots", "featured"}:
            err(pw, f"unknown fields {sorted(set(pt) - {'code', 'set', 'price', 'slots', 'support_slots', 'featured'})}")
        if not isinstance(pt.get("code"), str) or not re.fullmatch(r"[a-z0-9_]{1,32}", pt["code"]):
            err(pw, "code must be a-z, 0-9, _ (1 to 32 characters)")
        elif pt["code"] in all_pack_codes:
            err(pw, f"duplicate code (also in {all_pack_codes[pt['code']]})")
        else:
            all_pack_codes[pt["code"]] = file_label
        if pt.get("set") != set_path.stem:
            err(pw, f"a pack type of {file_label} belongs to the set {set_path.stem}")
        if not isinstance(pt.get("price"), int) or isinstance(pt.get("price"), bool) or pt["price"] <= 0:
            err(pw, "price must be a whole number above 0")
        slots_ = pt.get("slots", {})
        total_ = 0
        if not isinstance(slots_, dict):
            err(pw, "slots must be an object of TIER_1..TIER_4")
            slots_ = {}
        for tier_name, n_ in slots_.items():
            if tier_name not in ("TIER_1", "TIER_2", "TIER_3", "TIER_4") or not isinstance(n_, int) or isinstance(n_, bool) or not 0 <= n_ <= 1000:
                err(pw, f"slots.{tier_name} must be TIER_1..TIER_4 with a whole number from 0 to 1000")
                continue
            total_ += n_
            if n_ > 0 and int(tier_name[-1]) not in file_tiers:
                err(pw, f"{n_} {tier_name} slots and no creature of that Tier in the file: the pack cannot be filled")
        sup_ = pt.get("support_slots", 0)
        if not isinstance(sup_, int) or isinstance(sup_, bool) or not 0 <= sup_ <= 1000:
            err(pw, "support_slots must be a whole number from 0 to 1000")
        else:
            total_ += sup_
            if sup_ > 0 and not file_has_support:
                err(pw, "support slots and no spell or equipment in the file: the pack cannot be filled")
        if total_ < 1:
            err(pw, "a pack holds at least one card")
        if "featured" in pt and not isinstance(pt["featured"], bool):
            err(pw, "featured must be true or false")
    consts = vocab["constants"]
    rank_code = {1: "A", 11: "J", 12: "Q", 13: "K"}
    suit_codes = {x.upper(): x for x in consts["suits"]}
    sub_faction = {x.upper(): f for f, names in consts["subfactions"].items() for x in names}
    faction_codes = {f.upper().replace(" ", "_"): f for f in consts["factions"]}
    slots = bal["globals"].get("card_ability_slots", {})
    seen_ids, counts = set(), {}
    for card in cards_doc.get("cards", []):
        cid = card.get("id", "?")
        where = f"{file_label} {cid}"
        if cid in seen_ids:
            err(where, "duplicate id")
        seen_ids.add(cid)
        # a card may be written in two set files (a reprint) only with the same definition
        canon = json.dumps({k_: v_ for k_, v_ in card.items() if k_ != "sets"}, sort_keys=True)
        first = all_card_defs.setdefault(cid, (file_label, canon))
        if first[1] != canon:
            err(where, f"also in {first[0]} with a different definition")
        card_sets = card.get("sets")
        if not isinstance(card_sets, list) or not card_sets or len(set(card_sets)) != len(card_sets) or not all(
                isinstance(e_, str) and re.fullmatch(r"[A-Z0-9_]+", e_) for e_ in card_sets):
            err(where, "sets must be a non-empty list of distinct codes (A-Z, 0-9, _)")
        else:
            referenced_sets.update(card_sets)
            if set_path.stem not in card_sets:
                err(where, f"a card of {file_label} is in the set {set_path.stem}")
        kind = card.get("kind")
        card_kind = {"CREATURE": None, "SPELL": "spell", "EQUIPMENT": "equipment"}.get(kind, "?")
        if card_kind == "?":
            err(where, f"unknown kind '{kind}'")
            continue
        if kind == "CREATURE":
            extra = set(card) - {"id", "sets", "kind", "rank", "suit", "subfaction", "atk", "def", "abilities"}
            if extra:
                err(where, f"unknown fields {sorted(extra)}")
            if card.get("rank") not in range(1, 14):
                err(where, f"rank {card.get('rank')!r} must be 1..13 (Ace 1, Jack 11, Queen 12, King 13)")
            if card.get("suit") not in suit_codes:
                err(where, f"unknown suit '{card.get('suit')}'")
            if card.get("subfaction") not in sub_faction:
                err(where, f"unknown subfaction '{card.get('subfaction')}'")
            for f_ in ("atk", "def"):
                if not isinstance(card.get(f_), int) or isinstance(card.get(f_), bool) or card[f_] < (1 if f_ == "def" else 0):
                    err(where, f"{f_} must be an integer ({'>= 1' if f_ == 'def' else '>= 0'})")
            faction = sub_faction.get(card.get("subfaction"))
            # <SET>-RES-... or <SET>-AI-...: the code of the set the card was first printed in, then its faction
            marker = {"Resistenza Umana": "RES", "Potenze Artificiali": "AI"}.get(faction)
            if marker and not re.match(rf"^[A-Z0-9_]+-{marker}-", cid):
                err(where, f"id should look like <SET>-{marker}-... for a {faction} subfaction")
            rank_name = rank_code.get(card.get("rank"), str(card.get("rank")))
        else:
            extra = set(card) - {"id", "sets", "kind", "faction", "abilities"}
            if extra:
                err(where, f"unknown fields {sorted(extra)}")
            faction = faction_codes.get(card.get("faction"))
            if faction is None:
                err(where, f"unknown faction '{card.get('faction')}'")
            rank_name = None
        counts[(faction, kind)] = counts.get((faction, kind), 0) + 1
        abilities = card.get("abilities")
        if not isinstance(abilities, list):
            err(where, "abilities must be a list")
            continue
        if not abilities and kind != "CREATURE":
            # only a creature may be vanilla (decision 2026-11-19-vanilla-creatures)
            err(where, "a spell or an equipment needs at least one ability")
            continue
        n_kw = n_evo = 0
        used = set()
        for i, a in enumerate(abilities):
            aw = f"{where} ability[{i}]"
            if "expansions" in a:
                if set(a) != {"expansions"} or not a["expansions"]:
                    err(aw, "an inline ability has only a non-empty 'expansions' list")
                    continue
                for j, x in enumerate(a["expansions"]):
                    if "$param" in json.dumps(x):
                        err(f"{aw}[{j}]", "unresolved $param reference")
                    check_expansion(f"{aw}[{j}]", x, None, card_kind)
                continue
            if card_kind is not None:
                err(aw, "spells and equipment write their abilities as expansions")
                continue
            aid = a.get("id")
            if set(a) - {"id", "params"}:
                err(aw, f"unknown fields {sorted(set(a) - {'id', 'params'})}")
            table, bal_table = (kw, bal["keywords"]) if aid in kw else (evo, bal["evolutions"]) if aid in evo else (None, None)
            if table is None:
                err(aw, unknown("keyword or evolution", aid))
                continue
            if aid in used:
                err(aw, f"{aid} is listed twice")
            used.add(aid)
            if table is kw:
                n_kw += 1
            else:
                n_evo += 1
            schema, given = table[aid].get("params") or {}, a.get("params", {})
            bal_entry = bal_table.get(table[aid]["balance_ref"], {})
            for n_, spec in schema.items():
                if spec.get("required") and n_ not in given:
                    err(aw, f"{aid} missing required param {n_}")
            for n_ in given:  # a card may also write a tier-scaled number or a number of the ability's balance.json entry
                if n_ not in schema and n_ not in bal_entry.get("tier_scaled", []) and n_ not in bal_entry.get("params", {}):
                    err(aw, f"{aid} unknown param {n_}")
            allowed = bal_entry.get("allowed_ranks")
            if allowed is not None and rank_name is not None and rank_name not in allowed:
                err(aw, f"{aid} is not allowed on rank {rank_name} (allowed: {', '.join(allowed)})")
        if slots and n_kw > slots.get("keyword", 99):
            err(where, f"{n_kw} keywords, at most {slots['keyword']}")
        if slots and n_evo > slots.get("evolution", 99):
            err(where, f"{n_evo} evolutions, at most {slots['evolution']}")
    for (f_, k_), n_ in sorted(counts.items(), key=str):
        print(f"INFO  cards {f_} {k_}: {n_}")

    # Card text of this file (check 13): names, flavour and set text, by language. Mutable, outside the snapshot hash.
    # Italian is the fallback language, so texts.it must be complete: a name and a flavour for every card of the file, and
    # a name and a description for its set. Any other language may be partial, but must not have unknown keys and must keep
    # the same {placeholders}. A name is unique in its language across all the files (a deck list must tell two cards apart).
    placeholders = lambda t_: set(re.findall(r"\{(\w+)\}", t_))
    texts_doc = cards_doc.get("texts", {})
    if not isinstance(texts_doc, dict):
        err(file_label, "texts must be an object of languages")
        texts_doc = {}
    own_ids = [c_.get("id") for c_ in cards_doc.get("cards", [])]
    allowed = {f"card.{i_}.{f_}" for i_ in own_ids for f_ in ("name", "flavour")}
    allowed |= {f"set.{set_path.stem}.{f_}" for f_ in ("name", "description")}
    base_ct = texts_doc.get("it")
    if base_ct is None:
        err(file_label, "texts.it is missing: Italian is the fallback language and must exist")
    for loc, table in texts_doc.items():
        where = f"{file_label} texts.{loc}"
        if not re.fullmatch(r"[a-z]{2}(-[A-Z]{2})?", loc) or not isinstance(table, dict):
            err(where, "a language is a tag such as it or pt-BR, and its texts an object")
            continue
        for k, v in table.items():
            if not isinstance(v, str) or not v.strip():
                err(where, f"'{k}' must be a non-empty string")
                continue
            if k not in allowed:
                err(where, f"unknown key '{k}' (no such card in this file, or not its set)")
            if k.endswith(".name"):
                prev = all_names.setdefault((loc, v.strip().casefold()), (file_label, k))
                if prev[1] != k:
                    err(where, f"name '{v.strip()}' is also used by {prev[0]} {prev[1]}")
            if base_ct is not None and loc != "it" and isinstance(base_ct.get(k), str) and placeholders(base_ct[k]) != placeholders(v):
                err(where, f"'{k}' placeholders {sorted(placeholders(v))} differ from texts.it {sorted(placeholders(base_ct[k]))}")
    if isinstance(base_ct, dict):
        for k in sorted(allowed - base_ct.keys()):
            err(f"{file_label} texts.it", f"missing key '{k}'")

# every set a card names is defined by some set file (or it would only exist if an admin created it first)
for undefined in sorted(referenced_sets - all_defined_sets):
    err("sets/", f"a card is in the set {undefined}, which no file in sets/ defines")

# Labels (labels/<locale>.json): same keys and same {placeholders} in every locale, and every vocab constant has a label.
# Effect-related labels (keywords, evolutions, synergies) are deliberately not here yet: the MVP set is undecided.
labels_dir = root / "labels"
if labels_dir.is_dir():
    placeholders = lambda s: set(re.findall(r"\{(\w+)\}", s))
    locales = {p.stem: json.loads(p.read_text(encoding="utf-8")) for p in sorted(labels_dir.glob("*.json"))}
    base_name = "it" if "it" in locales else next(iter(locales), None)
    for loc, table in locales.items():
        for k, v in table.items():
            if not isinstance(v, str) or not v.strip():
                err(f"labels/{loc}.json", f"'{k}' must be a non-empty string")
    if base_name:
        base = locales[base_name]
        for loc, table in locales.items():
            if loc == base_name:
                continue
            for k in sorted(base.keys() - table.keys()):
                err(f"labels/{loc}.json", f"missing key '{k}' (present in {base_name}.json)")
            for k in sorted(table.keys() - base.keys()):
                err(f"labels/{loc}.json", f"extra key '{k}' (absent from {base_name}.json)")
            for k in base.keys() & table.keys():
                if isinstance(base[k], str) and isinstance(table[k], str) and placeholders(base[k]) != placeholders(table[k]):
                    err(f"labels/{loc}.json", f"'{k}' placeholders {sorted(placeholders(table[k]))} differ from {base_name}.json {sorted(placeholders(base[k]))}")
        slug = lambda s: re.sub(r"[^a-z0-9]+", "_", s.lower()).strip("_")
        c = vocab["constants"]
        # Hand types are keyed by the engine's HandType code (the keys of hand_multipliers_permille), as the
        # MatchLog writes them; the other groups by the slug of the data name, which equals the constant in lower case.
        hand_codes = [k for k in bal["hand_multipliers_permille"] if not k.startswith("_")]
        for code in hand_codes:
            if f"game.hand_type.{code.lower()}" not in base:
                err(f"labels/{base_name}.json", f"no label 'game.hand_type.{code.lower()}' for hand type '{code}'")
        wanted = [("faction", c["factions"]), ("suit", c["suits"]), ("phase", c["phases"]),
                  ("subfaction", [s_ for v_ in c["subfactions"].values() for s_ in v_])]
        for group, names in wanted:
            for n in names:
                if f"game.{group}.{slug(n)}" not in base:
                    err(f"labels/{base_name}.json", f"no label 'game.{group}.{slug(n)}' for vocab value '{n}'")

for w in warnings:
    print("WARN ", w)
for e_ in errors:
    print("ERROR", e_)
print(f"\n{len(errors)} error(s), {len(warnings)} warning(s)")
sys.exit(1 if errors else 0)
