package com.lord.chat;

import com.lord.Lord;
import com.lord.config.impl.MainConfig;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.service.ServiceRegistry;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class ChatListener implements Listener {

    private final Lord plugin;
    private final ChatService chatService;
    private final PlayerDataCache playerDataCache;
    private final String chatFormat;
    private final boolean spamFilterEnabled;
    private final double chatCooldown;

    public ChatListener(ServiceRegistry registry) {
        this.plugin = registry.get(Lord.class);
        this.chatService = registry.get(ChatService.class);
        this.playerDataCache = registry.get(PlayerDataCache.class);

        MainConfig config = registry.get(MainConfig.class);
        this.chatFormat = config.getChatFormat();
        this.spamFilterEnabled = config.isChatSpamFilterEnabled();
        this.chatCooldown = config.getChatSpamFilterCooldown();
    }

    // Olayın en son bizim tarafımızdan işlendiğinden emin olmak için önceliği MONITOR'e çekelim.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (chatService.handleInput(event)) return;

        Component messageComponent = event.originalMessage();
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(messageComponent);

        // --- Spam ve Chat kapalı kontrolleri ---
        if (!chatService.isChatEnabled() && !player.hasPermission("lord.chat.bypass")) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Chat is currently disabled."));
            event.setCancelled(true);
            return;
        }

        if (spamFilterEnabled && !player.hasPermission("lord.chat.spam.bypass")) {
            if (!chatService.canPlayerChat(player, plainMessage, chatCooldown)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Please do not spam and wait a moment."));
                event.setCancelled(true);
                return;
            }
        }

        // --- YENİ VE KESİN FORMATLAMA YÖNTEMİ ---

        // 1. Orijinal olayı tamamen iptal et.
        event.setCancelled(true);

        // 2. Oyuncunun verilerini al ve format için hazırla.
        playerDataCache.getPlayerData(player.getUniqueId()).ifPresent(cachedData -> {
            String prefix = cachedData.getMetaData().getPrefix() != null ? cachedData.getMetaData().getPrefix() + " " : "";
            String suffix = cachedData.getMetaData().getSuffix() != null ? " " + cachedData.getMetaData().getSuffix() : "";

            // 3. MiniMessage ile son mesaj Component'ini oluştur.
            Component formattedMessage = MiniMessage.miniMessage().deserialize(
                    chatFormat,
                    Placeholder.unparsed("prefix", prefix),
                    Placeholder.unparsed("player_name", player.getName()),
                    Placeholder.unparsed("suffix", suffix),
                    Placeholder.component("message", messageComponent)
            );

            // 4. Oluşturduğun bu son mesajı ana thread'de sunucudaki herkese gönder.
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcast(formattedMessage));
        });
    }
}