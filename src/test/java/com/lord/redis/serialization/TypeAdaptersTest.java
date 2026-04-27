package com.lord.redis.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TypeAdaptersTest {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .registerTypeAdapter(Duration.class, new DurationTypeAdapter())
            .create();

    @Test
    void shouldSerializeAndDeserializeInstant() {
        Instant now = Instant.now();

        String json = gson.toJson(now, Instant.class);
        Instant restored = gson.fromJson(json, Instant.class);

        assertThat(restored.toEpochMilli()).isEqualTo(now.toEpochMilli());
    }

    @Test
    void shouldSerializeAndDeserializeDuration() {
        Duration duration = Duration.ofMinutes(42);

        String json = gson.toJson(duration, Duration.class);
        Duration restored = gson.fromJson(json, Duration.class);

        assertThat(restored).isEqualTo(duration);
    }

    @Test
    void shouldHandleNullValues() {
        assertThat(gson.toJson(null, Instant.class)).isEqualTo("null");
        assertThat(gson.toJson(null, Duration.class)).isEqualTo("null");

        assertThat(gson.fromJson("null", Instant.class)).isNull();
        assertThat(gson.fromJson("null", Duration.class)).isNull();
    }
}
