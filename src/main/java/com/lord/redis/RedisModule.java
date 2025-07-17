package com.lord.redis;

import com.lord.module.Module;
import com.lord.redis.sync.GrantSyncService;
import com.lord.redis.sync.PunishmentSyncService;
import com.lord.redis.sync.RankSyncService;
import com.lord.service.ServiceRegistry;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

public class RedisModule implements Module {
    
    private final ServiceRegistry serviceRegistry;
    private final Logger logger;
    
    private RedisService redisService;
    
    public RedisModule(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        this.logger = Bukkit.getLogger();
    }
    
    @Override
    public void enable() {
        logger.info("Enabling Redis module...");
        
        // Initialize Redis service
        this.redisService = new RedisService(serviceRegistry);
        serviceRegistry.register(RedisService.class, redisService);
        
        // Connect to Redis
        boolean connected = redisService.connect().join();
        if (!connected) {
            logger.severe("Failed to connect to Redis! Redis synchronization will be disabled.");
            return;
        }
        
        // Register sync services
        serviceRegistry.register(PunishmentSyncService.class, new PunishmentSyncService(serviceRegistry));
        serviceRegistry.register(GrantSyncService.class, new GrantSyncService(serviceRegistry));
        serviceRegistry.register(RankSyncService.class, new RankSyncService(serviceRegistry));
        
        logger.info("Redis module enabled successfully!");
    }
    
    @Override
    public void disable() {
        logger.info("Disabling Redis module...");
        
        if (redisService != null) {
            redisService.disconnect();
        }
        
        logger.info("Redis module disabled!");
    }

    @Override
    public String getName() {
        return "Redis";
    }
}