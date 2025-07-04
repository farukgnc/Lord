package com.lord.punishment;

import com.lord.Lord;
import com.lord.punishment.enums.PunishmentType;
import com.lord.services.ServiceRegistry;
import com.lord.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class PunishmentListener implements Listener {

    // Artık repository'e değil, akıllı önbellek servisine bağımlı.
    private final PunishmentCacheService punishmentCacheService;
    private final Lord plugin;

    public PunishmentListener(ServiceRegistry registry) {
        this.punishmentCacheService = registry.get(PunishmentCacheService.class);
        this.plugin = registry.get(Lord.class);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID playerUuid = event.getUniqueId();

        try {
            // 1. Akıllı önbellekten oyuncunun ceza geçmişini iste.
            // Bu, oyuncu daha önce sorgulanmadıysa veritabanından asenkron olarak çeker.
            // Biz zaten asenkron bir olayda olduğumuz için, .get() ile beklemek güvenlidir ve sunucuyu kilitlemez.
            List<Punishment> punishments = punishmentCacheService.getPunishments(playerUuid).get(5, TimeUnit.SECONDS);

            // 2. Gelen cezalar içinde aktif bir BAN olup olmadığını kontrol et.
            Optional<Punishment> activeBan = punishments.stream()
                    .filter(p -> p.getType() == PunishmentType.BAN && p.isActive())
                    .findFirst();

            // 3. Eğer aktif bir ban varsa, oyuncunun girişini engelle.
            if (activeBan.isPresent()) {
                Punishment ban = activeBan.get();
                String reason = ban.getReason();
                String remainingTime = ban.isPermanent() ? "Permanent" : TimeUtil.formatDuration(Duration.between(Instant.now(), ban.getExpiry()));

                Component kickMessage = MiniMessage.miniMessage().deserialize(
                        "<red>You are banned from this server!\n \n<gray>Reason: <white><reason>\n<gray>Expires in: <white><expires>",
                        Placeholder.unparsed("reason", reason),
                        Placeholder.unparsed("expires", remainingTime)
                );
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);
            }
        } catch (Exception e) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("An error occurred while checking your punishment status.", NamedTextColor.RED));
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Akıllı önbellekten oyuncunun ceza geçmişini iste.
        // Oyuncu online olduğu için bu veri zaten cache'de olmalı ve anında dönmelidir.
        punishmentCacheService.getPunishments(player.getUniqueId()).thenAcceptAsync(punishments -> {
            Optional<Punishment> activeMute = punishments.stream()
                    .filter(p -> p.getType() == PunishmentType.MUTE && p.isActive())
                    .findFirst();

            if (activeMute.isPresent()) {
                // Eğer aktif mute varsa, ana thread'e dönerek olayı iptal et ve mesaj gönder.
                Bukkit.getScheduler().runTask(plugin, () -> {
                    event.setCancelled(true);
                    Punishment mute = activeMute.get();
                    String reason = mute.getReason();
                    String remainingTime = mute.isPermanent() ? "Permanent" : TimeUtil.formatDuration(Duration.between(Instant.now(), mute.getExpiry()));

                    player.sendMessage(Component.text("--------------------------------", NamedTextColor.RED));
                    player.sendMessage(Component.text("You are currently muted.", NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("Reason: ", NamedTextColor.GRAY).append(Component.text(reason, NamedTextColor.WHITE)));
                    player.sendMessage(Component.text("Expires in: ", NamedTextColor.GRAY).append(Component.text(remainingTime, NamedTextColor.WHITE)));
                    player.sendMessage(Component.text("--------------------------------", NamedTextColor.RED));
                });
            }
        });
    }
}