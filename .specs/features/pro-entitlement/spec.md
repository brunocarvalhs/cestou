# Feature Specification: Pro Entitlement — Monetization Scope & Rollout

## Status
Draft, PM-authored, 2026-10-23 — formalizes a direction bruno already set verbally. Does not
change any architecture decided in AD-010 (`.specs/STATE.md`); it fills in the business-rule gap
AD-010 explicitly left open ("while the paid business rules aren't finalized").

## Problem Statement
`FeatureFlagKeys.BILLING_ENABLED` has defaulted OFF since it shipped (commit `0a871c6e`), so
`SubscriptionRepository.observeStatus()` reports every user as `PRO` regardless of real Play
Billing state. Meanwhile Play Billing purchases keep happening for real and get acknowledged
(`PlayBillingSubscriptionRepository.acknowledge`) — **today there are real paying customers
receiving zero differentiation from non-paying users.** This is a product/ethics gap, not just an
unfinished feature: people are being charged for something the app currently gives everyone for
free. Separately, the Paywall (`PaywallScreen.kt`) advertises four benefits, and one of them
(Wear OS access) is not actually wired to subscription status at all — see FR-04 below.

This spec exists to (1) confirm the entitlement boundary already implemented in code is the
right one, (2) decide the free-chat message limit, (3) prioritize Pro-exclusive features
(shipped + candidates), and (4) sequence turning `BILLING_ENABLED` on safely.

## Scope boundary (confirmed)
Pro is positioned as **"the app's collaboration + AI assistant unlock,"** not "the app's core
trust features unlock." Every feature already gated or proposed for gating sits on the
*collaboration/convenience* side; every feature the budget-sensitive personas (Marina, Dona
Marlene, Dona Célia, Eduardo) rely on to trust the app at all — running total, budget alerts,
purchase history, price comparison — stays free. This boundary must hold for any future Pro
candidate: paywalling a feature a persona already treats as table-stakes-free reads as a
bait-and-switch, which is a sharper risk here than for most apps because the real user base is
tiny (2 known real accounts) and retention already falls to near-zero after week 1
(`project_auth_user_base_small.md`) — there is no slack to absorb a trust hit.

## User Stories

### FR-01: Unlimited AI chat is Pro; Free gets exactly one message
**Status**: Implemented, idle behind the flag. `ChatAiGateViewModel` combines
`SubscriptionRepository.observeStatus()` with `AiTrialRepository.hasUsedFreeMessage()`
(`core/data/.../AiTrialRepositoryImpl`, DataStore-backed, `markFreeMessageUsed()` persisted
`NonCancellable` so a fast back-out doesn't "refund" the trial). No code change needed to ship
this once `BILLING_ENABLED=true`.

**Acceptance Criteria**:
1. WHERE a Free user has not sent a chat message before, THEN they SHALL be allowed to send
   exactly one.
2. WHERE a Free user has already sent their one free message, THEN further sends SHALL be
   blocked and routed to the Paywall.
3. WHERE a user is Pro, THEN message count SHALL be unlimited.

**Decision on the limit — keep it at 1, don't change the number.** 28-day GA4 data: of 430 users
who open the chat screen, only 160 (37%) send any message at all, and among senders the mean is
1.16 messages/user. That mean comes from an **uncapped** sample (the flag has always been off, so
every one of those 160 users already had unlimited sends) — it is the true organic distribution,
not an artifact of a cap. Reconstructing the shape: if the large majority of the 160 senders sent
exactly 1 message, only a small minority (rough estimate: ~1 in 8 of them, ~20 people total out of
430 who even opened the screen) sent 2 or more. A free limit of 1 therefore does not cost the
typical user anything they would have done anyway — it only binds the already most-engaged tail,
which is exactly where a paywall should sit (shown after someone has gotten enough value to want
more, not before they've gotten any). **Caveat, not a reason to change the number now:** this
distribution comes from a tiny, non-representative beta cohort (950 opt-in testers, near-zero
week-1+ retention) behaving with no cap and no stakes — real behavior once a cap and a price are
visible may differ (people sometimes ration their one try more carefully, or abandon chat
entirely rather than hit a wall). Don't re-tune the number off this data again until there's
post-paywall production data to look at (see Rollout Plan, step 5).

### FR-02: Sharing a list is Pro
**Status**: Implemented, idle behind the flag. `CartSubscriptionGateViewModel.isPro` gates
`CartGraph.kt`'s `onShareShopping` intent — Free users get routed to
`Paywall(source = PAYWALL_SOURCE_SHARE)` instead of the native share sheet.

**Acceptance Criteria**: as implemented; no change requested.

**Persona risk flagged, not resolved here (bruno's call, see "Open risk" below):** this gate
covers the *outbound share* action (`ShareShoppingUseCase`, text/intent share), not list
membership itself — a Free user can still be *invited into* and fully edit a shared list someone
else created. That distinction matters because Lucas and Bianca-e-Diego's entire reason to use
the app is a live shared cart; if a future change widened this gate to block being a member (not
just initiating the share), it would directly block the core loop for exactly the two personas
whose headline use case *is* collaboration. Nothing in the current code does that — flagging so
nobody "simplifies" the gate later without noticing the blast radius.

### FR-03: QR-code list invite is Pro
**Status**: Implemented, idle behind the flag. `ShoppingSubscriptionGateViewModel.isPro` gates
the QR invite generation entry point in `feature/shopping`. Same persona caveat as FR-02 applies
(generating an invite is gated; joining via one, and being a member, is not).

**Acceptance Criteria**: as implemented; no change requested.

### FR-04: Wear OS access — NOT actually a Pro feature today (code/copy mismatch)
**Status**: Mismatch. `PaywallScreen.kt`'s `paywall_benefit_wear` string
("Use o Cestou no seu relógio Wear OS") implies Wear access is part of the Pro unlock. The real
gate is `ProfileSubscriptionViewModel.showWearLink`, which reads only
`FeatureFlagKeys.WEAR_LOGIN_ENABLED` — **it has no reference to `SubscriptionStatus` at all.**
`WEAR_LOGIN_ENABLED` defaults OFF for every user, Pro or Free, because the Wear OS companion app
isn't published yet (per that flag's own doc comment in `RemoteConfigKeys.kt`). Net effect: once
`WEAR_LOGIN_ENABLED` is eventually flipped on, Wear pairing becomes available to **every** user,
Pro or Free — the Paywall will have been selling something it structurally cannot deliver as an
exclusive. This is a real discrepancy between a "Verified"-adjacent paywall and the code behind
it, exactly the kind of gap `MVP-ROADMAP.md`'s process asks to be surfaced rather than silently
decided. Tracked as **G19** in `MVP-ROADMAP.md`.

**Acceptance Criteria (recommended, pending bruno)**:
1. WHERE `BILLING_ENABLED` is turned on, THEN the Paywall SHALL NOT claim a benefit the
   entitlement system cannot actually enforce.
2. Two ways to close this, either is acceptable, bruon decides: (a) remove
   `paywall_benefit_wear` from the Paywall now (cheap, one string + one list entry, zero risk) and
   revisit "Wear as a Pro perk" as its own decision once the companion app ships; or (b) wire a
   second check so Wear pairing requires both `WEAR_LOGIN_ENABLED` *and* `SubscriptionStatus.PRO`
   before it's offered — more honest to the current paywall copy, but adds scope to a flag that
   was deliberately kept simple ("whether the entry point even shows up," not an entitlement
   check) and changes behavior for a feature not yet live for anyone.
3. **Recommendation: (a).** Nothing about Wear pairing is ready to be sold as a perk yet (no
   companion app in the Play Store), so advertising it is premature regardless of the entitlement
   question — fixing the copy now avoids a second, compounding promise-vs-code gap.

### FR-05: New Pro-feature candidates (not built, prioritized for consideration — not a commitment)
No new engineering requested by this spec; this is the PM's ranked backlog for *after* FR-01–04
ship, cross-checked against the persona skill and the scope boundary above.

1. **Purchase-history export / month-end summary** — fits Anderson (MEI, manually re-keys every
   receipt into Excel each month) and Rafael (reseller, needs precise cost history) exactly:
   both already *want* their own historical data consolidated, which is a genuine incremental
   convenience on top of the free "see your list and total" promise, not a removal of something
   they already consider free. Best next candidate once FR-01–04 are live and there's room to
   build something new.
2. **Recurring/reusable list templates** — fits Rodrigo (prints the same checklist monthly).
   Speculative: no usage data yet validates demand (no reusable-list feature exists today to
   measure against), so this is a "validate with a cheap signal before building" item, not a
   build-now item.
3. **Price comparison across stores / unit-price history** — fits Dona Marlene and Eduardo
   directly, but **do not build this as Pro** without re-reading the scope boundary above first:
   `PERSONA-ACTION-PLAN.md` already treats "comparação de preço real" as a planned feature for
   *all* users (it's the thing that makes the app trustworthy to the most budget-sensitive
   personas). Paywalling it later would contradict that plan and read as bait-and-switch to
   exactly the personas least tolerant of friction. Listed here only to explicitly rule it out of
   the Pro track, not to recommend it.

### FR-06: "No ads for Pro" — explicitly deferred, not a candidate right now
Bruno raised this as an idea to explore, not a decision. PM recommendation: **don't build it now.**
- **No ad infrastructure exists at all** in this codebase (confirmed: zero references to
  `admob`/`play-services-ads` anywhere in the repo). "No ads as a Pro perk" requires first
  *introducing* ads to the Free tier — new SDK dependency, ad-unit placement UX design, mediation/
  consent flow (GDPR/LGPD — this app serves Brazil), and a Play Console ads-content policy
  declaration. This is a full feature initiative, not a flag flip, unlike FR-01–03.
- **The economics don't support it yet.** Ad revenue scales with impressions; the real user base
  is 2 known accounts plus ~950 opt-in beta testers whose retention already falls to near-zero
  after week 1. Any ad revenue at this scale is negligible — the engineering cost of building and
  maintaining ad infra is disproportionate to what it would earn for a long while.
- **The risk is asymmetric and compounds an existing problem.** Introducing ads changes something
  for every Free user at once, right when the app is still trying to establish basic trust and
  retention (the stated reason retention falls off a cliff after week 1 is not yet diagnosed — see
  G17 in `MVP-ROADMAP.md` — so this would add a second, simultaneous variable on top of an
  unexplained churn problem, making it harder to tell which change caused what).
- **Recommendation**: revisit only after (a) FR-01–04 have shipped and had at least one full
  release cycle to show real conversion data, and (b) organic DAU/retention is large enough that
  ad revenue is no longer rounding-error-sized. Keep it in the Phase-3/post-beta backlog in
  `MVP-ROADMAP.md`, not this quarter.

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
|---|---|---|---|
| FR-01 | Unlimited AI chat (Pro) / 1 free message | Execution | **Implemented, idle behind `BILLING_ENABLED`** |
| FR-02 | Share list (Pro) | Execution | **Implemented, idle behind `BILLING_ENABLED`** |
| FR-03 | QR-code invite (Pro) | Execution | **Implemented, idle behind `BILLING_ENABLED`** |
| FR-04 | Wear OS access as a Pro benefit | — | **Mismatch — paywall copy promises it, code doesn't gate it by entitlement at all. Tracked as G19.** |
| FR-05 | New Pro candidates (purchase-history export, templates) | Backlog | Not started — post-FR-01..04 |
| FR-06 | "No ads" Pro perk | Backlog | **Deferred — no infra exists, premature at current scale** |

## Rollout Plan — turning `BILLING_ENABLED` on

Ordered; each step gates the next. This is urgent (real users are paying for nothing today) but
"urgent" means "this sprint," not "skip verification":

1. **Fix the FR-04 copy mismatch first** (remove `paywall_benefit_wear` from the Paywall list) —
   trivial, zero risk, and avoids flipping the flag while the Paywall still overpromises.
2. **Device verification of the three real gates** (FR-01/02/03), requested from
   `android-engineer-quality`: these gates have only ever executed with
   `BILLING_ENABLED=false` (where everyone is PRO and the Free branch never runs in practice).
   Confirm on a device, with a real sandbox/test Play Billing purchase, that a genuine FREE status
   actually blocks share/QR/second-chat-message and routes to the Paywall, and that a genuine PRO
   purchase actually unblocks all three. This is new real-world exercise of existing code, not new
   code.
3. **Close the analytics gap** with `data-engineer`: today only `paywall_subscribe_clicked` and
   `paywall_purchase_failed` exist in `AnalyticsEvents.kt` — there is no `paywall_viewed`, no
   purchase-*success* event, and no event for "a Free user hit a gate and got routed to the
   Paywall" (with which gate: share/QR/chat). Without those, flipping the flag is unmeasurable:
   nobody will be able to tell whether the gates are even being hit, let alone converting. Add, at
   minimum: `paywall_viewed` (param: `source` = share/qr/chat), a purchase-success event
   (`paywall_purchase_completed` or similar), and a gate-blocked event per FR-01/02/03 call site.
4. **Give the 2 known real paying users a heads-up** before flipping — they purchased expecting
   something and got the same free experience everyone else did; a quiet behavior change under
   them is worse than a short message explaining Pro features are now live. Concierge-sized
   outreach, not a release-notes line, given the base is exactly 2 people.
5. **Flip `BILLING_ENABLED=true` in the Firebase console** (per AD-010, no app update required).
6. **Watch the new analytics from step 3** for at least one full session/cycle post-flip before
   calling this closed — specifically: are Free users actually hitting the gates (paywall_viewed
   counts), and is anyone converting (subscribe_clicked → purchase_completed). If gate-blocked
   counts are near-zero, that's a signal the gated features themselves aren't being discovered,
   not that pricing/limits are wrong — don't re-tune FR-01's message limit off silence.

## Open risk for bruno (not resolved here)
Gating list-sharing/QR-invite behind Pro is a deliberate, already-made call, but it is worth
bruno seeing it named plainly once: Lucas and Bianca-e-Diego's *only* reason to use this app at
all is a live shared cart (`.claude/skills/customer-personas/SKILL.md`), and the current gates sit
right at the edge of that — not blocking membership in a shared list, only the actions that start
one (share/invite). If churn data later shows these two persona-shaped segments bouncing right
after hitting a share/QR paywall, that is the first place to look before assuming the AI-chat
limit is the problem.
