#!/bin/bash
#
# 啟動 Kubernetes 上的 Spring PetClinic 應用
#

set -e

NAMESPACE="${NAMESPACE:-petclinic}"
REPLICAS="${REPLICAS:-1}"

echo "正在啟動 petclinic 應用 (namespace: $NAMESPACE, replicas: $REPLICAS)..."

# 檢查 namespace 是否存在
if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
    echo "錯誤: Namespace '$NAMESPACE' 不存在"
    echo "請先執行: kubectl apply -k k8s/overlays/dev"
    exit 1
fi

# 將所有 deployment 的副本數設為指定數量
kubectl scale deployment --all -n "$NAMESPACE" --replicas="$REPLICAS"

echo ""
echo "等待 Pod 就緒..."
kubectl wait --namespace "$NAMESPACE" \
    --for=condition=ready pod \
    --all \
    --timeout=300s || true

echo ""
echo "目前 Pod 狀態:"
kubectl get pods -n "$NAMESPACE"

echo ""
echo "✅ PetClinic 應用已啟動"
echo ""
echo "存取網址:"
echo "  - 前端 UI:    http://petclinic.local/"
echo "  - Zipkin:     http://petclinic.local/zipkin"
echo "  - Grafana:    http://petclinic.local/grafana"
