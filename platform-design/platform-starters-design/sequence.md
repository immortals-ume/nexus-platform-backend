```mermaid
sequenceDiagram
participant User
participant Gateway
participant OrderService
participant PaymentService

    User->>Gateway: Place Order
    Gateway->>OrderService: createOrder()
    OrderService->>PaymentService: pay()
    PaymentService-->>OrderService: success
    OrderService-->>Gateway: orderId
    Gateway-->>User: Order Confirmed 
   ```
