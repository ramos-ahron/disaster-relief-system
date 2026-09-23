# Deployment

The pipeline in `.github/workflows/ci.yml` tests every push and pull request. A push to `main` also builds the image, pushes it to Azure Container Registry and deploys the API to Azure Kubernetes Service. Create the items below before the first push.

## Azure resources

1. A resource group.
2. An Azure Container Registry (Basic is enough).
3. An AKS cluster created with Microsoft Entra integration and Azure RBAC for Kubernetes authorization, with the registry attached so nodes can pull from it.
4. A PostgreSQL database. Either use the StatefulSet in `k8s/postgres.yaml`, or an Azure Database for PostgreSQL flexible server reachable from the cluster. For the second option skip `postgres.yaml` and set `DB_URL` in `k8s/configmap.yaml` and `PGHOST` in `k8s/migrate-job.yaml` to the server.
5. A Microsoft Entra app registration with a federated credential for this GitHub repository (branch `main`), so the workflow signs in without a stored password.

Role assignments for the app registration's service principal:

| Role | Scope |
|---|---|
| AcrPush | the container registry |
| Azure Kubernetes Service Cluster User Role | the cluster |
| Azure Kubernetes Service RBAC Writer | the namespace used for deployment |

## Values to replace

In `.github/workflows/ci.yml` (`env:` block):

| Placeholder | Meaning |
|---|---|
| `<ACR_NAME>` | registry name without `.azurecr.io` |
| `<AKS_RESOURCE_GROUP>` | resource group of the cluster |
| `<AKS_CLUSTER_NAME>` | cluster name |
| `<K8S_NAMESPACE>` | namespace to deploy into |

`k8s/deployment.yaml` also contains `<ACR_NAME>` and `<IMAGE_TAG>`. The workflow fills both while deploying; only change `<ACR_NAME>` by hand if you apply the file yourself.

## GitHub secrets

Repository secrets, from the Entra app registration:

- `AZURE_CLIENT_ID`
- `AZURE_TENANT_ID`
- `AZURE_SUBSCRIPTION_ID`

No registry password is needed: the workflow logs in to Azure and then to the registry with `az acr login`.

## One-time cluster setup

Run these once, from a machine with `kubectl` access to the cluster. Replace the placeholders. The secret never goes into the repository.

```
kubectl create namespace <K8S_NAMESPACE>
kubectl -n <K8S_NAMESPACE> create secret generic db-credentials \
  --from-literal=username=<DB_USER> --from-literal=password=<DB_PASSWORD>
kubectl -n <K8S_NAMESPACE> apply -f k8s/postgres.yaml     # only for the in-cluster database
```

`k8s/secret.template.yaml` shows the shape of the secret.

## What a deployment does

1. `mvn verify` runs all tests, including the database tests against a PostgreSQL service container on the runner.
2. The image is built and pushed as `<ACR_NAME>.azurecr.io/disaster-relief-api:<commit sha>`.
3. The files in `database/migrations` are loaded into the ConfigMap `db-migrations`, and the Job `db-migrate` applies them. They only add tables, so the old Deployment keeps working against the new schema while pods roll over.
4. The ConfigMap and Service are applied, then the Deployment with the new image. The rollout waits for the `/ready` probe.

## Notes

- The Service is ClusterIP. The API has no authentication, so put an authenticating ingress in front before exposing it.
- The sample data in `database/sample` is for local use. It is not applied by the pipeline.
- The StatefulSet runs one PostgreSQL pod with a 5 Gi volume and no backups. Use a managed server where data matters.
