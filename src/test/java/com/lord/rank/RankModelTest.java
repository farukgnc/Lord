package com.lord.rank;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RankModelTest {

    @Test
    void shouldStoreRankProperties() {
        Rank rank = new Rank("admin");
        rank.setPriority(500);
        rank.setPrefix("[A]");
        rank.setSuffix("*");
        rank.getPermissions().add("core.use");
        rank.getParentRankNames().add("default");

        assertThat(rank.getName()).isEqualTo("admin");
        assertThat(rank.getPriority()).isEqualTo(500);
        assertThat(rank.getPrefix()).isEqualTo("[A]");
        assertThat(rank.getSuffix()).isEqualTo("*");
        assertThat(rank.getPermissions()).contains("core.use");
        assertThat(rank.getParentRankNames()).contains("default");
    }
}
