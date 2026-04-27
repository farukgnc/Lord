package com.lord.redis.utils;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RedisKeysTest {

    @Test
    void shouldGenerateExpectedChannelsAndKeys() {
        UUID uuid = UUID.randomUUID();

        assertThat(RedisKeys.PUNISHMENT_SYNC_CHANNEL).isEqualTo("lord:punishment:sync");
        assertThat(RedisKeys.GRANT_SYNC_CHANNEL).isEqualTo("lord:grant:sync");
        assertThat(RedisKeys.RANK_SYNC_CHANNEL).isEqualTo("lord:rank:sync");

        assertThat(RedisKeys.playerPunishments(uuid)).isEqualTo("lord:cache:punishments:" + uuid);
        assertThat(RedisKeys.playerGrants(uuid)).isEqualTo("lord:cache:grants:" + uuid);
        assertThat(RedisKeys.playerData(uuid)).isEqualTo("lord:cache:playerdata:" + uuid);
        assertThat(RedisKeys.rankData("admin")).isEqualTo("lord:cache:rank:admin");
        assertThat(RedisKeys.serverStatus("lobby-1")).isEqualTo("lord:server:status:lobby-1");
        assertThat(RedisKeys.serverHeartbeat("lobby-1")).isEqualTo("lord:server:heartbeat:lobby-1");
    }
}
