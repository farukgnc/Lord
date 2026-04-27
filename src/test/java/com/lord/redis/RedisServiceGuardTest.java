package com.lord.redis;

import com.lord.Lord;
import com.lord.config.impl.MainConfig;
import com.lord.service.ServiceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class RedisServiceGuardTest {

    @Test
    void shouldSafelyNoOpWhenNotConnected() {
        ServiceRegistry registry = new ServiceRegistry();

        Lord plugin = Mockito.mock(Lord.class);
        MainConfig config = Mockito.mock(MainConfig.class);

        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(config.getServerId()).thenReturn("server-1");

        registry.register(Lord.class, plugin);
        registry.register(MainConfig.class, config);

        RedisService redisService = new RedisService(registry);

        assertThat(redisService.get("missing")).isNull();

        assertThatCode(() -> redisService.publish("c", "m")).doesNotThrowAnyException();
        assertThatCode(() -> redisService.subscribe("c", null)).doesNotThrowAnyException();
        assertThatCode(() -> redisService.set("k", "v")).doesNotThrowAnyException();
        assertThatCode(() -> redisService.setex("k", 5, "v")).doesNotThrowAnyException();
        assertThatCode(() -> redisService.del("k")).doesNotThrowAnyException();
        assertThatCode(redisService::disconnect).doesNotThrowAnyException();
    }
}
