package com.lord.factory;

import com.lord.grant.GrantCacheService;
import com.lord.grant.repositories.GrantRepository;
import com.lord.punishment.PunishmentCacheService;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.rank.repositories.RankRepository;
import com.lord.service.ServiceRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRepositoryFactoryTest {

    @Test
    void shouldCreateRepositoriesAndCaches() {
        ServiceRegistry registry = new ServiceRegistry();
        InMemoryRepositoryFactory factory = new InMemoryRepositoryFactory(registry);

        assertThat(factory.connect().join()).isTrue();

        factory.createRepositories().join();

        assertThat(registry.get(RankRepository.class)).isNotNull();
        assertThat(registry.get(GrantRepository.class)).isNotNull();
        assertThat(registry.get(PunishmentRepository.class)).isNotNull();
        assertThat(registry.get(GrantCacheService.class)).isNotNull();
        assertThat(registry.get(PunishmentCacheService.class)).isNotNull();
    }
}
