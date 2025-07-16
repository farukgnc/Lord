package com.lord.grant;

import com.lord.data.playerdata.PlayerDataCache;
import com.lord.grant.repositories.GrantRepository;
import com.lord.redis.sync.GrantSyncService;
import com.lord.services.ServiceRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GrantService {
    
    private final ServiceRegistry registry;
    private final GrantRepository grantRepository;
    private final GrantCacheService grantCacheService;
    private final PlayerDataCache playerDataCache;
    private GrantSyncService grantSyncService;
    
    public GrantService(ServiceRegistry registry) {
        this.registry = registry;
        this.grantRepository = registry.get(GrantRepository.class);
        this.grantCacheService = registry.get(GrantCacheService.class);
        this.playerDataCache = registry.get(PlayerDataCache.class);
        
        // Initialize Redis sync service if available
        try {
            this.grantSyncService = registry.get(GrantSyncService.class);
        } catch (Exception e) {
            // Redis sync service not available, continue without it
            this.grantSyncService = null;
        }
    }
    
    /**
     * Creates a new grant for a player
     */
    public CompletableFuture<Grant> createGrant(UUID targetUuid, String targetName, String rankName, 
                                               CommandSender issuer, Duration duration) {
        
        UUID issuerUuid = (issuer instanceof Player p) ? p.getUniqueId() : null;
        Grant newGrant = new Grant(targetUuid, rankName, issuerUuid, duration);
        
        return grantRepository.save(newGrant).thenApply(savedGrant -> {
            // Broadcast grant creation to other servers via Redis
            if (grantSyncService != null) {
                grantSyncService.broadcastGrantCreate(newGrant);
            }
            
            // Refresh player data to apply the new grant immediately
            playerDataCache.refreshPlayerData(targetUuid).thenRun(() -> {
                // Send success message
                String issuerName = (issuer instanceof Player) ? issuer.getName() : "Console";
                String durationStr = duration.isZero() ? "permanently" : "for " + formatDuration(duration);
                
                Component message = MiniMessage.miniMessage().deserialize(
                    "<green><b>GRANT</b></green> <gray>»</gray> <white><target></white> was granted <yellow><rank></yellow> <duration> by <white><issuer></white>.",
                    Placeholder.unparsed("target", targetName),
                    Placeholder.unparsed("rank", rankName),
                    Placeholder.unparsed("duration", durationStr),
                    Placeholder.unparsed("issuer", issuerName)
                );
                
                Bukkit.broadcast(message);
            });
            
            return newGrant;
        });
    }
    
    /**
     * Removes a grant from a player
     */
    public CompletableFuture<Boolean> removeGrant(UUID grantId, UUID targetUuid, String targetName, CommandSender remover) {
        return grantRepository.findById(grantId).thenCompose(grantOpt -> {
            if (grantOpt.isEmpty()) {
                remover.sendMessage(Component.text("Grant not found.", NamedTextColor.RED));
                return CompletableFuture.completedFuture(false);
            }
            
            Grant grant = grantOpt.get();
            
            return grantRepository.delete(grantId).thenApply(success -> {
                if (success) {
                    // Broadcast grant deletion to other servers via Redis
                    if (grantSyncService != null) {
                        grantSyncService.broadcastGrantDelete(grantId, targetUuid);
                    }
                    
                    // Refresh player data to remove the grant immediately
                    playerDataCache.refreshPlayerData(targetUuid).thenRun(() -> {
                        String removerName = (remover instanceof Player) ? remover.getName() : "Console";
                        
                        Component message = MiniMessage.miniMessage().deserialize(
                            "<red><b>GRANT REMOVED</b></red> <gray>»</gray> <white><target></white>'s <yellow><rank></yellow> grant was removed by <white><remover></white>.",
                            Placeholder.unparsed("target", targetName),
                            Placeholder.unparsed("rank", grant.getRankName()),
                            Placeholder.unparsed("remover", removerName)
                        );
                        
                        Bukkit.broadcast(message);
                    });
                }
                return success;
            });
        });
    }
    
    /**
     * Gets all grants for a player
     */
    public CompletableFuture<Set<Grant>> getPlayerGrants(UUID playerUuid) {
        return grantCacheService.getGrants(playerUuid);
    }
    
    /**
     * Gets all active grants for a player
     */
    public CompletableFuture<Set<Grant>> getActiveGrants(UUID playerUuid) {
        return getPlayerGrants(playerUuid).thenApply(grants -> 
            grants.stream()
                  .filter(Grant::isActive)
                  .collect(java.util.stream.Collectors.toSet())
        );
    }
    
    /**
     * Checks if a player has a specific active grant
     */
    public CompletableFuture<Boolean> hasActiveGrant(UUID playerUuid, String rankName) {
        return getActiveGrants(playerUuid).thenApply(grants ->
            grants.stream().anyMatch(grant -> grant.getRankName().equalsIgnoreCase(rankName))
        );
    }
    
    /**
     * Invalidates the grant cache for a player
     */
    public void invalidateCache(UUID playerUuid) {
        grantCacheService.invalidate(playerUuid);
    }
    
    private String formatDuration(Duration duration) {
        if (duration.isZero()) {
            return "permanently";
        }
        
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m");
        
        return sb.toString().trim();
    }
}