package com.lord.data;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionDataTest {

    @Test
    void shouldResolveExactPermissionFirst() {
        PermissionData data = new PermissionData(Map.of(
                "essentials.fly", true,
                "essentials.*", false
        ));

        assertThat(data.hasPermission("essentials.fly")).isTrue();
    }

    @Test
    void shouldResolveWildcardPermission() {
        PermissionData data = new PermissionData(Map.of(
                "essentials.*", true
        ));

        assertThat(data.hasPermission("essentials.gamemode")).isTrue();
    }

    @Test
    void shouldResolveGlobalWildcardAsFallback() {
        PermissionData data = new PermissionData(Map.of("*", true));

        assertThat(data.hasPermission("random.permission")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNothingMatches() {
        PermissionData data = new PermissionData(Map.of());

        assertThat(data.hasPermission("unknown.node")).isFalse();
    }

    @Test
    void shouldHandleNegativeNodesStoredAsFalse() {
        PermissionData data = new PermissionData(Map.of("essentials.fly", false));

        assertThat(data.hasPermission("essentials.fly")).isFalse();
    }
}
