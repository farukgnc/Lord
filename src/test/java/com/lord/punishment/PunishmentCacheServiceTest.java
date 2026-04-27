package com.lord.punishment;

import com.lord.punishment.enums.PunishmentStatusFilter;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.service.ServiceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PunishmentCacheServiceTest {

    @Test
    void shouldLoadFromRepositoryAndInvalidateCorrectly() {
        ServiceRegistry registry = new ServiceRegistry();
        PunishmentRepository repository = Mockito.mock(PunishmentRepository.class);
        registry.register(PunishmentRepository.class, repository);

        UUID player = UUID.randomUUID();
        List<Punishment> punishments = List.of(new Punishment(
                com.lord.punishment.enums.PunishmentType.WARN,
                player,
                "reason",
                null,
                Duration.ZERO
        ));

        when(repository.findWithFilters(player, PunishmentStatusFilter.ALL, null))
                .thenReturn(CompletableFuture.completedFuture(punishments));

        PunishmentCacheService cacheService = new PunishmentCacheService(registry);

        assertThat(cacheService.getPunishments(player).join()).hasSize(1);
        assertThat(cacheService.getPunishments(player).join()).hasSize(1);
        verify(repository, times(1)).findWithFilters(player, PunishmentStatusFilter.ALL, null);

        cacheService.invalidate(player);
        assertThat(cacheService.getPunishments(player).join()).hasSize(1);
        verify(repository, times(2)).findWithFilters(player, PunishmentStatusFilter.ALL, null);
    }
}
