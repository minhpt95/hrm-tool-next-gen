#!/bin/bash

# HRM Tool Next Gen - Kubernetes Deployment Script
# This script deploys all components to a Kubernetes cluster

set -e

NAMESPACE="hrm-tool"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SECRETS_FILE="$SCRIPT_DIR/secrets.yaml"

echo "========================================="
echo "HRM Tool Next Gen - Kubernetes Deployment"
echo "========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    print_error "kubectl is not installed or not in PATH"
    exit 1
fi

# Check if we can connect to cluster
if ! kubectl cluster-info &> /dev/null; then
    print_error "Cannot connect to Kubernetes cluster"
    exit 1
fi

print_status "Connected to Kubernetes cluster"
kubectl cluster-info | head -n 1
echo ""

# Check if namespace exists
if kubectl get namespace "$NAMESPACE" &> /dev/null; then
    print_warning "Namespace '$NAMESPACE' already exists"
    read -p "Do you want to continue? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    print_status "Creating namespace '$NAMESPACE'"
    kubectl apply -f "$SCRIPT_DIR/namespace.yaml"
fi

# Ensure secrets file exists (ignored by git)
if [ ! -f "$SECRETS_FILE" ]; then
    print_error "Missing secrets file at $SECRETS_FILE"
    print_error "Copy k8s/secrets.example.yaml to k8s/secrets.yaml and fill in your real credentials."
    exit 1
fi

# Deploy secrets
print_status "Deploying secrets..."
kubectl apply -f "$SECRETS_FILE"

# Deploy configmap
print_status "Deploying ConfigMap..."
kubectl apply -f "$SCRIPT_DIR/configmap.yaml"

# Deploy MySQL
print_status "Deploying MySQL..."
kubectl apply -f "$SCRIPT_DIR/mysql/"
print_status "Waiting for MySQL to be ready..."
kubectl wait --for=condition=ready pod -l app=mysql -n "$NAMESPACE" --timeout=300s || print_warning "MySQL readiness check timed out"

# Deploy Redis
print_status "Deploying Redis..."
kubectl apply -f "$SCRIPT_DIR/redis/"
print_status "Waiting for Redis to be ready..."
kubectl wait --for=condition=ready pod -l app=redis -n "$NAMESPACE" --timeout=120s || print_warning "Redis readiness check timed out"

# Deploy RabbitMQ
print_status "Deploying RabbitMQ..."
kubectl apply -f "$SCRIPT_DIR/rabbitmq/"
print_status "Waiting for RabbitMQ to be ready..."
kubectl wait --for=condition=ready pod -l app=rabbitmq -n "$NAMESPACE" --timeout=300s || print_warning "RabbitMQ readiness check timed out"

# Deploy Application + supporting manifests
print_status "Deploying HRM Application..."
kubectl apply -f "$SCRIPT_DIR/app/"
print_status "Applying autoscaling and policies..."
kubectl apply -f "$SCRIPT_DIR/hpa.yaml"
kubectl apply -f "$SCRIPT_DIR/network-policy.yaml"
kubectl apply -f "$SCRIPT_DIR/pod-disruption-budget.yaml"
print_status "Waiting for application to be ready..."
kubectl wait --for=condition=ready pod -l app=hrm-app -n "$NAMESPACE" --timeout=300s || print_warning "Application readiness check timed out"

# Deploy Ingress (optional)
if [ -f "$SCRIPT_DIR/ingress.yaml" ]; then
    read -p "Do you want to deploy Ingress? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status "Deploying Ingress..."
        kubectl apply -f "$SCRIPT_DIR/ingress.yaml"
    fi
fi

echo ""
echo "========================================="
print_status "Deployment completed!"
echo "========================================="
echo ""
echo "Get pod status:"
echo "  kubectl get pods -n $NAMESPACE"
echo ""
echo "View application logs:"
echo "  kubectl logs -f deployment/hrm-app -n $NAMESPACE"
echo ""
echo "Port forward to access application:"
echo "  kubectl port-forward -n $NAMESPACE svc/hrm-app-service 8081:80"
echo ""
echo "Access at: http://localhost:8081"
echo ""
