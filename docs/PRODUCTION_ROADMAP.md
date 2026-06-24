# Production roadmap

Roadmap này chia quá trình nâng cấp hệ thống thành các phase nhỏ, có thể review,
kiểm thử và rollback độc lập. Mục tiêu ban đầu là production foundation cho tải
không quá 100 RPS, giữ tương thích API `/api/v1`.

## Nguyên tắc

- Mỗi phase đi qua một pull request riêng và không tự động merge.
- Không thay đổi API, schema và hạ tầng trong cùng một PR nếu không bắt buộc.
- Mọi thay đổi nghiệp vụ phải có unit test; thay đổi tích hợp phải có integration
  hoặc contract test tương ứng.
- Database thuộc sở hữu của từng service; chỉ chia sẻ event contract, security
  constants và observability configuration, không chia sẻ domain entity.
- Secret không được commit vào repository.

## Phase 1 - Baseline, tests và Compose wiring

- Sửa URL Basket/Inventory cho Ordering khi chạy full Docker Compose.
- Thêm unit test cho Basket và các nhánh chính của Order Saga.
- Chạy toàn Maven reactor trong GitHub Actions.
- Không thay đổi API, DTO, database hoặc hành vi nghiệp vụ.

## Phase 2 - Runtime và database foundation

- Nâng Java 21, Spring Boot 3.5.x và Spring Cloud 2025.0.x theo release tương
  thích đang được hỗ trợ.
- Thay `ddl-auto: update` bằng Flyway và production schema validation.
- Chuẩn hóa audit fields, error codes, trace ID và DTO mapping.
- Tách secret khỏi Compose, thêm healthcheck cho mọi service và profile chạy local.

## Phase 3 - Identity và authorization

- Dùng Keycloak OIDC; client mới đăng nhập bằng Authorization Code + PKCE.
- Gateway và từng service xác minh issuer, audience, expiration, role và scope.
- Áp dụng role `CUSTOMER`, `ADMIN` và service accounts cho lời gọi nội bộ.
- Gắn tài nguyên Customer, Basket và Order với JWT subject để chặn truy cập chéo.
- Giữ login v1 trong thời gian chuyển tiếp và đánh dấu deprecated.

## Phase 4 - Product, Basket và Inventory correctness

- Product là nguồn catalog/giá; Inventory là nguồn tồn kho duy nhất.
- Basket lấy giá hiện hành từ Product, có TTL và optimistic version.
- Inventory reserve/release bằng atomic update và reservation ID idempotent.
- Thêm test Redis/MongoDB bằng Testcontainers và contract test giữa các service.

## Phase 5 - Reliable checkout và messaging

- Order dùng state machine `PENDING`, `INVENTORY_RESERVED`, `COMPLETED`, `FAILED`.
- Hỗ trợ `Idempotency-Key`, order item snapshot và recovery cho saga bị gián đoạn.
- Ghi Order và Outbox trong cùng transaction.
- RabbitMQ dùng topic exchange, event envelope versioned, publisher confirms,
  retry và dead-letter queue.
- Consumer lưu event ID để xử lý message idempotent.

## Phase 6 - Background jobs và observability

- Background service theo dõi job execution, retry cleanup/recovery và DLQ replay.
- Thêm distributed lock cho scheduled jobs chạy nhiều replica.
- Tích hợp Elastic APM, ECS structured logs và correlation ID xuyên REST/RabbitMQ.
- Dashboard và alert cho latency, checkout failure, outbox lag, DLQ và job failure.

## Phase 7 - Kubernetes và delivery

- Helm chart dùng managed datastores và External Secrets Operator.
- Thêm probes, resource limits, HPA, PodDisruptionBudget và NetworkPolicy.
- GitHub Actions build SBOM, scan image, publish GHCR và deploy staging.
- Production dùng manual approval, rolling update và automatic rollback khi smoke
  test thất bại.

## Sau production foundation

Capability nghiệp vụ tiếp theo là Payment workflow với payment intent, webhook
idempotent và mở rộng Order state machine. Shipping, application email provider,
CQRS read model và microfrontend chưa nằm trong các phase foundation.
