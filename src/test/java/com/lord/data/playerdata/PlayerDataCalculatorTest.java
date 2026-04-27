package com.lord.data.playerdata;

import com.lord.data.CachedData;
import com.lord.grant.Grant;
import com.lord.rank.Rank;
import com.lord.rank.repositories.RankRepository;
import com.lord.service.ServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class PlayerDataCalculatorTest {

    private ServiceRegistry registry;
    private RankRepository rankRepository;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
        rankRepository = Mockito.mock(RankRepository.class);
        registry.register(RankRepository.class, rankRepository);
    }

    @Test
    void shouldResolveInheritanceAndPriorityCorrectly() {
        Rank parent = new Rank("default");
        parent.setPriority(1);
        parent.setPrefix("[P]");
        parent.getPermissions().add("chat.color");

        Rank child = new Rank("mod");
        child.setPriority(100);
        child.setPrefix("[M]");
        child.getPermissions().add("-chat.color");
        child.getPermissions().add("staff.kick");
        child.getParentRankNames().add("default");

        when(rankRepository.findByName("mod")).thenReturn(Optional.of(child));
        when(rankRepository.findByName("default")).thenReturn(Optional.of(parent));

        Grant activeGrant = new Grant(UUID.randomUUID(), "mod", null, Duration.ofHours(1));

        PlayerDataCalculator calculator = new PlayerDataCalculator(registry);
        CachedData cachedData = calculator.calculate(Set.of(activeGrant));

        assertThat(cachedData.getMetaData().getPrimaryRank()).isEqualTo("mod");
        assertThat(cachedData.getMetaData().getPrefix()).isEqualTo("[M]");
        assertThat(cachedData.getPermissionData().hasPermission("staff.kick")).isTrue();
        assertThat(cachedData.getPermissionData().hasPermission("chat.color")).isFalse();
    }

    @Test
    void shouldIgnoreInactiveGrants() {
        Rank rank = new Rank("vip");
        rank.setPriority(50);

        when(rankRepository.findByName("vip")).thenReturn(Optional.of(rank));

        Grant inactiveGrant = new Grant(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "vip",
                null,
                Instant.now().minus(Duration.ofHours(2)),
                Duration.ofHours(1)
        );

        PlayerDataCalculator calculator = new PlayerDataCalculator(registry);
        CachedData cachedData = calculator.calculate(Set.of(inactiveGrant));

        assertThat(cachedData.getMetaData().getPrimaryRank()).isNull();
        assertThat(cachedData.getPermissionData().hasPermission("vip.test")).isFalse();
    }
}
