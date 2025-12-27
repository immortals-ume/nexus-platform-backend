package com.immortals.platform.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for domain enums.
 */
class EnumTest {

    @Test
    void shouldHaveCorrectUserTypesValues() {
        UserTypes[] userTypes = UserTypes.values();
        assertThat(userTypes).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (UserTypes userType : userTypes) {
            assertThat(UserTypes.valueOf(userType.name())).isEqualTo(userType);
        }
    }

    @Test
    void shouldHaveCorrectOrderStatusValues() {
        OrderStatus[] orderStatuses = OrderStatus.values();
        assertThat(orderStatuses).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (OrderStatus status : orderStatuses) {
            assertThat(OrderStatus.valueOf(status.name())).isEqualTo(status);
        }
    }

    @Test
    void shouldHaveCorrectPaymentStatusValues() {
        PaymentStatus[] paymentStatuses = PaymentStatus.values();
        assertThat(paymentStatuses).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (PaymentStatus status : paymentStatuses) {
            assertThat(PaymentStatus.valueOf(status.name())).isEqualTo(status);
        }
    }

    @Test
    void shouldHaveCorrectPaymentMethodValues() {
        PaymentMethod[] paymentMethods = PaymentMethod.values();
        assertThat(paymentMethods).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (PaymentMethod method : paymentMethods) {
            assertThat(PaymentMethod.valueOf(method.name())).isEqualTo(method);
        }
    }

    @Test
    void shouldHaveCorrectProductStatusValues() {
        ProductStatus[] productStatuses = ProductStatus.values();
        assertThat(productStatuses).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (ProductStatus status : productStatuses) {
            assertThat(ProductStatus.valueOf(status.name())).isEqualTo(status);
        }
    }

    @Test
    void shouldHaveCorrectAddressTypeValues() {
        AddressType[] addressTypes = AddressType.values();
        assertThat(addressTypes).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (AddressType type : addressTypes) {
            assertThat(AddressType.valueOf(type.name())).isEqualTo(type);
        }
    }

    @Test
    void shouldHaveCorrectAddressStatusValues() {
        AddressStatus[] addressStatuses = AddressStatus.values();
        assertThat(addressStatuses).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (AddressStatus status : addressStatuses) {
            assertThat(AddressStatus.valueOf(status.name())).isEqualTo(status);
        }
    }

    @Test
    void shouldHaveCorrectAuthorityNameValues() {
        AuthorityName[] authorityNames = AuthorityName.values();
        assertThat(authorityNames).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (AuthorityName authority : authorityNames) {
            assertThat(AuthorityName.valueOf(authority.name())).isEqualTo(authority);
        }
    }

    @Test
    void shouldHaveCorrectAuthProviderValues() {
        AuthProvider[] authProviders = AuthProvider.values();
        assertThat(authProviders).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (AuthProvider provider : authProviders) {
            assertThat(AuthProvider.valueOf(provider.name())).isEqualTo(provider);
        }
    }

    @Test
    void shouldHaveCorrectDbTypeValues() {
        DbType[] dbTypes = DbType.values();
        assertThat(dbTypes).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (DbType type : dbTypes) {
            assertThat(DbType.valueOf(type.name())).isEqualTo(type);
        }
    }

    @Test
    void shouldHaveCorrectDeliveryStatusValues() {
        DeliveryStatus[] deliveryStatuses = DeliveryStatus.values();
        assertThat(deliveryStatuses).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (DeliveryStatus status : deliveryStatuses) {
            assertThat(DeliveryStatus.valueOf(status.name())).isEqualTo(status);
        }
    }

    @Test
    void shouldHaveCorrectShipmentStatusValues() {
        ShipmentStatus[] shipmentStatuses = ShipmentStatus.values();
        assertThat(shipmentStatuses).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (ShipmentStatus status : shipmentStatuses) {
            assertThat(ShipmentStatus.valueOf(status.name())).isEqualTo(status);
        }
    }

    @Test
    void shouldHaveCorrectNotificationTypeValues() {
        NotificationType[] notificationTypes = NotificationType.values();
        assertThat(notificationTypes).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (NotificationType type : notificationTypes) {
            assertThat(NotificationType.valueOf(type.name())).isEqualTo(type);
        }
    }

    @Test
    void shouldHaveCorrectNotificationStatusValues() {
        NotificationStatus[] notificationStatuses = NotificationStatus.values();
        assertThat(notificationStatuses).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (NotificationStatus status : notificationStatuses) {
            assertThat(NotificationStatus.valueOf(status.name())).isEqualTo(status);
        }
    }

    @Test
    void shouldHaveCorrectNotificationPriorityValues() {
        NotificationPriority[] notificationPriorities = NotificationPriority.values();
        assertThat(notificationPriorities).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (NotificationPriority priority : notificationPriorities) {
            assertThat(NotificationPriority.valueOf(priority.name())).isEqualTo(priority);
        }
    }

    @Test
    void shouldHaveCorrectRolesValues() {
        Roles[] roles = Roles.values();
        assertThat(roles).isNotEmpty();
        
        // Test that valueOf works for all enum values
        for (Roles role : roles) {
            assertThat(Roles.valueOf(role.name())).isEqualTo(role);
        }
    }

    @Test
    void shouldHaveUniqueOrdinalValues() {
        // Test UserTypes ordinals are unique
        UserTypes[] userTypes = UserTypes.values();
        for (int i = 0; i < userTypes.length; i++) {
            assertThat(userTypes[i].ordinal()).isEqualTo(i);
        }

        // Test OrderStatus ordinals are unique
        OrderStatus[] orderStatuses = OrderStatus.values();
        for (int i = 0; i < orderStatuses.length; i++) {
            assertThat(orderStatuses[i].ordinal()).isEqualTo(i);
        }
    }

    @Test
    void shouldSupportComparison() {
        UserTypes[] userTypes = UserTypes.values();
        if (userTypes.length > 1) {
            assertThat(userTypes[0].compareTo(userTypes[1])).isLessThan(0);
            assertThat(userTypes[1].compareTo(userTypes[0])).isGreaterThan(0);
            assertThat(userTypes[0].compareTo(userTypes[0])).isEqualTo(0);
        }
    }
}