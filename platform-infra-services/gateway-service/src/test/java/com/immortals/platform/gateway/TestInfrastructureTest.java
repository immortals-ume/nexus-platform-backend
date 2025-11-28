package com.immortals.platform.gateway;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify testing infrastructure is properly set up
 */
@Testcontainers
class TestInfrastructureTest {

    @Test
    void testJUnit5IsWorking() {
        assertTrue(true, "JUnit 5 is working");
    }

    @Test
    void testTestContainersIsAvailable() {
        assertTrue(this.getClass().isAnnotationPresent(Testcontainers.class),
                "TestContainers annotation is available");
    }
}
