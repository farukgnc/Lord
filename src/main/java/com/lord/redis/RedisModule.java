package com.lord.redis;

import com.lord.module.Module;
import com.lord.redis.sync.GrantSyncService;
import com.lord.redis.sync.PunishmentSyncService;
import com.lord.redis.sync.RankSyncService;
import com.lord.services.ServiceRegistry;

import java.util.logging.Logger;

public class RedisModule extends Module {
    
    private final ServiceRegistry serviceRegistry;
    private final Logger logger;
    
    private RedisService redisService;
    private PunishmentSyncService punishmentSyncService;
    private GrantSyncService grantSyncService;
    private RankSyncService rankSyncService;
    
    public RedisModule(ServiceRegistry serviceRegistry) {
        super("Redis");
        this.serviceRegistry = serviceRegistry;
        this.logger = serviceRegistry.get(Logger.class);
    }
    
    @Override
    public void onEnable() {
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
        
        // Initialize sync services
        this.punishmentSyncService = new PunishmentSyncService(serviceRegistry);
        this.grantSyncService = new GrantSyncService(serviceRegistry);
        this.rankSyncService = new RankSyncService(serviceRegistry);
        
        // Register sync services
        serviceRegistry.register(PunishmentSyncService.class, punishmentSyncService);
        serviceRegistry.register(GrantSyncService.class, grantSyncService);
        serviceRegistry.register(RankSyncService.class, rankSyncService);
        
        logger.info("Redis module enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        logger.info("Disabling Redis module...");
        
        if (redisService != null) {
            redisService.disconnect();
        }
        
        logger.info("Redis module disabled!");
    }
}