package com.lord.permission;

import com.lord.data.cached.CachedData;
import com.lord.services.ServiceRegistry;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PermissionCache {

    private final Map<UUID, CachedData> cache = new ConcurrentHashMap<>();
    private final PermissionCalculator calculator;

    public PermissionCache(ServiceRegistry registry) {
        this.calculator = new PermissionCalculator(registry);
    }

    /**
     * Gets a player's cached data.
     * If not present in the cache, it calculates, caches, and then returns it.
     *
     * @param playerUuid The UUID of the player.
     * @return The player's CachedData.
     */
    public CachedData getPlayerData(UUID playerUuid) {
        // computeIfAbsent: Eğer UUID için bir veri yoksa, calculator.calculate'ı çalıştırır,
        // sonucu cache'e ekler ve sonra o yeni sonucu döndürür. Varsa, mevcut olanı döndürür.
        // Bu, if(cache.contains) kontrolünden çok daha temiz ve atomik bir işlemdir.
        return this.cache.computeIfAbsent(playerUuid, this.calculator::calculate);
    }

    /**
     * Checks if a player has a specific permission node.
     *
     * @param player The player to check.
     * @param node   The permission node.
     * @return True if the player has the permission, false otherwise.
     */
    public boolean hasPermission(Player player, String node) {
        CachedData data = getPlayerData(player.getUniqueId());
        return data.getPermissionData().hasPermission(node);
    }

    /**
     * Invalidates and removes a player's data from the cache.
     * This should be called whenever a player's grants are changed.
     *
     * @param playerUuid The UUID of the player to invalidate.
     */
    public void invalidate(UUID playerUuid) {
        this.cache.remove(playerUuid);
    }
}