package com.lord.grant;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GrantModelTest {

    @Test
    void shouldTreatZeroDurationAsPermanent() {
        Grant grant = new Grant(UUID.randomUUID(), "default", null, Duration.ZERO);

        assertThat(grant.isPermanent()).isTrue();
        assertThat(grant.isActive()).isTrue();
        assertThat(grant.getExpiry()).isEqualTo(Instant.MAX);
    }

    @Test
    void shouldDetectExpiredGrant() {
        Grant expired = new Grant(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "mod",
                null,
                Instant.now().minus(Duration.ofHours(2)),
                Duration.ofHours(1)
        );

        assertThat(expired.isPermanent()).isFalse();
        assertThat(expired.isActive()).isFalse();
    }

    @Test
    void shouldDetectActiveTimedGrant() {
        Grant active = new Grant(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "admin",
                null,
                Instant.now().minus(Duration.ofMinutes(10)),
                Duration.ofHours(1)
        );

        assertThat(active.isActive()).isTrue();
        assertThat(active.getExpiry()).isAfter(Instant.now());
    }
}
