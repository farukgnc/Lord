package com.lord.data.playerdata;

import com.lord.Lord;
import com.lord.data.CachedData;
import com.lord.grant.Grant;
import com.lord.services.GrantCacheService;
import com.lord.services.ServiceRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissibleBase;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class PlayerDataListener implements Listener {

    private final PlayerDataCache playerDataCache;
    private final GrantCacheService grantCacheService;
    private final PlayerDataCalculator calculator;
    private final Field permissibleField;

    public PlayerDataListener(ServiceRegistry registry) {
        Lord plugin = registry.get(Lord.class);
        this.playerDataCache = registry.get(PlayerDataCache.class);
        this.grantCacheService = registry.get(GrantCacheService.class);
        this.calculator = new PlayerDataCalculator(registry);

        try {
            Class<?> craftHumanEntity = Class.forName("org.bukkit.craftbukkit.entity.CraftHumanEntity");
            this.permissibleField = craftHumanEntity.getDeclaredField("perm");
            this.permissibleField.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to initialize PlayerDataListener", e);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID playerUuid = event.getUniqueId();

        // 1. Akıllı önbellek servisinden grant'ları iste.
        // Bu, oyuncu ilk kez giriyorsa arka planda veritabanından çekme işlemini tetikler.
        CompletableFuture<Set<Grant>> grantsFuture = this.grantCacheService.getGrants(playerUuid);

        try {
            // 2. Grant'ların gelmesini bekle (10 saniye timeout ile).
            Set<Grant> grants = grantsFuture.get(10, TimeUnit.SECONDS);

            // 3. Gelen grant'lar ile oyuncunun tüm izin/meta verisini hesapla.
            CachedData calculatedData = this.calculator.calculate(grants);

            // 4. Hesaplanan bu hazır veriyi, izin sistemi için olan PlayerDataCache'e koy.
            this.playerDataCache.cacheData(playerUuid, calculatedData);

            // 5. Oyuncunun ham grant verilerini GrantCacheService'e tekrar koyarak
            // zaman aşımına uğramasını engelle ve online olduğu sürece taze kalmasını sağla.
            this.grantCacheService.preCacheGrants(playerUuid, grants);

        } catch (Exception e) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("Your data could not be loaded. Please try again.", NamedTextColor.RED));
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        try {
            // Bukkit'in izin sistemine kendi özel denetleyicimizi enjekte et.
            PermissibleBase oldPermissible = (PermissibleBase) this.permissibleField.get(player);
            PlayerPermissible newPermissible = new PlayerPermissible(player, this.playerDataCache, oldPermissible);
            this.permissibleField.set(player, newPermissible);
        } catch (Exception e) {
            e.printStackTrace();
            player.kick(Component.text("An error occurred while setting up your permissions."));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        // Oyuncu çıktığında, hem izin önbelleğini hem de grant önbelleğini temizle.
        this.playerDataCache.invalidate(playerUuid);
        this.grantCacheService.invalidate(playerUuid);
    }
}