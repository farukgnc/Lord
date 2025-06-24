package com.lord.modules.impl;

import com.lord.Lord;
import com.lord.command.CommandManager;
import com.lord.command.impl.PingCommand;
import com.lord.command.impl.grant.UngrantCommand;
import com.lord.command.impl.rank.RankCommand;
import com.lord.command.impl.grant.GrantCommand;
import com.lord.command.impl.grant.GrantsCommand;
import com.lord.modules.Module;
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
        commandManager.registerCommand(new UngrantCommand(this.registry));
    }

    @Override
    public void disable() {
    }

    @Override
    public String getName() {
        return "Commands";
    }
}