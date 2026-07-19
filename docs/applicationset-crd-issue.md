# ApplicationSet CRD が未インストール - 対処方法

## 問題

ApplicationSet CRDがクラスタにインストールされていないため、ApplicationSetを使用できません。

```bash
$ kubectl get applicationsets -n pulumi
error: the server doesn't have a resource type "applicationsets"
```

## 解決策オプション

### オプション1: ApplicationSet Controllerをインストール（推奨）

ApplicationSet ControllerはPulumi v2.3+で統合されていますが、別途有効化が必要な場合があります。

```bash
# Pulumi helmでインストールしている場合
helm upgrade pulumi argo/argo-cd \
  --namespace pulumi \
  --set applicationSet.enabled=true

# または、マニフェストで直接インストール
kubectl apply -n pulumi -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

### オプション2: 個別Applicationを作成（即時対応）

ApplicationSet Controllerがインストールできない場合、各performer用に個別のApplicationを作成します。

#### 自動生成スクリプト作成

```bash
#!/bin/bash
# 70-tools/70-tools/70-tools/scripts/generate-performer-applications.sh

PERFORMERS_DIRS=$(find performers -type d -name "k8s" -path "*/performers/*/k8s" | sort)

for dir in $PERFORMERS_DIRS; do
  performer_name=$(echo "$dir" | sed 's|performers/.*/performers/\(.*\)/k8s|\1|')
  app_file="50-infra/pulumi/apps/${performer_name}.yaml"

  cat > "$app_file" <<EOF
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: ${performer_name}
  namespace: pulumi
  labels:
    app.kubernetes.io/part-of: etzhayyim-performers-org-org_34dKrNTTK3cNixZzHIzzFwLw1s4
    1.etzhayyim.com: "true"
  finalizers:
    - resources-finalizer.pulumi.argoproj.io
spec:
  project: default
  source:
    repoURL: git@github.com:etzhayyim/etzhayyim-root.git
    targetRevision: HEAD
    path: ${dir}
  destination:
    server: https://kubernetes.default.svc
    namespace: etzhayyim-performers-org-org_34dKrNTTK3cNixZzHIzzFwLw1s4
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
      - ApplyOutOfSyncOnly=true
EOF

  echo "Created $app_file"
done
```

## 推奨アクション

1. **まずApplicationSet Controllerのインストールを試す**
2. それができない場合は個別Application作成スクリプトを実行
3. 既存のApplicationSetマニフェストは保持（将来の移行用）

## ApplicationSet Controllerインストール確認

```bash
# Pulumi Deploymentを確認
kubectl get deployment -n pulumi pulumi-applicationset-controller

# Pulumi version確認
pulumi version
```

Pulumi v2.3以上であればApplicationSetがサポートされているはずです。
