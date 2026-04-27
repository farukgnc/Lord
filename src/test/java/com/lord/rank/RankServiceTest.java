package com.lord.rank;

import com.lord.Lord;
import com.lord.rank.exceptions.RankAlreadyExistsException;
import com.lord.rank.repositories.RankRepository;
import com.lord.service.ServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class RankServiceTest {

    private ServiceRegistry registry;
    private RankRepository rankRepository;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
        rankRepository = Mockito.mock(RankRepository.class);

        Lord plugin = Mockito.mock(Lord.class);

        registry.register(Lord.class, plugin);
        registry.register(RankRepository.class, rankRepository);
    }

    @Test
    void shouldCreateRankSuccessfully() throws Exception {
        when(rankRepository.findByName("admin")).thenReturn(Optional.empty());
        when(rankRepository.save(any(Rank.class))).thenReturn(CompletableFuture.completedFuture(true));

        RankService rankService = new RankService(registry);
        Rank created = rankService.createRank("admin", 100, "[A]", null, Set.of("default"));

        assertThat(created.getName()).isEqualTo("admin");
        assertThat(created.getPriority()).isEqualTo(100);
        assertThat(created.getParentRankNames()).contains("default");
    }

    @Test
    void shouldThrowWhenRankAlreadyExists() {
        when(rankRepository.findByName("admin")).thenReturn(Optional.of(new Rank("admin")));

        RankService rankService = new RankService(registry);

        assertThatThrownBy(() -> rankService.createRank("admin", 100, null, null, Set.of()))
                .isInstanceOf(RankAlreadyExistsException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldThrowWhenSaveFails() {
        when(rankRepository.findByName(eq("admin"))).thenReturn(Optional.empty());
        when(rankRepository.save(any(Rank.class))).thenReturn(CompletableFuture.completedFuture(false));

        RankService rankService = new RankService(registry);

        assertThatThrownBy(() -> rankService.createRank("admin", 100, null, null, Set.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to save rank");
    }
}
