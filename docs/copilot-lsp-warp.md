# Copilot CLI / copilot-lsp — Warp 向けセットアップ

概要
- Warp はターミナルエミュレータであり、一般的な LSP クライアントではありません。つまり、直接 Warp から LSP を使ってコード補完や補助を受けることは通常できません。
- 選択肢:
  1. Copilot CLI を Warp 上で直接使う（`copilot` コマンドを実行してチャットや補助を利用する）。
  2. `copilot-lsp` を起動して、Neovim / VS Code 等のエディタから LSP 経由で Copilot を利用する。

インストール（推奨手順）

1. GitHub CLI がある場合（簡単）

   gh extension install github/copilot-cli

   - この方法でインストールされると `copilot` サブコマンドが利用可能になります。もし `copilot-lsp` が提供されている場合は LSP サーバも使えるはずです。

2. 手動インストール（確実）

   - リリースページ: https://github.com/github/copilot-cli/releases
   - OS/アーキテクチャに合ったバイナリをダウンロードし、PATH に置いて実行権限を付与します。

起動例

- LSP サーバ（stdio 例）:

  copilot-lsp --stdio &>~/copilot-lsp.log &

- または Copilot CLI の LSP サブコマンドがある場合:

  copilot lsp --stdio &>~/copilot-lsp.log &

Warp での使い方

- Warp 自体は LSP クライアントではないため、直接エディタ相当の補完を Warp 内で受けるのは難しいです。
- 代替案:
  - (1) Warp 上で `copilot` コマンドを直接実行して、チャットや簡単な補助を利用する。
  - (2) `copilot-lsp` をバックグラウンドで起動し、別のエディタ（例: Neovim / VS Code）から接続して利用する。

エディタ接続のサンプル（メモ）

- Neovim (nvim-lspconfig) の例:

```lua
require'lspconfig'.copilot_lsp.setup{
  cmd = {"copilot-lsp", "--stdio"},
  filetypes = {"*"},
}
```

- VS Code: 専用拡張がない場合は汎用 LSP クライアントで接続する必要があります（設定に `cmd: ["copilot-lsp","--stdio"]` を指定する等）。

次のステップ

- Copilot CLI のインストールを代行するスクリプトを作成しました (70-tools/70-tools/70-tools/scripts/setup-copilot-lsp-warp.sh)。Warp上で直接使うのか、どのエディタから LSP 接続するか教えてください（例: Neovim, VS Code）。
