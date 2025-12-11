# Quickstart: K8s 部署指南

**Branch**: `002-k8s-migration` | **Date**: 2025-12-10

## Prerequisites

- Kubernetes cluster (minikube, kind, Docker Desktop K8s, or cloud provider)
- kubectl CLI installed
- NGINX Ingress Controller installed

### Install NGINX Ingress Controller

```bash
# For minikube
minikube addons enable ingress

# For kind
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# For other clusters
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.9.4/deploy/static/provider/cloud/deploy.yaml
```

---

## Quick Deploy

### 1. Build Docker Images (if needed)

```bash
# Build all images
./mvnw clean package -DskipTests -PbuildDocker
```

### 2. Deploy to Kubernetes

```bash
# Create namespace
kubectl create namespace petclinic

# Deploy all services (dev environment)
kubectl apply -k k8s/overlays/dev

# Or deploy to production
kubectl apply -k k8s/overlays/prod
```

### 3. Verify Deployment

```bash
# Check all pods are running
kubectl get pods -n petclinic

# Expected output:
# NAME                                 READY   STATUS    RESTARTS   AGE
# customers-service-xxx                1/1     Running   0          2m
# vets-service-xxx                     1/1     Running   0          2m
# visits-service-xxx                   1/1     Running   0          2m
# api-gateway-xxx                      1/1     Running   0          2m
# genai-service-xxx                    1/1     Running   0          2m
# tracing-server-xxx                   1/1     Running   0          2m
# prometheus-xxx                       1/1     Running   0          2m
# grafana-xxx                          1/1     Running   0          2m

# Check services
kubectl get svc -n petclinic

# Check ingress
kubectl get ingress -n petclinic
```

### 4. Configure Local Access

```bash
# Add to /etc/hosts (Linux/Mac) or C:\Windows\System32\drivers\etc\hosts (Windows)
echo "127.0.0.1 petclinic.local" | sudo tee -a /etc/hosts

# For minikube, get the IP
minikube ip
# Then add: <minikube-ip> petclinic.local
```

### 5. Access the Application

| Service | URL |
|---------|-----|
| Frontend UI | http://petclinic.local |
| Zipkin (Tracing) | http://petclinic.local/zipkin |
| Grafana (Monitoring) | http://petclinic.local/grafana |
| Prometheus | kubectl port-forward svc/prometheus 9090:9090 -n petclinic |

---

## Development Workflow

### Port Forwarding (for debugging)

```bash
# Access individual services directly
kubectl port-forward svc/customers-service 8081:8081 -n petclinic
kubectl port-forward svc/vets-service 8083:8083 -n petclinic
kubectl port-forward svc/visits-service 8082:8082 -n petclinic
kubectl port-forward svc/api-gateway 8080:8080 -n petclinic
```

### View Logs

```bash
# View logs for a specific service
kubectl logs -f deployment/customers-service -n petclinic

# View logs for all services
kubectl logs -f -l app=customers-service -n petclinic
```

### Update Configuration

```bash
# Edit ConfigMap
kubectl edit configmap petclinic-config -n petclinic

# Restart deployments to pick up changes
kubectl rollout restart deployment -n petclinic
```

### Scale Services

```bash
# Scale a specific service
kubectl scale deployment customers-service --replicas=3 -n petclinic

# Check horizontal pod autoscaler (if configured)
kubectl get hpa -n petclinic
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod events
kubectl describe pod <pod-name> -n petclinic

# Check logs
kubectl logs <pod-name> -n petclinic --previous
```

### Service Not Accessible

```bash
# Check service endpoints
kubectl get endpoints -n petclinic

# Test service connectivity from another pod
kubectl run curl --image=curlimages/curl -it --rm -- curl http://customers-service:8081/actuator/health
```

### Ingress Not Working

```bash
# Check ingress status
kubectl describe ingress petclinic-ingress -n petclinic

# Check ingress controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx
```

---

## Clean Up

```bash
# Delete all resources
kubectl delete -k k8s/overlays/dev

# Or delete namespace (removes everything)
kubectl delete namespace petclinic
```

---

## Environment-Specific Deployment

| Environment | Command | Replicas | Memory |
|-------------|---------|----------|--------|
| Development | `kubectl apply -k k8s/overlays/dev` | 1 | 256Mi |
| Staging | `kubectl apply -k k8s/overlays/staging` | 2 | 512Mi |
| Production | `kubectl apply -k k8s/overlays/prod` | 3 | 1Gi |

---

**Version**: 1.0 | **Status**: Complete
