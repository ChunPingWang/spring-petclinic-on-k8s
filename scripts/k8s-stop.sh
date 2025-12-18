#!/bin/bash
#
# 停止 Kubernetes 上的 Spring PetClinic 應用
#

set -e

NAMESPACE="${NAMESPACE:-petclinic}"

echo "正在停止 petclinic 應用 (namespace: $NAMESPACE)..."

# 檢查 namespace 是否存在
if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
    echo "警告: Namespace '$NAMESPACE' 不存在，無需停止"
    exit 0
fi

# 將所有 deployment 的副本數設為 0
kubectl scale deployment --all -n "$NAMESPACE" --replicas=0

echo ""
echo "等待 Pod 終止..."
kubectl wait --namespace "$NAMESPACE" \
    --for=delete pod \
    --all \
    --timeout=60s 2>/dev/null || true

echo ""
echo "目前 Pod 狀態:"
kubectl get pods -n "$NAMESPACE" 2>/dev/null || echo "  (無運行中的 Pod)"

echo ""
echo "✅ PetClinic 應用已停止"
echo ""
echo "如需重新啟動，請執行: ./scripts/k8s-start.sh"
