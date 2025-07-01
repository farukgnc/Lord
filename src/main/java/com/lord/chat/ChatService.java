package com.lord.chat;

import com.lord.Lord;
import com.lord.services.ServiceRegistry;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatService {

    private final Lord plugin;

    @Getter @Setter
    private boolean chatEnabled = true;

    // Spam filtresi için verileri tutan haritalar
    private final Map<UUID, Long> lastMessageTimes = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessageContent = new ConcurrentHashMap<>();

    private final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();

    public ChatService(ServiceRegistry registry) {
        this.plugin = registry.get(Lord.class);
    }

    /**
     * Bir oyuncunun, spam filtresine göre sohbet edip edemeyeceğini kontrol eder.
     * @param player Kontrol edilecek oyuncu.
     * @param message Oyuncunun gönderdiği mesaj.
     * @param cooldownSeconds Mesajlar arası bekleme süresi (saniye).
     * @return Oyuncu sohbet edebilirse true.
     */
    public boolean canPlayerChat(Player player, String message, double cooldownSeconds) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Cooldown kontrolü
        long lastMessage = lastMessageTimes.getOrDefault(uuid, 0L);
        if (now - lastMessage < cooldownSeconds * 1000) {
            return false; // Cooldown aktif
        }

        // Tekrar eden mesaj kontrolü
        String lastContent = lastMessageContent.getOrDefault(uuid, "");
        if (lastContent.equalsIgnoreCase(message)) {
            return false; // Aynı mesajı tekrar gönderiyor
        }

        // Kontrollerden geçtiyse, son mesaj bilgilerini güncelle.
        lastMessageTimes.put(uuid, now);
        lastMessageContent.put(uuid, message);
        return true;
    }

    public void prompt(Player player, Consumer<String> onInput) {
        this.pendingInputs.put(player.getUniqueId(), onInput);
    }

    public boolean handleInput(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> onInput = this.pendingInputs.remove(player.getUniqueId());

        if (onInput == null) {
            return false;
        }

        event.setCancelled(true);

        // Oyuncunun sohbet eylemini ana sunucu thread'inde çalıştırmak daha güvenlidir.
        Bukkit.getScheduler().runTask(plugin, () -> {
            onInput.accept(PlainTextComponentSerializer.plainText().serialize(event.originalMessage()));
        });

        return true;
    }
}