package com.lord.data.playerdata;

import com.lord.data.CachedData;
import com.lord.grant.Grant;
import com.lord.grant.GrantCacheService;
import com.lord.rank.Rank;
import com.lord.rank.repositories.RankRepository;
import com.lord.service.ServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class PlayerDataCacheTest {

    private ServiceRegistry registry;
    private GrantCacheService grantCacheService;
    private RankRepository rankRepository;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
        grantCacheService = Mockito.mock(GrantCacheService.class);
        rankRepository = Mockito.mock(RankRepository.class);

        registry.register(GrantCacheService.class, grantCacheService);
        registry.register(RankRepository.class, rankRepository);
    }

    @Test
    void shouldCacheAndInvalidatePlayerData() {
        PlayerDataCache cache = new PlayerDataCache(registry);
        UUID player = UUID.randomUUID();

        CachedData data = new CachedData(
                new com.lord.data.PermissionData(java.util.Map.of("test.node", true)),
                new com.lord.data.MetaData("[X]", null, "default")
        );

        cache.cacheData(player, data);

        assertThat(cache.getPlayerData(player)).contains(data);

        cache.invalidate(player);

        assertThat(cache.getPlayerData(player)).isEmpty();
    }

    @Test
    void shouldRefreshPlayerDataFromGrantCache() {
        PlayerDataCache cache = new PlayerDataCache(registry);
        UUID player = UUID.randomUUID();

        Rank rank = new Rank("default");
        rank.setPriority(1);
        rank.getPermissions().add("example.use");

        Grant grant = new Grant(player, "default", null, Duration.ZERO);

        when(grantCacheService.getGrants(player)).thenReturn(CompletableFuture.completedFuture(Set.of(grant)));
        when(rankRepository.findByName("default")).thenReturn(Optional.of(rank));

        cache.refreshPlayerData(player).join();

        Optional<CachedData> cached = cache.getPlayerData(player);
        assertThat(cached).isPresent();
        assertThat(cached.get().getPermissionData().hasPermission("example.use")).isTrue();
        assertThat(cached.get().getMetaData().getPrimaryRank()).isEqualTo("default");
    }
}
