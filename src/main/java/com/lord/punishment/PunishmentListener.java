package com.lord.punishment;

import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.services.ServiceRegistry;
import com.lord.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent; // YENİ IMPORT

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID; // YENİ IMPORT

public final class PunishmentListener implements Listener {

    private final PunishmentRepository punishmentRepository;

    public PunishmentListener(ServiceRegistry registry) {
        this.punishmentRepository = registry.get(PunishmentRepository.class);
    }

    // --- YENİ METOT BAŞLANGICI ---
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID playerUuid = event.getUniqueId();

        // Oyuncunun aktif bir BAN cezası olup olmadığını kontrol et.
        Optional<Punishment> activeBan = this.punishmentRepository
                .findActiveByType(playerUuid, PunishmentType.BAN)
                .stream()
                .findFirst();

        // Eğer aktif bir ban varsa...
        if (activeBan.isPresent()) {
            Punishment punishment = activeBan.get();
            String reason = punishment.getReason();
            String remainingTime;

            if (punishment.isPermanent()) {
                remainingTime = "Permanent";
            } else {
                Duration timeLeft = Duration.between(Instant.now(), punishment.getExpiry());
                remainingTime = TimeUtil.formatDuration(timeLeft);
            }

            // Oyuncunun girişini reddetmek için bir kick mesajı oluştur.
            Component kickMessage = MiniMessage.miniMessage().deserialize(
                    """
                    <red>You are banned from this server!
                    
                    <gray>Reason: <white><reason>
                    <gray>Expires in: <white><expires>
                    """,
                    Placeholder.unparsed("reason", reason),
                    Placeholder.unparsed("expires", remainingTime)
            );

            // Oyuncunun girişine izin verme ve oluşturduğumuz mesajı göster.
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);
        }
    }
    // --- YENİ METOT BİTİŞİ ---

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        Optional<Punishment> activeMute = this.punishmentRepository
                .findActiveByType(player.getUniqueId(), PunishmentType.MUTE)
                .stream()
                .findFirst();

        if (activeMute.isPresent()) {
            event.setCancelled(true);

            Punishment punishment = activeMute.get();
            String reason = punishment.getReason();
            String remainingTime;

            if (punishment.isPermanent()) {
                remainingTime = "Permanent";
            } else {
                Duration timeLeft = Duration.between(Instant.now(), punishment.getExpiry());
                remainingTime = TimeUtil.formatDuration(timeLeft);
            }

            player.sendMessage(Component.text("--------------------------------", NamedTextColor.RED));
            player.sendMessage(Component.text("You are currently muted.", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Reason: ", NamedTextColor.GRAY).append(Component.text(reason, NamedTextColor.WHITE)));
            player.sendMessage(Component.text("Expires in: ", NamedTextColor.GRAY).append(Component.text(remainingTime, NamedTextColor.WHITE)));
            player.sendMessage(Component.text("--------------------------------", NamedTextColor.RED));
        }
    }
}