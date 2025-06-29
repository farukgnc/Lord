package com.lord.command;

import com.lord.Lord;
import com.lord.command.impl.PingCommand;
import com.lord.punishment.commands.*;
import com.lord.rank.commands.RankCommand;
import com.lord.grant.commands.GrantCommand;
import com.lord.grant.commands.GrantsCommand;
import com.lord.module.Module;
import com.lord.services.ServiceRegistry;

public final class CommandModule implements Module {

    private final ServiceRegistry registry;

    public CommandModule(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void enable() {
        Lord plugin = this.registry.get(Lord.class);

        CommandManager commandManager = new CommandManager(plugin);

        commandManager.registerCommand(new PingCommand());

        commandManager.registerCommand(new RankCommand(this.registry));
        commandManager.registerCommand(new GrantCommand(this.registry));
        commandManager.registerCommand(new GrantsCommand(this.registry));

        commandManager.registerCommand(new BanCommand(this.registry));
        commandManager.registerCommand(new MuteCommand(this.registry));
        commandManager.registerCommand(new KickCommand(this.registry));
        commandManager.registerCommand(new WarnCommand(this.registry));
        commandManager.registerCommand(new UnbanCommand(this.registry));
        commandManager.registerCommand(new UnmuteCommand(this.registry));
        commandManager.registerCommand(new PunishmentsCommand(this.registry));

        this.registry.register(CommandModule.class, this);
    }

    @Override
    public void disable() {
        this.registry.unregister(CommandModule.class);
    }

    @Override
    public String getName() {
        return "Commands";
    }
}