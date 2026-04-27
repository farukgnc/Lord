package com.lord.punishment.repositories;

import com.lord.punishment.Punishment;
import com.lord.punishment.enums.PunishmentStatusFilter;
import com.lord.punishment.enums.PunishmentType;
import com.lord.punishment.repositories.impl.InMemoryPunishmentRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryPunishmentRepositoryTest {

    @Test
    void shouldFilterByTypeAndActiveStatus() {
        InMemoryPunishmentRepository repository = new InMemoryPunishmentRepository();
        UUID target = UUID.randomUUID();

        Punishment activeBan = new Punishment(PunishmentType.BAN, target, "rule break", null, Duration.ofHours(1));
        Punishment inactiveMute = new Punishment(PunishmentType.MUTE, target, "spam", null, Duration.ofMinutes(10));
        inactiveMute.setPardoned(true);

        repository.save(activeBan).join();
        repository.save(inactiveMute).join();

        List<Punishment> activeBans = repository
                .findWithFilters(target, PunishmentStatusFilter.ACTIVE, PunishmentType.BAN)
                .join();

        List<Punishment> activeMutes = repository
                .findWithFilters(target, PunishmentStatusFilter.ACTIVE, PunishmentType.MUTE)
                .join();

        List<Punishment> inactive = repository
                .findWithFilters(target, PunishmentStatusFilter.INACTIVE, null)
                .join();

        assertThat(activeBans).containsExactly(activeBan);
        assertThat(activeMutes).isEmpty();
        assertThat(inactive).contains(inactiveMute);
    }
}
