package com.lord.redis;

import com.lord.config.impl.MainConfig;
import com.lord.services.ServiceRegistry;
import lombok.Getter;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Redis {

    private final MainConfig mainConfig;
    private final Logger logger;

    @Getter
    private JedisPool jedisPool;

    public Redis(ServiceRegistry registry, Logger logger) {
        this.mainConfig = registry.get(MainConfig.class);
        this.logger = logger;
    }

    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String host = mainConfig.getRedisHost();
                int port = mainConfig.getRedisPort();
                String password = mainConfig.getRedisPassword();

                if (host == null || host.isEmpty()) {
                    logger.log(Level.SEVERE, "Redis host is not configured!");
                    return false;
                }

                JedisPoolConfig poolConfig = new JedisPoolConfig();
                poolConfig.setMaxTotal(128);

                if (password != null && !password.isEmpty()) {
                    this.jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
                } else {
                    this.jedisPool = new JedisPool(poolConfig, host, port, 2000);
                }

                // Test connection
                try (redis.clients.jedis.Jedis jedis = this.jedisPool.getResource()) {
                    logger.log(Level.INFO, "Redis connection successful. PING response: " + jedis.ping());
                }

                logger.log(Level.INFO, "Redis connection pool established.");
                return true;
            } catch (JedisConnectionException e) {
                logger.log(Level.SEVERE, "Failed to connect to Redis!", e);
                this.jedisPool = null;
                return false;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "An unexpected error occurred during Redis connection!", e);
                this.jedisPool = null;
                return false;
            }
        });
    }

    public void disconnect() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.log(Level.INFO, "Redis connection pool closed.");
        }
    }
}