# Kubernetes Quick Start Guide

## Quick Deployment

### 1. Build and Push Docker Image

```bash
# Build image
docker build -t your-registry/hrm-tool-next-gen:latest .

# Push to registry
docker push your-registry/hrm-tool-next-gen:latest
```

### 2. Update Image Reference

Edit `k8s/app/deployment.yaml`:
```yaml
image: your-registry/hrm-tool-next-gen:latest
```

> **Secrets reminder:** copy `k8s/secrets.example.yaml` to `k8s/secrets.yaml`, fill in your credentials, and keep the file out of version control before running any deploy command.

### 3. Deploy

**Option A: Using the deployment script (Linux/Mac)**
```bash
chmod +x k8s/deploy.sh
./k8s/deploy.sh
```

**Option B: Using kubectl apply**
```bash
kubectl apply -k k8s/
```

**Option C: Manual deployment**
```bash
# Prepare namespace & config
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml   # local file based on secrets.example.yaml

# Data services
kubectl apply -f k8s/mysql/
kubectl apply -f k8s/redis/
kubectl apply -f k8s/rabbitmq/

# Application + policies
kubectl apply -f k8s/app/
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/network-policy.yaml
kubectl apply -f k8s/pod-disruption-budget.yaml

# Optional ingress
kubectl apply -f k8s/ingress.yaml
```

## Access Application

### Port Forward
```bash
kubectl port-forward svc/hrm-app-service 8081:8081 -n hrm-tool
# Access at http://localhost:8081
```

### RabbitMQ Management UI
```bash
kubectl port-forward svc/rabbitmq-service 15672:15672 -n hrm-tool
# Access at http://localhost:15672
# Username: rabbitmq_admin
# Password: rabbitmq_admin
```

## Check Status

```bash
# All pods
kubectl get pods -n hrm-tool

# All services
kubectl get svc -n hrm-tool

# Application logs
kubectl logs -f deployment/hrm-app -n hrm-tool
```

## Cleanup

```bash
# Using script
chmod +x k8s/undeploy.sh
./k8s/undeploy.sh

# Or using kubectl
kubectl delete -k k8s/
```

## Important Notes

1. **Storage**: Ensure your cluster has an appropriate StorageClass (defaults to `standard`). Update the PVC sections inside `mysql/statefulset.yaml`, `redis/deployment.yaml`, and `rabbitmq/statefulset.yaml` if you need a different class.

2. **Image Registry**: Update the image reference in `k8s/app/deployment.yaml` to point to your container registry.

3. **Secrets**: Copy `secrets.example.yaml` to `secrets.yaml`, keep it out of git, and prefer managed secrets (Vault, External Secrets Operator, etc.) for production.

4. **Ingress**: Update `k8s/ingress.yaml` with your domain name and configure TLS for production.

5. **Resource Limits**: Adjust CPU/memory requests and limits in deployment files based on your cluster capacity.

