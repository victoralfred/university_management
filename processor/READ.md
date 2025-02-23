# Architecture Overview
## Components
1.  Backend (Spring Boot WebFlux):
    * Reactive REST API to manage Terraform configurations.
    * Integrates with PostgreSQL (metadata), S3 (files), DynamoDB (locking), and Terraform execution.
    * Secured with OAuth 2.0 (Keycloak) and RBAC.
2. Frontend (Angular):
   * UI for creating, listing, and executing Terraform templates.
   * Tenant-aware and role-based controls.
3. Storage:
   * **PostgreSQL**: Stores metadata (tenantRequest ID, template name, owner, execution history).
   * **S3**: Stores .tf files with tenantRequest-specific prefixes (e.g., s3://bucket/tenantRequest-id/).
   * **DynamoDB**: Manages Terraform state locking with tenantRequest-specific partition keys.
4. CI/CD:
   * GitHub Actions for linting, testing, and deploying backend, frontend, and Terraform.
5. Monitoring:
   * Prometheus/Grafana for metrics, audit logs in PostgreSQL, alerts via Slack.
6. Deployment
   * Backend: Docker + Kubernetes on AWS EKS.
   * Frontend: AWS S3 + CloudFront.
   * Infrastructure: Terraform with GitOps (ArgoCD).