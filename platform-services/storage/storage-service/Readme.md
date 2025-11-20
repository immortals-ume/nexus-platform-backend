# Attachment Service

## 1. Goals & Requirements

### Functional

* Accept uploads (multipart/form-data / direct binary / presigned uploads).
* Store attachments (images, PDFs, docs, videos) and their metadata.
* Retrieve attachments via secure URLs (presigned or CDN-backed).
* Support versioning, soft-deletes, retention policies.
* Metadata: owner, mime-type, size, checksum, tags, custom attributes.
* Fine-grained access control (resource-level ACLs + roles).
* Audit logging for uploads/downloads/deletes/changes.
* Virus/malware scanning and file validation.
* Quotas, rate-limiting, and throttling per tenant/user.

### Non-functional

* Highly available, fault-tolerant, regionally distributable across clouds (AWS/GCP/Azure).
* Scalable: handle bursts and large files (multipart/resumable uploads).
* Secure: encryption at rest/in transit, strong authz/authn.
* Cost-efficient: lifecycle policies, cold storage tiering.
* Observability: tracing, metrics, alerts, logs.

### Constraints

* Implemented in Java + Spring Boot.
* Cloud-extensible: pluggable storage backends (S3/GCS/Azure/On-prem).
* Standalone microservice architecture.

---

## 2. High-Level Architecture (HLD)

### Core Components

* **API Gateway / Edge**: rate-limiting, TLS termination, routing.
* **Auth & IAM**: integrates with OAuth2 / OIDC (Keycloak, Cognito, Auth0).
* **Attachment Service (Spring Boot)**: REST/GRPC endpoints, business logic.
* **Storage Adapter Layer**: pluggable implementations for S3/GCS/Azure/Filesystem.
* **Metadata DB**: relational DB (Postgres) for metadata, ACLs, versions.
* **Blob Storage**: object stores (S3/GCS/Azure Blob) for file content.
* **CDN**: optional CDN (CloudFront/Cloud CDN/Azure CDN) for public/fast retrieval.
* **Async Workers**: background jobs — virus scan, thumbnailing, indexing.
* **Message Bus**: Kafka/RabbitMQ for events (upload-complete, scan-complete).
* **Audit & Observability**: ELK/EFK, Prometheus + Grafana, distributed tracing (Jaeger).
* **Admin UI / Dashboard** (optional): for monitoring, manual scans, policy changes.

### Multi-Cloud / Extensibility Strategy

* Implement a **Storage Adapter** interface with implementations for each provider.
* Use feature flags or configuration to select provider per deployment or per-tenant.
* Abstract presigned URL generation behind adapter.
* Use Terraform/Helm charts for each cloud deployment; leverage a common orchestration pattern (Kubernetes + CSI for on-prem).

### Data Flow (Upload Example)

1. Client authenticates (OAuth2) and requests upload.
2. Client requests either direct upload or presigned URL.
3. If presigned: Attachment Service generates presigned URL via Storage Adapter and returns it.
4. Client uploads directly to Blob store (offloads service). If direct upload: client posts to service which streams to blob store.
5. Storage sends an event (or the service emits one) `UploadComplete` to Message Bus.
6. Async worker picks up event: runs virus scan, generates thumbnails, extracts metadata, computes checksum.
7. Worker updates Metadata DB and emits `Ready` or `Quarantine` event.
8. Client requests retrieval; service validates authorization then returns secure URL (short-lived) or proxies via CDN.

---

## 3. API Contract (REST)

### Authentication

* OAuth2 bearer tokens (JWT).

### Endpoints (summary)

* `POST /attachments` — start upload (returns attachment-id + presigned URLs or upload token)
* `PUT /attachments/{id}/commit` — commit multipart upload
* `GET /attachments/{id}` — get metadata
* `GET /attachments/{id}/download` — get presigned download URL or redirect
* `DELETE /attachments/{id}` — soft delete
* `POST /attachments/{id}/restore` — restore soft-deleted version
* `GET /attachments/{id}/versions` — list versions
* `POST /attachments/{id}/acl` — set ACL
* `GET /attachments?owner=...&tag=...` — query attachments

Responses include standard HTTP codes + structured error body: `code`, `message`, `details`.

---

## 4. Data Model 

```yaml
   Attachment
   - id: UUID
   - ownerId: UUID
   - filename: string
   - contentType: string
   - size: long
   - checksum: sha256
   - storageKey: string
   - storageProvider: enum(AWS_S3, GCP_GCS, AZURE_BLOB, LOCAL)
   - version: int
   - status: enum(UPLOADING, READY, QUARANTINED, DELETED)
   - createdAt, updatedAt
   - metadata: jsonb (custom attrs)
   - acl: jsonb or normalized table


     AttachmentVersion
   - id
   - attachmentId
   - versionNumber
   - storageKey
   - createdAt


     AuditLog
   - id, userId, action, attachmentId, timestamp, details
```

Use Postgres (jsonb) for flexible metadata and ACL storage. Store large text or complex metadata sparingly.

---

## 5. Low-Level Design (LLD)

### Package Structure (conceptual)

* `com.immortals.attachments.api` — controllers, DTOs
* `com.immortals.attachments.service` — core business services
* `com.immortals.attachments.storage` — StorageAdapter interface + implementations
* `com.immortals.attachments.worker` — async worker handlers
* `com.immortals.attachments.auth` — auth utilities, permission checks
* `com.immortals.attachments.db` — repositories, entities
* `com.immortals.attachments.audit` — audit logging
* `com.immortals.attachments.metrics` — Prometheus metrics

### Key Interfaces

```java
interface StorageAdapter {
  PresignedUpload createPresignedUpload(String key, long size, String contentType);
  PresignedDownload createPresignedDownload(String key, Duration ttl);
  void streamUpload(String key, InputStream data);
  void delete(String key);
  StorageInfo getInfo(String key);
}
```

Adapters: `S3Adapter`, `GcsAdapter`, `AzureBlobAdapter`, `LocalFsAdapter`.

### Services

* `AttachmentService` — orchestrates uploads, commits, metadata updates, and access checks.
* `VersioningService` — manages version creation and lookup.
* `ACLService` — evaluate permissions (owner, role, explicit ACL).
* `ScanService` — submits files to virus scanner (ClamAV or cloud-managed).
* `EventPublisher` — publishes to message bus.

### Async Workers

* `ScanWorker` — waits for `UploadComplete`, scans file, updates status.
* `ThumbnailWorker` — generates thumbnails (images/PDFs) and stores derivatives.
* `RetentionWorker` — enforces retention & lifecycle policies.

### Database Transactions

* Upload initiation: create DB row with status UPLOADING.
* Commit: finalize storage key, increment version in a serializable/optimistic manner.
* Use advisory locks or optimistic version checking for concurrent updates to same attachment.

---

## 6. Security

* **AuthN**: OAuth2/OIDC (JWT). Validate scopes and token audience.
* **AuthZ**: RBAC + resource ACLs; support tenant-scoped permissions.
* **Encryption**: server-side encryption (SSE) for object stores; additionally encrypt sensitive metadata in DB if needed.
* **Transport**: TLS for all endpoints and presigned URLs should be HTTPS.
* **Input Validation**: enforce accepted mime-types and size limits; scan for malicious content.
* **Presigned URL TTL**: short-lived (minutes) for downloads; rotate keys as needed.
* **Audit**: immutable audit logs (WORM / append-only) for critical operations.
* **Key Management**: integrate with KMS (AWS KMS / GCP KMS / Azure Key Vault) for encryption keys.

---

## 7. Scaling & Availability

* **Stateless app**: multiple Spring Boot instances behind LB (Kubernetes Deployments).
* **Storage scaling**: use cloud object stores; scale horizontally.
* **DB**: Postgres with read replicas for read-heavy loads; partitioning if needed.
* **Workers**: scale consumers independently.
* **Large files**: offload upload traffic with presigned URLs; support multipart/resumable uploads for reliability.
* **Regional deployment**: deploy service and storage in multiple regions; replicate metadata with async events or use a central DB with geo-replication as needed.

---

## 8. Reliability & Failure Modes

* **Upload failure**: keep partial uploads with TTL cleanup job.
* **Virus scan false positive**: quarantine and provide manual review UI & override with audit trail.
* **Storage provider outage**: fallback to alternate provider if configured (cross-cloud replication or failover adapters).
* **DB outage**: use replicas and failover. Graceful degradation—allow reads from cache.

---

## 9. Observability

* Metrics: request rates, latencies, upload sizes, scan times, error rates, storage costs.
* Tracing: instrument key flows (upload -> scan -> ready).
* Logs: structured logs (correlationId, userId, attachmentId).
* Alerts: high error rate, scan backlog, storage cost spike.

---

## 10. Operational Concerns

* **Backup & Restore**: backup metadata DB; blob stores have versioning + replication.
* **Cost Management**: lifecycle policies to move to infrequent/archival tiers.
* **Data Residency**: per-tenant region selection; ensure compliance with regulations.
* **Migration Strategy**: when switching providers, copy blobs and update metadata to new storageKey.

---

## 11. Testing Strategy

* Unit tests for services & adapters.
* Integration tests with an S3-compatible in-memory store (LocalStack, MinIO).
* Contract tests for StorageAdapter interface.
* E2E tests for upload-to-download path including scan workers.
* Chaos tests for component failures (storage, DB, message bus).

---


