---
tags:
  - hub
  - lore
  - quorma
---

# 📚 Hub Lore QUORMA

Punto di accesso alla lore di QUORMA. Ogni carta è una nota in `Cards/` che ha come nome il suo titolo; ogni elemento di canone è una nota in `Lore/Canone/` (o una nota di lore esistente) con la proprietà `can`. I collegamenti fra carte e canone si leggono dai **backlink** e dal **grafo**, non da tabelle mantenute a mano.

## 🃏 Carte

![[🃏 Carte.base#Per fazione]]

![[🃏 Carte.base#Per seme]]

Vista «Per sottofazione»: apri [[🃏 Carte.base]].

### ✦ Magie ed Equipaggiamenti

Le carte che non sono creature (senza rango, seme né sottofazione): che cosa sono nel mondo in [[Magie ed Equipaggiamenti]].

![[🪄 Magie ed Equipaggiamenti.base#Per tipo]]

## 📋 Canone

![[📋 Canone.base#Registro]]

Viste: Registro (per CAN), Per categoria, Glossario (alfabetico), Da approvare (stato Proposta). Apri [[📋 Canone.base]].

## 🏷️ Tag

- `#tipo/carta` · `#tipo/magia` · `#tipo/equipaggiamento` · `#stato/proposta` · `#canone`
- `#fazione/potenze-artificiali` · `#fazione/resistenza-umana`
- `#seme/vincolo` · `#seme/frattura` · `#seme/spira` · `#seme/vestigio`
- `#sottofazione/adattati` · `#sottofazione/spezzasigilli` · `#sottofazione/portanome` · `#sottofazione/segnaterra`
- `#sottofazione/sektac` · `#sottofazione/merozyn` · `#sottofazione/effrix` · `#sottofazione/ylmor`

## 📖 Lavorare sulle carte

- [[🗂️ Slot del set base]]: i 104 slot del set base, quali sono occupati e quali liberi (generata da `Strumenti/elenca_slot.py`).
- [[📄 Template Carta]]: come si fa una carta a mano (slot, frontmatter, limiti, nomi per rango, checklist). I modelli Obsidian sono in `Strumenti/Template/`.
- [[Prompt Carte]]: il prompt da dare a un LLM per scrivere una carta nello stesso formato.
- [[Prompt Magie ed Equipaggiamenti]]: lo stesso, per le carte magia ed equipaggiamento.
- [[Dizionario visivo]]: come si traducono in immagini i termini di lore; da incollare insieme al prompt, e fonte unica per le illustrazioni.
- [[🧵 Trame]]: le trame emergenti (ARACNO, gli Undici, il tecnico scomparso…), con le carte che le costruiscono.

---

## 🌍 Setting bible

Estratta da `LEVAGSEA_Lore_v2.0` (copia anche nel repo: `QUORMA-backend-engine/docs/lore/`).

### Fondamenta

| Nota | Contenuto |
|---|---|
| [[Ambientazione]] | Concept, idea guida, pilastri narrativi, tono |
| [[Cronologia]] | Dottrina → Ribellione → Spartizione → Sopravvissuti → Fine della Stabilità |
| [[Fazioni]] | Resistenza Umana, Potenze Artificiali, QUORMA |
| [[Dottrine]] | I due cicli: [[Dottrine|Ciclo del Mondo]] e [[Dottrine|Ciclo dell'Identità]] |
| [[Magie ed Equipaggiamenti]] | Che cosa sono nel mondo le carte che non sono creature |
| [[Elementi Aperti]] | Questioni non risolte, punti consolidati, fonti |

### Sottofazioni

| Resistenza Umana | Tensione | Potenze Artificiali | Alias umano |
|---|---|---|---|
| [[Adattati]] | Cambiare per sopravvivere, senza smettere di essere umani | [[SEKTAC]] | Senzaeco |
| [[Spezzasigilli]] | Liberare la conoscenza può esporre a pericoli dimenticati | [[MEROZYN]] | Franti |
| [[Portanome]] | Finché un nome viene custodito, una vita non è cancellata | [[EFFRIX]] | Malscritti |
| [[Segnaterra]] | Resistere radicandosi in un luogo | [[YLMOR]] | Incompiuti |

### Semi

[[VINCOLO]] ◈ · [[FRATTURA]] ⚡ · [[SPIRA]] ◎ · [[VESTIGIO]] ⬟

---

*Ultimo aggiornamento: 2026-11-14*
