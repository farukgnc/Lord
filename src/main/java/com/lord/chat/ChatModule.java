package com.lord.chat;

import com.lord.Lord;
import com.lord.module.Module;
import com.lord.services.ServiceRegistry;
import org.bukkit.Bukkit;

public final class ChatModule implements Module {

    private final ServiceRegistry registry;
    private final Lord plugin;

    public ChatModule(ServiceRegistry registry) {
        this.registry = registry;
        this.plugin = registry.get(Lord.class);
    }

    @Override
    public void enable() {
        ChatService chatService = new ChatService(this.registry);
        this.registry.register(ChatService.class, chatService);

        this.registry.register(ChatModule.class, this);

        Bukkit.getPluginManager().registerEvents(new ChatListener(this.registry), this.plugin);

        System.out.println("[" + getName() + "] module has been enabled.");
    }

    @Override
    public void disable() {
        this.registry.unregister(ChatService.class);
        this.registry.unregister(ChatModule.class);
    }

    @Override
    public String getName() {
        return "Chat";
    }
}