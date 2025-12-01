#!/bin/bash

# HRM Tool Next Gen - Kubernetes Undeployment Script
# This script removes all components from a Kubernetes cluster

set -e

NAMESPACE="hrm-tool"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================="
echo "HRM Tool Next Gen - Kubernetes Undeployment"
echo "========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if namespace exists
if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
    print_warning "Namespace '$NAMESPACE' does not exist. Nothing to undeploy."
    exit 0
fi

print_warning "This will delete ALL resources in the '$NAMESPACE' namespace, including persistent data!"
read -p "Are you sure you want to continue? (yes/no): " -r
if [[ ! $REPLY == "yes" ]]; then
    print_status "Undeployment cancelled."
    exit 0
fi

# Delete resources
print_status "Deleting application..."
kubectl delete -f "$SCRIPT_DIR/app/" --ignore-not-found=true || true

print_status "Deleting autoscaling and network policies..."
kubectl delete -f "$SCRIPT_DIR/hpa.yaml" --ignore-not-found=true || true
kubectl delete -f "$SCRIPT_DIR/network-policy.yaml" --ignore-not-found=true || true
kubectl delete -f "$SCRIPT_DIR/pod-disruption-budget.yaml" --ignore-not-found=true || true

print_status "Deleting RabbitMQ..."
kubectl delete -f "$SCRIPT_DIR/rabbitmq/" --ignore-not-found=true || true

print_status "Deleting Redis..."
kubectl delete -f "$SCRIPT_DIR/redis/" --ignore-not-found=true || true

print_status "Deleting MySQL..."
kubectl delete -f "$SCRIPT_DIR/mysql/" --ignore-not-found=true || true

if [ -f "$SCRIPT_DIR/ingress.yaml" ]; then
    print_status "Deleting Ingress..."
    kubectl delete -f "$SCRIPT_DIR/ingress.yaml" --ignore-not-found=true || true
fi

print_status "Deleting ConfigMap and Secrets..."
kubectl delete -f "$SCRIPT_DIR/configmap.yaml" --ignore-not-found=true || true
kubectl delete -f "$SCRIPT_DIR/secrets.yaml" --ignore-not-found=true || true

# Wait a bit for resources to be cleaned up
sleep 5

# Delete namespace (this will delete all remaining resources)
read -p "Do you want to delete the namespace '$NAMESPACE'? This will delete ALL remaining resources. (yes/no): " -r
if [[ $REPLY == "yes" ]]; then
    print_status "Deleting namespace '$NAMESPACE'..."
    kubectl delete namespace "$NAMESPACE" --timeout=120s || true
    print_status "Namespace deleted."
else
    print_status "Namespace '$NAMESPACE' retained. Clean up manually if needed."
fi

echo ""
echo "========================================="
print_status "Undeployment completed!"
echo "========================================="
echo ""
print_warning "Note: Persistent Volume Claims (PVCs) may still exist if namespace was not deleted."
echo "To remove PVCs manually, run:"
echo "  kubectl get pvc -n $NAMESPACE"
echo "  kubectl delete pvc <pvc-name> -n $NAMESPACE"
echo ""
