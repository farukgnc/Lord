package com.lord.redis;

import com.lord.Lord;
import com.lord.config.impl.MainConfig;
import com.lord.service.ServiceRegistry;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Getter
public class RedisService {

    private final Logger logger;
    private final MainConfig config;
    private final String serverId;
    
    private JedisPool jedisPool;
    private ExecutorService executorService;
    private boolean connected = false;

    public RedisService(ServiceRegistry serviceRegistry) {
        this.logger = serviceRegistry.get(Lord.class).getLogger();
        this.config = serviceRegistry.get(MainConfig.class);
        this.serverId = config.getServerId();
        this.executorService = Executors.newCachedThreadPool();
    }

    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JedisPoolConfig poolConfig = new JedisPoolConfig();
                poolConfig.setMaxTotal(10);
                poolConfig.setMaxIdle(5);
                poolConfig.setMinIdle(1);
                poolConfig.setTestOnBorrow(true);
                poolConfig.setTestOnReturn(true);
                poolConfig.setTestWhileIdle(true);

                if (config.getRedisPassword().isEmpty()) {
                    this.jedisPool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort());
                } else {
                    this.jedisPool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort(), 2000, config.getRedisPassword());
                }

                // Test connection
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.ping();
                }

                this.connected = true;
                logger.info("Redis connection established successfully!");
                return true;
            } catch (Exception e) {
                logger.severe("Failed to connect to Redis: " + e.getMessage());
                return false;
            }
        });
    }

    public void disconnect() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        connected = false;
    }

    public void publish(String channel, String message) {
        if (!connected) return;
        
        executorService.submit(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(channel, message);
            } catch (Exception e) {
                logger.warning("Failed to publish message to Redis: " + e.getMessage());
            }
        });
    }

    public void subscribe(String channel, JedisPubSub subscriber) {
        if (!connected) return;
        
        executorService.submit(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(subscriber, channel);
            } catch (Exception e) {
                logger.warning("Failed to subscribe to Redis channel: " + e.getMessage());
            }
        });
    }

    public void set(String key, String value) {
        if (!connected) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        } catch (Exception e) {
            logger.warning("Failed to set Redis key: " + e.getMessage());
        }
    }

    public void setex(String key, int seconds, String value) {
        if (!connected) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, seconds, value);
        } catch (Exception e) {
            logger.warning("Failed to setex Redis key: " + e.getMessage());
        }
    }

    public String get(String key) {
        if (!connected) return null;
        
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        } catch (Exception e) {
            logger.warning("Failed to get Redis key: " + e.getMessage());
            return null;
        }
    }

    public void del(String key) {
        if (!connected) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        } catch (Exception e) {
            logger.warning("Failed to delete Redis key: " + e.getMessage());
        }
    }
}