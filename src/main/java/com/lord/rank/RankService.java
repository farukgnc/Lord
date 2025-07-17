package com.lord.rank;

import com.lord.rank.exceptions.RankAlreadyExistsException;
import com.lord.rank.repositories.RankRepository;
import com.lord.redis.sync.RankSyncService;
import com.lord.service.ServiceRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RankService {

    private final RankRepository rankRepository;
    private RankSyncService rankSyncService;
    
    public RankService(ServiceRegistry registry) {
        this.rankRepository = registry.get(RankRepository.class);
        
        // Initialize Redis sync service if available
        try {
            this.rankSyncService = registry.get(RankSyncService.class);
        } catch (Exception e) {
            // Redis sync service not available, continue without it
            this.rankSyncService = null;
        }
    }
    
    /**
     * Creates a new rank
     */
    public Rank createRank(String name, int priority, String prefix, String suffix, Set<String> parents) throws RankAlreadyExistsException {
        if (this.rankRepository.findByName(name).isPresent()) {
            throw new RankAlreadyExistsException("A rank with name " + name + " already exists.");
        }

        Rank newRank = new Rank(name);
        newRank.setPriority(priority);
        if (prefix != null) newRank.setPrefix(prefix);
        if (suffix != null) newRank.setSuffix(suffix);
        if (parents != null) newRank.getParentRankNames().addAll(parents);

        this.rankRepository.save(newRank);
        
        // Broadcast rank creation to other servers via Redis
        if (rankSyncService != null) {
            rankSyncService.broadcastRankCreate(newRank);
        }
        
        return newRank;
    }
    
    /**
     * Updates an existing rank
     */
    public CompletableFuture<Boolean> updateRank(String name, Integer priority, String prefix, String suffix, Set<String> parents, CommandSender updater) {
        Optional<Rank> rankOpt = rankRepository.findByName(name);
        if (rankOpt.isEmpty()) {
            updater.sendMessage(Component.text("Rank '" + name + "' not found.", NamedTextColor.RED));
            return CompletableFuture.completedFuture(false);
        }
        
        Rank rank = rankOpt.get();
        boolean changed = false;
        
        if (priority != null && rank.getPriority() != priority) {
            rank.setPriority(priority);
            changed = true;
        }
        
        if (prefix != null && !prefix.equals(rank.getPrefix())) {
            rank.setPrefix(prefix);
            changed = true;
        }
        
        if (suffix != null && !suffix.equals(rank.getSuffix())) {
            rank.setSuffix(suffix);
            changed = true;
        }
        
        if (parents != null && !parents.equals(rank.getParentRankNames())) {
            rank.getParentRankNames().clear();
            rank.getParentRankNames().addAll(parents);
            changed = true;
        }
        
        if (!changed) {
            updater.sendMessage(Component.text("No changes made to rank '" + name + "'.", NamedTextColor.YELLOW));
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            rankRepository.save(rank);
            
            // Broadcast rank update to other servers via Redis
            if (rankSyncService != null) {
                rankSyncService.broadcastRankUpdate(rank);
            }
            
            String updaterName = (updater instanceof Player) ? updater.getName() : "Console";
            Component message = MiniMessage.miniMessage().deserialize(
                "<yellow><b>RANK UPDATED</b></yellow> <gray>»</gray> Rank <white><rank></white> was updated by <white><updater></white>.",
                Placeholder.unparsed("rank", name),
                Placeholder.unparsed("updater", updaterName)
            );
            
            Bukkit.broadcast(message);
            return true;
        });
    }
    
    /**
     * Deletes a rank
     */
    public CompletableFuture<Boolean> deleteRank(String name, CommandSender deleter) {
        Optional<Rank> rankOpt = rankRepository.findByName(name);
        if (rankOpt.isEmpty()) {
            deleter.sendMessage(Component.text("Rank '" + name + "' not found.", NamedTextColor.RED));
            return CompletableFuture.completedFuture(false);
        }
        
        return rankRepository.delete(name).thenApply(success -> {
            if (success) {
                // Broadcast rank deletion to other servers via Redis
                if (rankSyncService != null) {
                    rankSyncService.broadcastRankDelete(name);
                }
                
                String deleterName = (deleter instanceof Player) ? deleter.getName() : "Console";
                Component message = MiniMessage.miniMessage().deserialize(
                    "<red><b>RANK DELETED</b></red> <gray>»</gray> Rank <white><rank></white> was deleted by <white><deleter></white>.",
                    Placeholder.unparsed("rank", name),
                    Placeholder.unparsed("deleter", deleterName)
                );
                
                Bukkit.broadcast(message);
            }
            return success;
        });
    }
    
    /**
     * Gets a rank by name
     */
    public Optional<Rank> getRank(String name) {
        return rankRepository.findByName(name);
    }
    
    /**
     * Gets all ranks
     */
    public Set<Rank> getAllRanks() {
        return rankRepository.getAllRanks();
    }
    
    /**
     * Checks if the rank repository is empty
     */
    public CompletableFuture<Boolean> isEmpty() {
        return rankRepository.isEmpty();
    }
    
    /**
     * Creates default ranks if the repository is empty
     */
    public void createDefaultRanks() {
        try {
            createRank("default", 1, "[Player]", null, Set.of());
            createRank("mod", 100, "<gray>[Mod]", null, Set.of("default"));
            createRank("admin", 200, "<red>[Admin]", null, Set.of("mod"));
            createRank("owner", 999, "<dark_red>[Owner]", null, Set.of("admin"));
        } catch (RankAlreadyExistsException e) {
            // Expected when server restarts - ranks already exist
        }
    }
}