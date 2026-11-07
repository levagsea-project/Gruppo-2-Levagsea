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
        if want is not None and x[field] not in as_list(want):
            return False
    if "allowed_triggers" in rule:
        # whitelist rule: matches (= invalid) when effect matches and trigger not allowed
        return x["trigger_id"] not in rule["allowed_triggers"] and x["effect_id"] in as_list(rule["effect_id"])
    return True


def check_expansion(where, x, origin):
    t, e, s, d = x["trigger_id"], x["effect_id"], x["target_id"], x["duration"]
    tp = x.get("trigger_params", {})
    # 1 ids exist
    for kind, key, table in (("trigger", t, T), ("effect", e, E), ("target", s, S), ("duration", d, D)):
        if key not in table:
            err(where, f"unknown {kind} {key}")
            return
    for c in x.get("conditions", []):
        if c["id"] not in C:
            err(where, f"unknown condition {c['id']}")
    # 2 class
    cls = x["class"]
    tcls = class_of_trigger(t, tp)
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
    if ctx and ctx not in provides_of(t, tp):
        err(where, f"target {s} needs context '{ctx}' not provided by {t}")
    # 5 duration
    mode = E[e]["duration_mode"]
    if mode == "instant_only" and d != "D_Istantanea":
        err(where, f"effect {e} is instant_only, got {d}")
    if mode == "sustained_only" and d == "D_Istantanea":
        err(where, f"effect {e} is sustained_only, got {d}")
    # 6 class I only
    if set(E[e]["class"]) == {"I"}:
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
            err(where, f"E14 grants unknown keyword {p.get('keyword')}")
        else:
            schema, given = k.get("params", {}), p.get("keyword_params", {})
            for n, spec in schema.items():
                if spec.get("required") and n not in given:
                    err(where, f"E14 missing required keyword param {n}")
            for n in given:
                if n not in schema:
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
            err(where, f"unresolved $tune.{ref}")
    for ref in re.findall(r"\$param\.([a-z_]+)", json.dumps(x)):
        if ref not in kw.get(origin, {}).get("params", {}):
            err(where, f"$param.{ref} not declared by {origin}")


for k, v in kw.items():
    for i, x in enumerate(v["expands_to"]):
        if x["trigger_id"] == "T_EVO":  # 10
            err(f"keyword {k}[{i}]", "keywords must not use T_EVO")
        check_expansion(f"keyword {k}[{i}]", x, k)
    if v.get("balance_ref") not in bal["keywords"]:
        err(f"keyword {k}", "balance_ref missing in balance.json")
for k, v in evo.items():
    for i, x in enumerate(v["expands_to"]):
        if x["trigger_id"] != "T_EVO":
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
                err(f"ranks.json role_affinity {t}", f"unknown ability {i}")

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

for w in warnings:
    print("WARN ", w)
for e_ in errors:
    print("ERROR", e_)
print(f"\n{len(errors)} error(s), {len(warnings)} warning(s)")
sys.exit(1 if errors else 0)
