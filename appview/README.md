# etzhayyim-project-docs App migration

このディレクトリは `legacy-runtime` 実装を残したまま、App 版を段階移行するための配置先です。

## 対象 App services

- `etzhayyim-docs-ufrumx68`
- `docs-actor-gen1n2iy`
- `docs-svc-embed-7hqx05lz`
- `docs-svc-graphql-kr40xmug`

## App 実装方針

- 各 service は `projects/*/wasm/*-component` として順次実装。
- 既存 App runtime は互換運用のため維持。
- HTTP/cron/job エンドポイントから優先して移植。

## docs app 構成

- docs UI + API は `docs-performers-r5ycqp6x` の単一 Kotodama app (`kotodama.toml`) で管理。
- docs MCP component は `docs-performers-r5ycqp6x/components/docs-mcp-component` 配下に配置。
- ルーティング:
  - `/api/...` -> `docs-performers-r5ycqp6x`
  - `/mcp/...` -> `docs-mcp-component`
  - `/...` -> `fileserver` (`svelte/build/`)
