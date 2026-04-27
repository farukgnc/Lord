package com.lord.rank.repositories;

import com.lord.rank.Rank;
import com.lord.rank.repositories.impl.InMemoryRankRepository;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRankRepositoryTest {

    @Test
    void shouldSaveFindAndDeleteRank() {
        InMemoryRankRepository repository = new InMemoryRankRepository();
        Rank rank = new Rank("Admin");
        rank.setPriority(100);

        boolean saved = repository.save(rank).join();

        assertThat(saved).isTrue();
        assertThat(repository.findByName("admin")).isPresent();
        assertThat(repository.findByName("Admin")).isPresent();

        boolean deleted = repository.delete("admin").join();

        assertThat(deleted).isTrue();
        assertThat(repository.findByName("admin")).isEmpty();
    }

    @Test
    void loadAllRanksShouldReturnSnapshotFuture() {
        InMemoryRankRepository repository = new InMemoryRankRepository();
        Rank rank = new Rank("mod");
        repository.save(rank).join();

        Set<Rank> loaded = repository.loadAllRanks().join();

        assertThat(loaded).hasSize(1);
        assertThat(loaded.iterator().next().getName()).isEqualTo("mod");
    }
}
