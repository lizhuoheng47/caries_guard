# Kubernetes application tier

These manifests scale the three stateless application services. They intentionally reference external clustered MySQL, Redis, RabbitMQ, and MinIO services and two pre-provisioned read-only model PVCs.

Before deployment:

1. Replace the three image names through `kustomization.yaml`.
2. Adjust the infrastructure DNS names in `cariesguard-config`.
3. Create `cariesguard-secrets` with the same secret environment variables documented by `.env.docker.example`.
4. Provision `cariesguard-model-weights` and `cariesguard-disease-model` PVCs.
5. Install Metrics Server so the HPAs can act.
6. Configure an Ingress with TLS for the `frontend` service.

Validate without applying:

```powershell
kubectl kustomize .\deploy\k8s
kubectl apply --dry-run=server -k .\deploy\k8s
```

Do not describe this template as end-to-end high availability until the external stateful services, backups, monitoring, and fault drills have been verified in the target cluster.
