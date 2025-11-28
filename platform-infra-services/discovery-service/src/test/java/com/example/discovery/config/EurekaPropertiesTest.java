package com.example.discovery.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.jqwik.api.*;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: infra-services-production-ready, Property 24: Required property validation
 * Validates: Requirements 9.3
 * 
 * Tests that EurekaProperties validates required properties at startup
 * and fails with clear error messages when required properties are missing.
 */
class EurekaPropertiesTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validPropertiesShouldPassValidation() {
        EurekaProperties properties = new EurekaProperties();
        properties.setInstance(createValidInstance());
        properties.setServer(createValidServer());
        properties.setClient(createValidClient());

        Set<ConstraintViolation<EurekaProperties>> violations = validator.validate(properties);

        assertThat(violations).isEmpty();
    }

    /**
     * Property: Instance hostname validation
     * For any EurekaProperties with blank hostname,
     * validation should fail
     */
    @Property(tries = 100)
    @Label("Eureka hostname is required and cannot be blank")
    void hostnameCannotBeBlank(@ForAll @From("blankStrings") String blankHostname) {
        EurekaProperties properties = new EurekaProperties();
        
        EurekaProperties.Instance instance = createValidInstance();
        instance.setHostname(blankHostname);
        properties.setInstance(instance);
        
        properties.setServer(createValidServer());
        properties.setClient(createValidClient());

        Set<ConstraintViolation<EurekaProperties>> violations = validator.validate(properties);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("hostname"));
    }

    /**
     * Property: Lease renewal interval validation
     * For any EurekaProperties with invalid lease renewal interval,
     * validation should fail
     */
    @Property(tries = 100)
    @Label("Lease renewal interval must be between 1 and 300 seconds")
    void leaseRenewalIntervalMustBeInValidRange(
            @ForAll @IntRange(min = -100, max = 0) int tooLow,
            @ForAll @IntRange(min = 301, max = 1000) int tooHigh) {
        
        EurekaProperties properties1 = new EurekaProperties();
        EurekaProperties.Instance instance1 = createValidInstance();
        instance1.setLeaseRenewalIntervalInSeconds(tooLow);
        properties1.setInstance(instance1);
        properties1.setServer(createValidServer());
        properties1.setClient(createValidClient());

        Set<ConstraintViolation<EurekaProperties>> violations1 = validator.validate(properties1);
        assertThat(violations1).isNotEmpty();

        EurekaProperties properties2 = new EurekaProperties();
        EurekaProperties.Instance instance2 = createValidInstance();
        instance2.setLeaseRenewalIntervalInSeconds(tooHigh);
        properties2.setInstance(instance2);
        properties2.setServer(createValidServer());
        properties2.setClient(createValidClient());

        Set<ConstraintViolation<EurekaProperties>> violations2 = validator.validate(properties2);
        assertThat(violations2).isNotEmpty();
    }

    /**
     * Property: Renewal percent threshold validation
     * For any EurekaProperties with renewal threshold outside 0-1 range,
     * validation should fail
     */
    @Property(tries = 100)
    @Label("Renewal percent threshold must be between 0 and 1")
    void renewalThresholdMustBeInValidRange(
            @ForAll @DoubleRange(min = -10.0, max = -0.01) double tooLow,
            @ForAll @DoubleRange(min = 1.01, max = 10.0) double tooHigh) {
        
        EurekaProperties properties1 = new EurekaProperties();
        properties1.setInstance(createValidInstance());
        EurekaProperties.Server server1 = createValidServer();
        server1.setRenewalPercentThreshold(tooLow);
        properties1.setServer(server1);
        properties1.setClient(createValidClient());

        Set<ConstraintViolation<EurekaProperties>> violations1 = validator.validate(properties1);
        assertThat(violations1).isNotEmpty();

        EurekaProperties properties2 = new EurekaProperties();
        properties2.setInstance(createValidInstance());
        EurekaProperties.Server server2 = createValidServer();
        server2.setRenewalPercentThreshold(tooHigh);
        properties2.setServer(server2);
        properties2.setClient(createValidClient());

        Set<ConstraintViolation<EurekaProperties>> violations2 = validator.validate(properties2);
        assertThat(violations2).isNotEmpty();
    }

    /**
     * Property: Default zone URL validation
     * For any EurekaProperties with blank default zone,
     * validation should fail
     */
    @Property(tries = 100)
    @Label("Default zone URL is required and cannot be blank")
    void defaultZoneCannotBeBlank(@ForAll @From("blankStrings") String blankZone) {
        EurekaProperties properties = new EurekaProperties();
        properties.setInstance(createValidInstance());
        properties.setServer(createValidServer());
        
        EurekaProperties.Client client = createValidClient();
        client.getServiceUrl().setDefaultZone(blankZone);
        properties.setClient(client);

        Set<ConstraintViolation<EurekaProperties>> violations = validator.validate(properties);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Default zone"));
    }

    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.oneOf(
            Arbitraries.just(""),
            Arbitraries.just("   "),
            Arbitraries.just("\t"),
            Arbitraries.just("\n")
        );
    }

    private EurekaProperties.Instance createValidInstance() {
        EurekaProperties.Instance instance = new EurekaProperties.Instance();
        instance.setHostname("localhost");
        instance.setPreferIpAddress(false);
        instance.setLeaseRenewalIntervalInSeconds(30);
        instance.setLeaseExpirationDurationInSeconds(90);
        return instance;
    }

    private EurekaProperties.Server createValidServer() {
        EurekaProperties.Server server = new EurekaProperties.Server();
        server.setEnableSelfPreservation(true);
        server.setRenewalPercentThreshold(0.85);
        server.setEvictionIntervalTimerInMs(30000);
        server.setResponseCacheUpdateIntervalMs(30000);
        server.setResponseCacheAutoExpirationInSeconds(180);
        return server;
    }

    private EurekaProperties.Client createValidClient() {
        EurekaProperties.Client client = new EurekaProperties.Client();
        client.setRegisterWithEureka(false);
        client.setFetchRegistry(false);
        
        EurekaProperties.Client.ServiceUrl serviceUrl = new EurekaProperties.Client.ServiceUrl();
        serviceUrl.setDefaultZone("http://localhost:8761/eureka/");
        client.setServiceUrl(serviceUrl);
        
        return client;
    }
}
