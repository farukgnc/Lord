package com.lord.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceRegistryTest {

    @Test
    void shouldRegisterAndResolveService() {
        ServiceRegistry registry = new ServiceRegistry();
        String value = "hello";

        registry.register(String.class, value);

        assertThat(registry.get(String.class)).isEqualTo("hello");
    }

    @Test
    void shouldThrowWhenServiceIsMissing() {
        ServiceRegistry registry = new ServiceRegistry();

        assertThatThrownBy(() -> registry.get(Integer.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Service not found");
    }

    @Test
    void shouldUnregisterService() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.register(String.class, "bye");

        registry.unregister(String.class);

        assertThatThrownBy(() -> registry.get(String.class))
                .isInstanceOf(IllegalStateException.class);
    }
}
