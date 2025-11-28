package com.immortals.platform.config;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify jqwik property-based testing infrastructure is properly set up
 */
class PropertyBasedTestInfrastructureTest {

    @Property
    void stringLengthIsNonNegative(@ForAll String anyString) {
        assertThat(anyString.length()).isGreaterThanOrEqualTo(0);
    }

    @Property
    void additionIsCommutative(@ForAll int a, @ForAll int b) {
        assertThat(a + b).isEqualTo(b + a);
    }
}
