package com.lord.punishment.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PunishmentEnumsTest {

    @Test
    void shouldExposePastTenseValues() {
        assertThat(PunishmentType.BAN.getPastTense()).isEqualTo("banned");
        assertThat(PunishmentType.MUTE.getPastTense()).isEqualTo("muted");
        assertThat(PunishmentType.KICK.getPastTense()).isEqualTo("kicked");
        assertThat(PunishmentType.WARN.getPastTense()).isEqualTo("warned");
    }

    @Test
    void shouldCycleStatusFilter() {
        assertThat(PunishmentStatusFilter.ALL.next()).isEqualTo(PunishmentStatusFilter.ACTIVE);
        assertThat(PunishmentStatusFilter.ACTIVE.next()).isEqualTo(PunishmentStatusFilter.INACTIVE);
        assertThat(PunishmentStatusFilter.INACTIVE.next()).isEqualTo(PunishmentStatusFilter.ALL);
        assertThat(PunishmentStatusFilter.ACTIVE.getDisplayName()).isEqualTo("Active");
    }
}
