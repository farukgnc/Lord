package com.lord.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class CommandContextTest {

    @Test
    void shouldExposeArgsAndLength() {
        CommandSender sender = Mockito.mock(CommandSender.class);
        CommandContext context = new CommandContext(sender, new String[]{"a", "b"});

        assertThat(context.length()).isEqualTo(2);
        assertThat(context.arg(0)).isEqualTo("a");
        assertThat(context.arg(1)).isEqualTo("b");
        assertThat(context.arg(2)).isNull();
        assertThat(context.sender()).isSameAs(sender);
    }

    @Test
    void shouldDetectPlayerSender() {
        Player player = Mockito.mock(Player.class);
        CommandContext context = new CommandContext(player, new String[0]);

        assertThat(context.isPlayer()).isTrue();
        assertThat(context.player()).isSameAs(player);
    }

    @Test
    void shouldReturnNullPlayerWhenSenderIsNotPlayer() {
        CommandSender sender = Mockito.mock(CommandSender.class);
        CommandContext context = new CommandContext(sender, new String[0]);

        assertThat(context.isPlayer()).isFalse();
        assertThat(context.player()).isNull();
    }
}
