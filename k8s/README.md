# Kubernetes Deployment Guide for HRM Tool Next Gen

This directory contains Kubernetes manifests for deploying the HRM Tool Next Gen application and its dependencies.

## Prerequisites

- Kubernetes cluster (v1.24+)
- kubectl configured to access your cluster
- Docker image for the application built and pushed to a registry

## Architecture

The deployment includes:

- **MySQL 8.0** - Primary database (StatefulSet)
- **Redis 7** - Caching and session storage (Deployment)
- **RabbitMQ 3** - Message queue (StatefulSet)
- **HRM Application** - Spring Boot application (Deployment)

## Directory Structure

```
k8s/
├── namespace.yaml          # Namespace definition
├── secrets.example.yaml    # Template for credentials (copy to secrets.yaml)
├── configmap.yaml          # Application configuration
├── mysql/
│   ├── statefulset.yaml    # MySQL StatefulSet
│   └── service.yaml        # MySQL Service
├── redis/
│   ├── deployment.yaml     # Redis Deployment with PVC
│   └── service.yaml        # Redis Service
├── rabbitmq/
│   ├── statefulset.yaml    # RabbitMQ StatefulSet
│   └── service.yaml        # RabbitMQ Service
├── app/
│   ├── deployment.yaml     # Application Deployment
│   └── service.yaml        # Application Services (ClusterIP & NodePort)
├── ingress.yaml            # Ingress for external access
├── hpa.yaml                # Horizontal Pod Autoscaler
├── network-policy.yaml     # NetworkPolicies for app + data services
├── pod-disruption-budget.yaml # Ensures at least one app pod stays up
├── kustomization.yaml      # Kustomize configuration
├── deploy.sh / undeploy.sh # Helper scripts
├── QUICKSTART.md           # TL;DR instructions
├── secrets.example.yaml    # Template for secrets (copy to secrets.yaml)
└── README.md              # This file
```

## Quick Start

### 1. Prepare Secrets

Create a non-committed `secrets.yaml` from the provided template and fill in real values:

```bash
cp k8s/secrets.example.yaml k8s/secrets.yaml
# Edit secrets.yaml and update:
# - MySQL passwords
# - RabbitMQ credentials
# - JWT secrets
# - AWS / SMTP credentials (if needed)
```

`k8s/.gitignore` is configured to ignore `secrets.yaml`, so your credentials stay local.  
**Important**: For production, prefer sealed-secrets, external-secrets, or another secrets management solution.

### 2. Build and Push Docker Image

Build your Docker image and push it to a container registry:

```bash
# Build the image
docker build -t your-registry/hrm-tool-next-gen:latest .

# Push to registry
docker push your-registry/hrm-tool-next-gen:latest
```

### 3. Update Image Reference

Update the image reference in `k8s/app/deployment.yaml`:

```yaml
image: your-registry/hrm-tool-next-gen:latest
```

### 4. Deploy Using kubectl

Deploy all resources via Kustomize (recommended) or use the helper script:

```bash
# Apply with kustomize
kubectl apply -k k8s/

# Or run the scripted workflow (handles readiness checks)
./k8s/deploy.sh
```

### 5. Verify Deployment

Check the status of all resources:

```bash
# Check pods
kubectl get pods -n hrm-tool

# Check services
kubectl get svc -n hrm-tool

# Check deployments and statefulsets
kubectl get deploy,statefulset -n hrm-tool

# View application logs
kubectl logs -f deployment/hrm-app -n hrm-tool
```

## Deployment Steps

### Step 1: Create Namespace

```bash
kubectl apply -f k8s/namespace.yaml
```

### Step 2: Create Secrets

```bash
kubectl apply -f k8s/secrets.yaml   # local file created from secrets.example.yaml
```

### Step 3: Create ConfigMap

```bash
kubectl apply -f k8s/configmap.yaml
```

### Step 4: Deploy Infrastructure Services

Deploy MySQL, Redis, and RabbitMQ:

```bash
# MySQL
kubectl apply -f k8s/mysql/

# Redis
kubectl apply -f k8s/redis/

# RabbitMQ
kubectl apply -f k8s/rabbitmq/
```

Wait for infrastructure services to be ready:

```bash
kubectl wait --for=condition=ready pod -l app=mysql -n hrm-tool --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n hrm-tool --timeout=300s
kubectl wait --for=condition=ready pod -l app=rabbitmq -n hrm-tool --timeout=300s
```

### Step 5: Deploy Application & Policies

```bash
kubectl apply -f k8s/app/
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/network-policy.yaml
kubectl apply -f k8s/pod-disruption-budget.yaml
```

### Step 6: Configure Ingress (Optional)

```bash
kubectl apply -f k8s/ingress.yaml
```

## Accessing the Application

### Option 1: Port Forwarding

```bash
# Forward local port to service
kubectl port-forward -n hrm-tool svc/hrm-app-service 8081:80

# Access at http://localhost:8081
```

### Option 2: NodePort

The deployment includes a NodePort service. Access via:
- `http://<node-ip>:30081`

### Option 3: Ingress

If you've configured the ingress and have an ingress controller:

1. Add the host to your `/etc/hosts`:
   ```
   <ingress-ip> hrm-tool.local
   ```

2. Access at:
   - `http://hrm-tool.local`
   - `http://hrm-tool.local/swagger-ui.html`

## Monitoring and Health Checks

All services include health checks:

- **Application**: `/actuator/health`
- **MySQL**: `mysqladmin ping`
- **Redis**: `redis-cli ping`
- **RabbitMQ**: `rabbitmq-diagnostics ping`

Check health status:

```bash
# Application health
kubectl exec -n hrm-tool deployment/hrm-app -- curl http://localhost:8081/actuator/health

# View all pod statuses
kubectl get pods -n hrm-tool -o wide
```

## Scaling

### Scale Application

```bash
kubectl scale deployment hrm-app -n hrm-tool --replicas=3
```

### Scale Redis

```bash
kubectl scale deployment redis -n hrm-tool --replicas=2
```

**Note**: MySQL and RabbitMQ are StatefulSets. For production, consider using operators for better scaling support.

## Updating Configuration

### Update ConfigMap

```bash
# Edit configmap
kubectl edit configmap hrm-app-config -n hrm-tool

# Restart pods to pick up changes
kubectl rollout restart deployment hrm-app -n hrm-tool
```

### Update Secrets

```bash
# Update secret
kubectl edit secret hrm-secrets -n hrm-tool

# Restart affected pods
kubectl rollout restart deployment hrm-app -n hrm-tool
```

## Troubleshooting

### Check Pod Logs

```bash
# Application logs
kubectl logs -f deployment/hrm-app -n hrm-tool

# MySQL logs
kubectl logs -f statefulset/mysql -n hrm-tool

# Redis logs
kubectl logs -f deployment/redis -n hrm-tool

# RabbitMQ logs
kubectl logs -f statefulset/rabbitmq -n hrm-tool
```

### Debug Pod Issues

```bash
# Describe pod for events
kubectl describe pod <pod-name> -n hrm-tool

# Execute into pod
kubectl exec -it <pod-name> -n hrm-tool -- /bin/sh
```

### Check Service Endpoints

```bash
# Verify service endpoints
kubectl get endpoints -n hrm-tool

# Check service connectivity
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- curl http://hrm-app-service:80/actuator/health
```

## Persistent Storage

The following services use persistent volumes:

- **MySQL**: 10Gi PVC
- **Redis**: 5Gi PVC
- **RabbitMQ**: 5Gi PVC

Ensure your cluster has a StorageClass configured:

```bash
kubectl get storageclass
```

## Production Considerations

1. **Secrets Management**: Use external secrets management (Vault, AWS Secrets Manager, etc.)
2. **Resource Limits**: Adjust resource requests/limits based on your workload
3. **High Availability**: 
   - Consider MySQL operator for HA MySQL
   - Use Redis cluster mode for HA
   - Configure RabbitMQ clustering
4. **Backup Strategy**: Implement backup solutions for MySQL data
5. **Monitoring**: Integrate with Prometheus and Grafana
6. **Logging**: Set up centralized logging (ELK, Loki, etc.)
7. **Network Policies**: Implement network policies for security
8. **Ingress TLS**: Configure TLS certificates for ingress

## Cleanup

To remove all resources:

```bash
kubectl delete -f k8s/
# Or
kubectl delete namespace hrm-tool
```

**Warning**: This will delete all data. Backup your databases first!

## Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
- [MySQL on Kubernetes](https://dev.mysql.com/doc/mysql-operator/en/)
- [Redis on Kubernetes](https://redis.io/docs/stack/get-started/tutorials/stack-docker/)
