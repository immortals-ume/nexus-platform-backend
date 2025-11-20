# Notification Service

Notification Service is a microservice designed to handle and manage various types of notifications, such as SMS, email, and WhatsApp messages. It provides an easy-to-use API for sending notifications with support for tracking, customizable content, and expiration times. The service integrates with communication platforms like Twilio to deliver messages.

## Key Features

- **Multi-Channel Notifications**: Send notifications via SMS, email, and WhatsApp.
- **Customizable Content**: Supports customizable content for different types of notifications.
- **Status Tracking**: Tracks the delivery status of notifications.
- **Expiration Handling**: Configurable notification expiration for time-sensitive messages.
- **Java Spring Boot**: Built using the Spring Boot framework.
- **Database Persistence**: Integrates with a relational database to store and manage notifications.

## Technologies Used

- **Java 17**
- **Spring Boot 3.x**
- **Hibernate/JPA** for database interactions
- **Gupshup** ,**Twillio** for SMS and WhatsApp integration
- **PostgreSQL/Redis** for persistence
- **Maven** for dependency management

## Requirements

- Java 17 or higher
- Maven 3.6+
- Twilio account with phone numbers for SMS/WhatsApp
- A relational database (H2, PostgreSQL, or MySQL)

## Setup Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/notification-service.git
   cd notification-service
   ```