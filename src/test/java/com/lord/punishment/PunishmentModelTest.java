package com.lord.punishment;

import com.lord.punishment.enums.PunishmentType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PunishmentModelTest {

    @Test
    void shouldBeActiveWhenPermanentAndNotPardoned() {
        Punishment punishment = new Punishment(PunishmentType.BAN, UUID.randomUUID(), "reason", null, Duration.ZERO);

        assertThat(punishment.isPermanent()).isTrue();
        assertThat(punishment.isActive()).isTrue();
        assertThat(punishment.getExpiry()).isEqualTo(Instant.MAX);
    }

    @Test
    void shouldBeInactiveWhenPardoned() {
        Punishment punishment = new Punishment(PunishmentType.MUTE, UUID.randomUUID(), "spam", null, Duration.ofHours(3));
        punishment.setPardoned(true);

        assertThat(punishment.isActive()).isFalse();
    }

    @Test
    void shouldBeInactiveWhenExpired() {
        Punishment expired = new Punishment(
                UUID.randomUUID(),
                PunishmentType.BAN,
                UUID.randomUUID(),
                "reason",
                null,
                Instant.now().minus(Duration.ofHours(2)),
                Duration.ofHours(1)
        );

        assertThat(expired.isActive()).isFalse();
    }
}
