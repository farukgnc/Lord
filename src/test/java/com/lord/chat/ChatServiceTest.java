package com.lord.chat;

import com.lord.Lord;
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

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.register(Lord.class, Mockito.mock(Lord.class));
        chatService = new ChatService(registry);
    }

    @Test
    void shouldBlockSpamByCooldownAndDuplicateMessage() {
        Player player = Mockito.mock(Player.class);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());

        boolean first = chatService.canPlayerChat(player, "hello", 1.0);
        boolean secondImmediate = chatService.canPlayerChat(player, "hello", 1.0);

        assertThat(first).isTrue();
        assertThat(secondImmediate).isFalse();
    }

    @Test
    void shouldHandlePromptInput() {
        Player player = Mockito.mock(Player.class);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());

        AsyncChatEvent event = Mockito.mock(AsyncChatEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.originalMessage()).thenReturn(Component.text("typed value"));

        AtomicReference<String> captured = new AtomicReference<>();
        chatService.prompt(player, captured::set);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            BukkitScheduler scheduler = Mockito.mock(BukkitScheduler.class);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable task = invocation.getArgument(1);
                task.run();
                return Mockito.mock(BukkitTask.class);
            });

            boolean handled = chatService.handleInput(event);

            assertThat(handled).isTrue();
            assertThat(captured.get()).isEqualTo("typed value");
        }
    }

    @Test
    void shouldReturnFalseWhenNoPendingInput() {
        Player player = Mockito.mock(Player.class);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());

        AsyncChatEvent event = Mockito.mock(AsyncChatEvent.class);
        when(event.getPlayer()).thenReturn(player);

        assertThat(chatService.handleInput(event)).isFalse();
    }
}
