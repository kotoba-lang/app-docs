# lg-docs — langgraph-clj port (ADR-2606280030)

clj/bb port of the LangGraph **Python** appview `../lg/` (FastAPI + `langgraph`)
onto **langgraph-clj** + the kotoba Datom-log idiom (repo rule: clj/bb over kotoba,
langgraph-python → langgraph-clj).

## Status: CANONICAL (the Python twin has been deleted, ADR-2606280030)

This clj/bb tree is now the **canonical** lg-docs code. The DEV-stage Python appview
(`../lg/`) it was ported from has been **deleted** (founder directive "twin の py を削除",
2026-06-28): every `.py` had a verified `.cljc` twin (table below), nothing outside the
app imported `lg_docs`, and the app carried no cron (no `crons` in `langgraph.json`, no
`cron.py`/APScheduler — the Dockerfile only ran `uvicorn lg_docs.server:app`). The twin
suite stays green after the deletion (`bb run_tests.clj` → 23 tests / 54 assertions).

The Python→clj parity that justified the deletion:

| Python (deleted `lg/lg_docs/`) | clj/bb twin (`lg-clj/src/lg_docs/`) | notes |
|---|---|---|
| `graphs/health.py` (StateGraph) | `graph.cljc` | langgraph-clj `:probe` node, parity topology |
| `handlers.py` (async) | `handlers.cljc` | synchronous (store no longer async) |
| `docbody.py` | `docbody.cljc` | batchUpdate index engine; `apply-request` returns a new body |
| `mapping.py` | `mapping.cljc` | `:doc/*` datom mapping; JSON via cheshire |
| `store.py` | `store.cljc` | `DocStore` protocol + Fake (atom) + Kotoba records |
| `kotoba_datomic.py` (httpx) | `kotoba_datomic.cljc` | `httpx → babashka.http-client` (#2612) |
| `edn.py` | `edn.cljc` | tx-op builders; `pr-str` is the EDN encoder |
| `ids.py` | `ids.cljc` | slug / eid / DID / AT-URI |
| `gitoffice_normalize.py` | `gitoffice_normalize.cljc` | fractional-index parity preserved |
| `server.py` (FastAPI) | `server.cljc` | pure `handle-request` dispatcher + x-api-key auth |
| `tests/test_handlers.py` | `tests/lg_docs/handlers_test.cljc` | clojure.test |
| `tests/test_gitoffice_normalize.py` | `tests/lg_docs/gitoffice_normalize_test.cljc` | clojure.test |

## Test

```bash
cd 60-apps/etzhayyim-project-docs/lg-clj
bb run_tests.clj      # or: bb test   →  23 tests / 54 assertions, 0 failures
```

## Deviations (honest)

- **HTTP socket binding deferred.** `server.cljc` ports the routing + auth as a pure
  `handle-request` dispatcher (fully tested); binding it to a concrete listener
  (httpkit/jetty) is the remaining infra leg — a deployment-cutover follow-up, out of
  scope for this DEV-stage deletion (per ADR-2606280030: this is a deletion, not a
  deploy cutover). The langgraph.json `health` graph is fully ported to `graph.cljc`.
- **`kotoba_datomic.cljc` live legs unexercised** (no kotoba `:8077`/`docs-v1` engine in
  CI): the pure parts (CID/base32, EDN tx encode, datom folding) are ported; the
  `transact/q/pull` HTTP calls mirror the Python client shape via babashka.http-client.
