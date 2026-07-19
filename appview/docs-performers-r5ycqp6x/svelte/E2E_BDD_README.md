# E2E BDD Testing for Docs Service

## 概要

このディレクトリには、Docs Service用のE2E（End-to-End）BDD（Behavior-Driven Development）テストが含まれています。

## セットアップ

### 依存関係のインストール

```bash
pnpm install
```

### Playwrightブラウザのインストール

```bash
npx playwright install
```

## 使用方法

### BDDテストの実行

```bash
# すべてのBDDテストを実行（ヘッドレスモード）
pnpm test:bdd

# ウォッチモードで実行
pnpm test:bdd:watch

# デバッグモード（ヘッドフルモード、ブラウザが表示される）
pnpm test:bdd:debug
```

### 環境変数

- `DOCS_BASE_URL`: テスト対象のURL（デフォルト: `https://docs.systems.etzhayyim.com`）
- `HEADLESS`: `false`に設定するとヘッドフルモードで実行（デフォルト: `true`）
- `CI`: CI環境では自動的にヘッドレスモードになる

## ディレクトリ構造

```
e2e/
├── features/
│   ├── support/
│   │   ├── world.ts          # Cucumber World object (Playwright統合)
│   │   └── hooks.ts          # Before/After hooks
│   ├── step_definitions/
│   │   └── common.steps.ts   # 共通ステップ定義
│   ├── docs-management.feature
│   └── generated/            # 自動生成されたfeatureファイル（gitignore対象）
└── reports/                   # テストレポート（gitignore対象）
    ├── cucumber-report.json
    └── cucumber-report.html
```

## デバッグ方法

### 1. ヘッドフルモードで実行

```bash
pnpm test:bdd:debug
```

### 2. 特定のfeatureファイルのみ実行

```bash
TS_NODE_PROJECT=tsconfig.cucumber.json cucumber-js --require-module ts-node/register --require 'e2e/features/**/*.ts' e2e/features/docs-management.feature
```

### 3. 特定のシナリオのみ実行（タグを使用）

featureファイルにタグを追加：
```gherkin
@debug
Scenario: Create a new page
  ...
```

実行：
```bash
TS_NODE_PROJECT=tsconfig.cucumber.json cucumber-js --require-module ts-node/register --require 'e2e/features/**/*.ts' --tags @debug e2e/features/**/*.feature
```

### 4. スクリーンショットを取得

step definitionsでスクリーンショットを取得：
```typescript
await this.page.screenshot({ path: 'debug-screenshot.png' });
```

### 5. コンソールログを確認

```typescript
this.page.on('console', msg => console.log('Browser console:', msg.text()));
```

## Featureファイルの例

```gherkin
Feature: Docs Management
  As a user
  I want to manage documents
  So that I can create, edit, and organize documents

  Background:
    Given I initialize the browser
    And I am logged in to Docs Service

  Scenario: View workspace list
    Given I am on the "/" page
    Then I should see "etzhayyim Docs"
```

## トラブルシューティング

### タイムアウトエラー

`e2e/features/support/hooks.ts`の`setDefaultTimeout`を増やす：
```typescript
setDefaultTimeout(60 * 1000); // 60秒
```

### 認証エラー

Clerkの認証状態を保存して再利用：
```typescript
// world.tsでstorageStateを設定
this.storageState = 'playwright/.auth/user.json';
```

### 要素が見つからない

要素の待機時間を増やす：
```typescript
await button.waitFor({ state: "visible", timeout: 30000 }); // 30秒
```

## 参考

- [Cucumber.js Documentation](https://github.com/cucumber/cucumber-js)
- [Playwright Documentation](https://playwright.dev/)
- [Gherkin Syntax](https://cucumber.io/docs/gherkin/)
