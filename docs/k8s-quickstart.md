# Kubernetes Deployment Quickstart

This guide walks you through deploying Spring PetClinic Microservices to a local Kubernetes cluster using Kind.

## Prerequisites

- Docker or Rancher Desktop installed and running
- kubectl CLI installed
- Kind installed (`brew install kind` on macOS)
- Java 17+ and Maven (for building images)

## Step 1: Create Kind Cluster

Create a Kind cluster with Ingress support:

```bash
cat <<EOF | kind create cluster --name petclinic --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
EOF
```

## Step 2: Install NGINX Ingress Controller

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# Wait for the Ingress controller to be ready
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s
```

## Step 3: Build Docker Images

Build all service images using Spring Boot's build-image plugin:

```bash
# Build all services (this takes a few minutes)
./mvnw clean spring-boot:build-image -pl spring-petclinic-customers-service -DskipTests
./mvnw clean spring-boot:build-image -pl spring-petclinic-vets-service -DskipTests
./mvnw clean spring-boot:build-image -pl spring-petclinic-visits-service -DskipTests
./mvnw clean spring-boot:build-image -pl spring-petclinic-api-gateway -DskipTests
```

## Step 4: Load Images into Kind

```bash
kind load docker-image petclinic/customers-service:latest --name petclinic
kind load docker-image petclinic/vets-service:latest --name petclinic
kind load docker-image petclinic/visits-service:latest --name petclinic
kind load docker-image petclinic/api-gateway:latest --name petclinic
```

## Step 5: Deploy to Kubernetes

```bash
# Deploy using dev overlay
kubectl apply -k k8s/overlays/dev

# Wait for all pods to be ready (this may take 2-3 minutes)
kubectl wait --namespace petclinic \
  --for=condition=ready pod \
  --all \
  --timeout=300s
```

## Step 6: Configure /etc/hosts

Add the following entry to your `/etc/hosts` file:

```
127.0.0.1 petclinic.local
```

## Step 7: Access the Application

| Service | URL |
|---------|-----|
| Frontend UI | http://petclinic.local/ |
| Customers API | http://petclinic.local/api/customer/owners |
| Vets API | http://petclinic.local/api/vet/vets |
| Visits API | http://petclinic.local/api/visit/pets/visits?petId=1 |
| Zipkin | http://petclinic.local/zipkin |
| Grafana | http://petclinic.local/grafana (admin/admin) |

For Prometheus, use port-forward:
```bash
kubectl port-forward -n petclinic svc/prometheus 9090:9090
# Then access http://localhost:9090
```

## Useful Commands

### Check Pod Status
```bash
kubectl get pods -n petclinic
```

### View Logs
```bash
kubectl logs -n petclinic deployment/customers-service
kubectl logs -n petclinic deployment/api-gateway
```

### Restart a Service
```bash
kubectl rollout restart deployment/customers-service -n petclinic
```

### Scale a Service
```bash
kubectl scale deployment/customers-service --replicas=2 -n petclinic
```

### Delete Everything
```bash
kubectl delete -k k8s/overlays/dev
# Or delete the entire cluster
kind delete cluster --name petclinic
```

## Troubleshooting

### Pods stuck in Pending
Check if images are loaded:
```bash
docker images | grep petclinic
kind load docker-image <image-name> --name petclinic
```

### Pods in CrashLoopBackOff
Check logs and events:
```bash
kubectl logs -n petclinic <pod-name>
kubectl describe pod -n petclinic <pod-name>
```

### Ingress not working
1. Verify Ingress controller is running:
   ```bash
   kubectl get pods -n ingress-nginx
   ```
2. Check Ingress resource:
   ```bash
   kubectl describe ingress -n petclinic
   ```
3. Verify /etc/hosts entry is correct

### Database connection issues
Check if MySQL is running:
```bash
kubectl get pods -n petclinic | grep mysql
kubectl logs -n petclinic deployment/mysql
```

## Architecture Overview

```
                    ┌─────────────────────┐
                    │   Ingress (nginx)   │
                    │   petclinic.local   │
                    └─────────┬───────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  API Gateway  │   │   Customers   │   │     Vets      │
│    :8080      │   │    :8081      │   │    :8083      │
└───────────────┘   └───────────────┘   └───────────────┘
                              │
                    ┌─────────┴─────────┐
                    │                   │
                    ▼                   ▼
          ┌───────────────┐   ┌───────────────┐
          │    Visits     │   │     MySQL     │
          │    :8082      │   │    :3306      │
          └───────────────┘   └───────────────┘
```

All services connect to:
- MySQL for data persistence
- Zipkin for distributed tracing
- Prometheus scrapes metrics from all services
