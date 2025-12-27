package com.immortals.platform.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for PlatformException and its subclasses.
 */
class PlatformExceptionTest {

    @Test
    void shouldCreateBusinessExceptionWithMessage() {
        String message = "Business rule violated";
        BusinessException exception = new BusinessException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateBusinessExceptionWithMessageAndCause() {
        String message = "Business rule violated";
        Throwable cause = new RuntimeException("Root cause");
        BusinessException exception = new BusinessException(message, cause);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldCreateTechnicalExceptionWithMessage() {
        String message = "Technical error occurred";
        TechnicalException exception = new TechnicalException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateTechnicalExceptionWithMessageAndCause() {
        String message = "Technical error occurred";
        Throwable cause = new RuntimeException("Root cause");
        TechnicalException exception = new TechnicalException(message, cause);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldCreateValidationExceptionWithMessage() {
        String message = "Validation failed";
        ValidationException exception = new ValidationException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateResourceNotFoundExceptionWithMessage() {
        String message = "Resource not found";
        ResourceNotFoundException exception = new ResourceNotFoundException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateAuthenticationExceptionWithMessage() {
        String message = "Authentication failed";
        AuthenticationException exception = new AuthenticationException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateSecurityExceptionWithMessage() {
        String message = "Security violation";
        SecurityException exception = new SecurityException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateDatabaseExceptionWithMessage() {
        String message = "Database error";
        DatabaseException exception = new DatabaseException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateCacheExceptionWithMessage() {
        String message = "Cache error";
        CacheException exception = new CacheException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateCacheConfigurationExceptionWithMessage() {
        String message = "Cache configuration error";
        CacheConfigurationException exception = new CacheConfigurationException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateCacheConnectionExceptionWithMessage() {
        String message = "Cache connection error";
        CacheConnectionException exception = new CacheConnectionException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateCacheSerializationExceptionWithMessage() {
        String message = "Cache serialization error";
        CacheSerializationException exception = new CacheSerializationException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateRateLimitExceptionWithMessage() {
        String message = "Rate limit exceeded";
        String userId = "user123";
        String channel = "email";
        long retryAfter = 60L;
        RateLimitException exception = new RateLimitException(message, userId, channel, retryAfter);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getUserId()).isEqualTo(userId);
        assertThat(exception.getChannel()).isEqualTo(channel);
        assertThat(exception.getRetryAfterSeconds()).isEqualTo(retryAfter);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateDuplicateResourceExceptionWithMessage() {
        String message = "Duplicate resource";
        DuplicateResourceException exception = new DuplicateResourceException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateInvalidOperationExceptionWithMessage() {
        String message = "Invalid operation";
        InvalidOperationException exception = new InvalidOperationException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateBusinessRuleViolationExceptionWithMessage() {
        String message = "Business rule violation";
        BusinessRuleViolationException exception = new BusinessRuleViolationException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateUserExceptionWithMessage() {
        String message = "User error";
        UserException exception = new UserException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateProviderExceptionWithMessage() {
        String message = "Provider error";
        String providerId = "email-provider";
        ProviderException exception = new ProviderException(message, providerId);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getProviderId()).isEqualTo(providerId);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateNotificationExceptionWithMessage() {
        String message = "Notification error";
        NotificationException exception = new NotificationException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateTemplateExceptionWithMessage() {
        String message = "Template error";
        String templateCode = "welcome-email";
        TemplateException exception = new TemplateException(message, templateCode);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getTemplateCode()).isEqualTo(templateCode);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateTemplateRenderingExceptionWithMessage() {
        String message = "Template rendering error";
        TemplateRenderingException exception = new TemplateRenderingException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldMaintainInheritanceHierarchy() {
        BusinessException businessException = new BusinessException("test");
        assertThat(businessException).isInstanceOf(PlatformException.class);
        assertThat(businessException).isInstanceOf(RuntimeException.class);

        TechnicalException technicalException = new TechnicalException("test");
        assertThat(technicalException).isInstanceOf(PlatformException.class);
        assertThat(technicalException).isInstanceOf(RuntimeException.class);

        ValidationException validationException = new ValidationException("test");
        assertThat(validationException).isInstanceOf(BusinessException.class);
        assertThat(validationException).isInstanceOf(PlatformException.class);

        CacheException cacheException = new CacheException("test");
        assertThat(cacheException).isInstanceOf(RuntimeException.class);
    }
}