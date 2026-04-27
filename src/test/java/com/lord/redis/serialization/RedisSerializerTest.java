package com.lord.redis.serialization;

import com.lord.grant.Grant;
import com.lord.punishment.Punishment;
import com.lord.punishment.enums.PunishmentType;
import com.lord.rank.Rank;
import com.lord.redis.events.GrantSyncEvent;
import com.lord.redis.events.PunishmentSyncEvent;
import com.lord.redis.events.RankSyncEvent;
import com.lord.redis.events.RedisEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisSerializerTest {

    @Test
    void shouldSerializeAndDeserializeGrantEvent() {
        Grant grant = new Grant(UUID.randomUUID(), "admin", UUID.randomUUID(), Duration.ofHours(1));
        GrantSyncEvent event = new GrantSyncEvent(
                "server-1",
                GrantSyncEvent.Action.CREATE,
                grant.getUniqueId(),
                grant.getGranteeUuid(),
                "Notch",
                "Console",
                grant
        );

        String json = RedisSerializer.serialize(event);
        RedisEvent deserialized = RedisSerializer.deserialize(json);

        assertThat(deserialized).isInstanceOf(GrantSyncEvent.class);
        GrantSyncEvent restored = (GrantSyncEvent) deserialized;
        assertThat(restored.getAction()).isEqualTo(GrantSyncEvent.Action.CREATE);
        assertThat(restored.getGrant().getDuration()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void shouldSerializeAndDeserializePunishmentEvent() {
        Punishment punishment = new Punishment(PunishmentType.BAN, UUID.randomUUID(), "reason", null, Duration.ZERO);
        PunishmentSyncEvent event = new PunishmentSyncEvent(
                "server-2",
                PunishmentSyncEvent.Action.UPDATE,
                punishment.getUniqueId(),
                punishment.getPunishedUuid(),
                "Steve",
                "Admin",
                punishment
        );

        String json = RedisSerializer.serialize(event);
        RedisEvent deserialized = RedisSerializer.deserialize(json);

        assertThat(deserialized).isInstanceOf(PunishmentSyncEvent.class);
        PunishmentSyncEvent restored = (PunishmentSyncEvent) deserialized;
        assertThat(restored.getPunishment().getType()).isEqualTo(PunishmentType.BAN);
        assertThat(restored.getAction()).isEqualTo(PunishmentSyncEvent.Action.UPDATE);
    }

    @Test
    void shouldSerializeAndDeserializeRankEvent() {
        Rank rank = new Rank("mod");
        rank.setPriority(100);
        RankSyncEvent event = new RankSyncEvent("server-3", RankSyncEvent.Action.UPDATE, "mod", rank);

        String json = RedisSerializer.serialize(event);
        RedisEvent deserialized = RedisSerializer.deserialize(json);

        assertThat(deserialized).isInstanceOf(RankSyncEvent.class);
        RankSyncEvent restored = (RankSyncEvent) deserialized;
        assertThat(restored.getRankName()).isEqualTo("mod");
        assertThat(restored.getAction()).isEqualTo(RankSyncEvent.Action.UPDATE);
    }

    @Test
    void shouldThrowForUnknownEventType() {
        String invalidJson = "{\"type\":\"UnknownEvent\",\"data\":\"{}\"}";

        assertThatThrownBy(() -> RedisSerializer.deserialize(invalidJson))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown event type");
    }
}
