package com.lord.grant;

import com.lord.grant.repositories.GrantRepository;
import com.lord.service.ServiceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrantCacheServiceTest {

    @Test
    void shouldLoadFromRepositoryAndInvalidateCorrectly() {
        ServiceRegistry registry = new ServiceRegistry();
        GrantRepository repository = Mockito.mock(GrantRepository.class);
        registry.register(GrantRepository.class, repository);

        UUID player = UUID.randomUUID();
        Set<Grant> grants = Set.of(new Grant(player, "default", null, Duration.ZERO));

        when(repository.findByPlayer(player)).thenReturn(CompletableFuture.completedFuture(grants));

        GrantCacheService cacheService = new GrantCacheService(registry);

        assertThat(cacheService.getGrants(player).join()).hasSize(1);
        assertThat(cacheService.getGrants(player).join()).hasSize(1);
        verify(repository, times(1)).findByPlayer(player);

        cacheService.invalidate(player);
        assertThat(cacheService.getGrants(player).join()).hasSize(1);
        verify(repository, times(2)).findByPlayer(player);
    }
}
