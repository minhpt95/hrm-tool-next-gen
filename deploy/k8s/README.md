# Kubernetes deployment

Helm chart for the HRM Tool Spring Boot app. Postgres, Redis, and RabbitMQ are
expected to exist outside this chart — point at managed services or existing
in-cluster releases via `values.yaml`.

## Layout

```
deploy/k8s/
└── hrm-app/        # Helm chart
    ├── Chart.yaml
    ├── values.yaml
    └── templates/
```

## Prerequisites

- Kubernetes 1.25+
- Helm 3.10+
- Image `hrm-app:<tag>` pushed to a registry your cluster can pull from
- Postgres, Redis, RabbitMQ reachable from the cluster

## Install

```bash
helm install hrm deploy/k8s/hrm-app \
  --namespace hrm --create-namespace \
  --set image.repository=<registry>/hrm-app \
  --set image.tag=0.0.1-SNAPSHOT \
  --set secret.data.JWT_SECRET="$(openssl rand -base64 48)" \
  --set config.DB_HOST=<postgres-host> \
  --set secret.data.DB_PASSWORD=<password>
```

`JWT_SECRET` is required — install fails fast if it is empty.

## Production: externalize the Secret

Avoid putting secrets on the CLI or in values files committed to git. Create a
Secret out-of-band (External Secrets Operator, Vault, Sealed Secrets, etc.) and
point the chart at it:

```bash
helm install hrm deploy/k8s/hrm-app \
  --namespace hrm \
  --set existingSecret=hrm-app-secrets \
  -f my-values.yaml
```

The Secret must contain the same keys as `secret.data` in `values.yaml`
(`JWT_SECRET`, `DB_PASSWORD`, `RABBITMQ_PASSWORD`, etc.).

## Upgrade

```bash
helm upgrade hrm deploy/k8s/hrm-app -n hrm -f my-values.yaml
```

## Uninstall

```bash
helm uninstall hrm -n hrm
```

## Notes

- App listens on `9000` (matches `SERVER_PORT` in `application-docker.yaml`).
  The Dockerfile's `EXPOSE 8080` is stale and unused — `containerPort` is driven
  by `service.targetPort` in values.
- Liveness/readiness hit `/actuator/health/liveness` and `/actuator/health/readiness`.
  Confirm Spring Boot Actuator's health groups are enabled (default since 2.3+).
- Logs default to an `emptyDir`. Enable `logs.persistence.enabled=true` only if
  you need on-disk retention; prefer stdout + a cluster log shipper.
