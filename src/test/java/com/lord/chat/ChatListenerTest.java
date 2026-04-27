package com.lord.chat;

import com.lord.Lord;
import com.lord.config.impl.MainConfig;
import com.lord.data.CachedData;
import com.lord.data.MetaData;
import com.lord.data.PermissionData;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.service.ServiceRegistry;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatListenerTest {

    private ServiceRegistry registry;
    private ChatService chatService;
    private PlayerDataCache playerDataCache;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
        chatService = Mockito.mock(ChatService.class);
        playerDataCache = Mockito.mock(PlayerDataCache.class);

        MainConfig mainConfig = Mockito.mock(MainConfig.class);
        when(mainConfig.getChatFormat()).thenReturn("<prefix><player_name>: <message>");
        when(mainConfig.isChatSpamFilterEnabled()).thenReturn(true);
        when(mainConfig.getChatSpamFilterCooldown()).thenReturn(3.0);

        registry.register(Lord.class, Mockito.mock(Lord.class));
        registry.register(ChatService.class, chatService);
        registry.register(PlayerDataCache.class, playerDataCache);
        registry.register(MainConfig.class, mainConfig);
    }

    @Test
    void shouldReturnImmediatelyWhenInputHandledByChatService() {
        ChatListener listener = new ChatListener(registry);

        AsyncChatEvent event = Mockito.mock(AsyncChatEvent.class);
        Player player = Mockito.mock(Player.class);
        when(event.getPlayer()).thenReturn(player);
        when(chatService.handleInput(event)).thenReturn(true);

        listener.onPlayerChat(event);

        verify(chatService, never()).canPlayerChat(any(), any(), any(Double.class));
        verify(event, never()).setCancelled(true);
    }

    @Test
    void shouldCancelWhenChatDisabledAndNoBypass() {
        ChatListener listener = new ChatListener(registry);

        AsyncChatEvent event = Mockito.mock(AsyncChatEvent.class);
        Player player = Mockito.mock(Player.class);

        when(event.getPlayer()).thenReturn(player);
        when(event.originalMessage()).thenReturn(Component.text("hello"));
        when(chatService.handleInput(event)).thenReturn(false);
        when(chatService.isChatEnabled()).thenReturn(false);
        when(player.hasPermission("lord.chat.bypass")).thenReturn(false);

        listener.onPlayerChat(event);

        verify(event, times(1)).setCancelled(true);
        verify(player, times(1)).sendMessage(any(Component.class));
    }

    @Test
    void shouldCancelWhenSpamFilterBlocksMessage() {
        ChatListener listener = new ChatListener(registry);

        AsyncChatEvent event = Mockito.mock(AsyncChatEvent.class);
        Player player = Mockito.mock(Player.class);

        when(event.getPlayer()).thenReturn(player);
        when(event.originalMessage()).thenReturn(Component.text("hello"));
        when(chatService.handleInput(event)).thenReturn(false);
        when(chatService.isChatEnabled()).thenReturn(true);
        when(player.hasPermission("lord.chat.spam.bypass")).thenReturn(false);
        when(chatService.canPlayerChat(player, "hello", 3.0)).thenReturn(false);

        listener.onPlayerChat(event);

        verify(event, times(1)).setCancelled(true);
        verify(player, times(1)).sendMessage(any(Component.class));
    }

    @Test
    void shouldBroadcastFormattedMessageWhenAllowed() {
        ChatListener listener = new ChatListener(registry);

        AsyncChatEvent event = Mockito.mock(AsyncChatEvent.class);
        Player player = Mockito.mock(Player.class);
        UUID uuid = UUID.randomUUID();

        CachedData cachedData = new CachedData(
                new PermissionData(Map.of("x", true)),
                new MetaData("[A]", "[S]", "admin")
        );

        when(event.getPlayer()).thenReturn(player);
        when(event.originalMessage()).thenReturn(Component.text("hello"));
        when(chatService.handleInput(event)).thenReturn(false);
        when(chatService.isChatEnabled()).thenReturn(true);
        when(player.hasPermission("lord.chat.spam.bypass")).thenReturn(false);
        when(chatService.canPlayerChat(player, "hello", 3.0)).thenReturn(true);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("Faruk");
        when(playerDataCache.getPlayerData(uuid)).thenReturn(Optional.of(cachedData));

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            BukkitScheduler scheduler = Mockito.mock(BukkitScheduler.class);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.broadcast(any(Component.class))).thenReturn(0);

            when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable task = invocation.getArgument(1);
                task.run();
                return Mockito.mock(BukkitTask.class);
            });

            listener.onPlayerChat(event);

            verify(event, times(1)).setCancelled(true);
            bukkit.verify(() -> Bukkit.broadcast(any(Component.class)), times(1));
        }
    }
}
