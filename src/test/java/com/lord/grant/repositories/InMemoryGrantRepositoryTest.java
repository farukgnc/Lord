package com.lord.grant.repositories;

import com.lord.grant.Grant;
import com.lord.grant.repositories.impl.InMemoryGrantRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryGrantRepositoryTest {

    @Test
    void shouldSaveFindAndDeleteGrant() {
        InMemoryGrantRepository repository = new InMemoryGrantRepository();
        UUID playerUuid = UUID.randomUUID();
        Grant grant = new Grant(playerUuid, "default", null, Duration.ZERO);

        boolean saveResult = repository.save(grant).join();
        Set<Grant> playerGrants = repository.findByPlayer(playerUuid).join();

        assertThat(saveResult).isTrue();
        assertThat(playerGrants).contains(grant);
        assertThat(repository.findById(grant.getUniqueId()).join()).contains(grant);

        boolean deleteResult = repository.delete(grant).join();

        assertThat(deleteResult).isTrue();
        assertThat(repository.findByPlayer(playerUuid).join()).isEmpty();
    }
}
