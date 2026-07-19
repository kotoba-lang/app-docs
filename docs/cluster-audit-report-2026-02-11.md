# クラスタリソース管理状態監査レポート

## 実施日時
2026年2月11日

## 概要

クラスタ上のリソースとリポジトリ管理状態を監査した結果、**重大な管理ギャップ**が発見されました。

## 🔴 主要な発見事項

### 1. Pulumi Applications 状態

| Application | Sync Status | Health | Path |
|------------|-------------|---------|------|
| etzhayyim-performers-org-org_34dKrNTTK3cNixZzHIzzFwLw1s4 | **OutOfSync** | Healthy | 50-infra/pulumi/apps |
| cluster-infrastructure | Unknown | Healthy | infra/cluster |
| auth-gateway | Unknown | Healthy | performers/sys-auth-gateway/k8s |
| performers-dashboard | Unknown | Healthy | performers/sys-dashboard/k8s |
| performers-namespace-resources | Unknown | Healthy | performers/sys-infra/namespace-resources |

### 2. 管理されていないリソース（Pulumi labelなし）

#### HTTPRoutes（全て unmanaged）
- `etzhayyim-performers-org-org_34dKrNTTK3cNixZzHIzzFwLw1s4` namespace: **60+個のHTTPRoutes**
- `etzhayyim-performers-org-org_34dKrNTTK3cNixZzHIzzFwLw1s4` namespace: 9個のHTTPRoutes

**問題**: クラスタ上の全HTTPRoutesにPulumi管理ラベルが付いていない

#### Deployments（大部分 unmanaged）
- `etzhayyim-performers-org-org_34dKrNTTK3cNixZzHIzzFwLw1s4` namespace: **50+個のDeployments**
- `etzhayyim-performers-org-org_34dKrNTTK3cNixZzHIzzFwLw1s4` namespace: 11個のDeployments

**問題**: アプリケーションDeploymentsがPulumiで管理されていない

#### その他のリソース
- ConfigMaps: 20+個が unmanaged
- StatefulSets: org-nats, org-postgres など

### 3. リポジトリ管理状況

#### infra/ ディレクトリ
- **212個のYAMLファイル** を管理
- 以下のコンポーネントを含む:
  - Pulumi Applications定義
  - Cluster infrastructure（新規統合）
  - legacy runtime components
  - KEDA ScaledObjects

#### performers/ ディレクトリ
- 複数のperformerプロジェクトがk8s/配下にマニフェストを持つ
- しかし、これらのマニフェストがクラスタに正しく適用されていない可能性

## 🔍 詳細分析

### ApplicationSet の動作状況

`performers.yaml` でApplicationSetが定義されており、以下のパターンでApplicationを生成:
```yaml
directories:
  - path: "performers/*/k8s"
  - path: "performers/*/*/k8s"
```

**問題**: 生成されたApplicationsがクラスタリソースにラベルを付けていない可能性

### クラスタリソースの実態

#### Namespaces
- `etzhayyim-performers-org-org_34dKrNTTK3cNixZzHIzzFwLw1s4`: 主要org namespace（Pulumi labelなし）
- `etzhayyim-performers-org-org_34dKrNTTK3cNixZzHIzzFwLw1s4`: Pulumi managed-by label **あり**
- システムnamespaces: cert-manager, legacy-runtime-system, envoy-gateway-system, keda, tekton-pipelines

#### 動作中のワークロード
- **50個のDeployments** が稼働中
- StatefulSets: NATS, PostgreSQL
- 多数のHTTPRoutesで外部公開

#### Custom Resources
- **48個のインフラ関連CRDs** (legacy runtime, Gateway API, Pulumi)

## ⚠️ リスク評価

### 高リスク
1. **Drift Detection不能**: Pulumi管理下にないため、手動変更を検出できない
2. **災害復旧困難**: リポジトリからクラスタを完全に復元できない
3. **監査証跡なし**: 誰がいつリソースを変更したか追跡不可
4. **自動修復不能**: selfHealが機能しない

### 中リスク
1. **ドキュメント不足**: クラスタの実態とリポジトリが乖離
2. **CI/CD統合不完全**: 自動デプロイが不完全な可能性

## 📋 推奨アクション

### 緊急（即時対応）
1. ✅ **cluster-infrastructure Applicationの同期**
   ```bash
   pulumi app sync cluster-infrastructure
   ```

2. ✅ **Root App of Appsの同期**
   ```bash
   pulumi app sync etzhayyim-performers-org-org_34dKrNTTK3cNixZzHIzzFwLw1s4
   ```

3. ✅ **Degraded Applicationの修復**（該当がある場合）

### 短期（1週間以内）
4. **ApplicationSet生成状況の確認**
   ```bash
   kubectl get applications -n pulumi -l app.kubernetes.io/part-of=etzhayyim-performers-org-org_34dKrNTTK3cNixZzHIzzFwLw1s4
   ```

5. **手動デプロイリソースの特定と移行**
   - クラスタ上の各Deploymentがどのperformerに対応するか調査
   - 対応するk8s/マニフェストが存在するか確認
   - 存在しない場合は `kubectl get -o yaml` でマニフェスト化

6. **Pulumi labelの一括付与**（慎重に実施）
   ```bash
   kubectl label deployment <name> -n <namespace> pulumi.argoproj.io/instance=<app-name>
   ```

### 中期（1ヶ月以内）
7. **performers配下の全k8s/マニフェストの棚卸**
   ```bash
   find performers -type d -name "k8s" -exec echo {} \;
   ```

8. **ApplicationSetの動作検証と修正**
   - ApplicationSetが正しくApplicationを生成しているか確認
   - 生成されたApplicationsがリソースにラベルを付けているか検証

9. **管理ポリシーの策定**
   - 全リソースはPulumi経由でデプロイすることを強制
   - 手動 `kubectl apply` を禁止（緊急時を除く）
   - Kyverno policyで未管理リソースを検出

### 長期（継続的改善）
10. **監視とアラート設定**
    - Pulumi managed labelがないリソースを定期スキャン
    - OutOfSync状態のApplicationを Slack通知

11. **ドキュメント整備**
    - 50-infra/README.md の維持
    - 各performerのk8s/にREADME追加

## 📊 統計情報

- **リポジトリYAMLファイル数**: 212 (infra配下のみ)
- **Pulumi Applications数**: 6
- **管理されていないHTTPRoutes**: 60+
- **管理されていないDeployments**: 50+
- **稼働中のNamespaces**: 14 (system除く)
- **インフラCRDs**: 48

## 結論

**クラスタリソースの大部分がリポジトリで正式に管理されていない状態**です。manifests統合により`infra/cluster/`は整理されましたが、`performers/`配下のアプリケーションリソースとクラスタの実態が乖離しています。

GitOpsベストプラクティスに準拠するため、全リソースのPulumi管理下への移行が必要です。
