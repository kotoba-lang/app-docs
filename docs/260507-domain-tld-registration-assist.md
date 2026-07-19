# domain.etzhayyim.com TLD Registration Assist

Date: 2026-05-07

## Scope

`domain.etzhayyim.com` is a T2 actor for TLD registration assistance. Phase 1 is
advisory and ledger-only: it checks eligibility, recommends registrars, and
writes a draft registration row for operator follow-up. It does not automate
registrar checkout, payment, WHOIS contact submission, DNS changes, or legal
verification.

## Architecture

- Runtime: shared `kotodama` LangServer worker.
- Worker module: `kotodama.primitives.domain`.
- BPMN contracts:
  - `etzhayyim-root/00-contracts/bpmn/com/etzhayyim/domain/eligibilityCheck.bpmn`
  - `etzhayyim-root/00-contracts/bpmn/com/etzhayyim/domain/registerAssist.bpmn`
  - `etzhayyim-root/00-contracts/bpmn/com/etzhayyim/domain/refreshTldCatalog.bpmn`
- XRPC lexicons:
  - `com.etzhayyim.apps.domain.eligibilityCheck`
  - `com.etzhayyim.apps.domain.registerAssist`
- Query/catalog lexicons:
  - `com.etzhayyim.apps.domain.coverage`
  - `com.etzhayyim.apps.domain.getTld`
  - `com.etzhayyim.apps.domain.listRegistrars`
  - `com.etzhayyim.apps.domain.listTlds`

## Graph Model

Phase 1 adds small public-reference catalog tables and one draft ledger table:

- `vertex_domain_tld`
- `vertex_domain_registrar`
- `vertex_domain_legal_regulator`
- `vertex_domain_eligibility_advice`
- `vertex_domain_registration`
- `edge_domain_registrar_supports_tld`
- `edge_domain_tld_accepts_regulator`
- `mv_domain_registrable_via`

The catalog is seeded by:

- `30-graph/graph-schema/migrations/20260507230000_vertex_domain_schema.ts`
- `30-graph/graph-schema/migrations/20260507230100_seed_domain_catalog_and_bpmn.ts`

## Process Behavior

`eligibilityCheck` resolves `(tld, jurisdiction, actorKind)` in this order:

1. Exact advice row for `(tld, jurisdiction, actorKind)`.
2. Jurisdiction fallback row for `(tld, jurisdiction, any)`.
3. Open TLD fallback when the TLD is not restricted.

`registerAssist` runs the same eligibility logic, ranks registrars for the TLD,
then appends `vertex_domain_registration` with:

- `status='planning'` when eligible.
- `status='blocked'` when the requested TLD is restricted and the actor does
  not qualify.

`refreshTldCatalog` is a monthly timer-start BPMN. Phase 1 returns catalog
counts only; Phase 2 should fetch policy URLs and diff stored excerpts.

## Operational Notes

- No Kubernetes resource is introduced for Phase 1; the existing shared
  `kotodama.worker_api` imports and registers the domain primitives.
- `pyproject.toml` version `0.3.71` marks the worker package change.
- Registration assistance is not legal advice. The output must keep source
  URLs and policy excerpts visible so an operator can verify the registry and
  regulator requirements before purchasing a domain.

## Follow-Ups

- Implement XRPC query handlers for `coverage`, `getTld`, `listTlds`, and
  `listRegistrars` if a direct appview surface is needed.
- Add `recordRegistration` to move a draft ledger row from `planning` to
  `active` after the registrar flow is completed.
- Extend `refreshTldCatalog` to fetch registry policy URLs and version advice
  rows when eligibility text changes.
