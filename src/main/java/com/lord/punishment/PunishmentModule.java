package com.lord.punishment;

import com.lord.Lord;
import com.lord.module.Module;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.services.ServiceRegistry;
import org.bukkit.Bukkit;

public final class PunishmentModule implements Module {

    private final ServiceRegistry registry;
    private final PunishmentRepository punishmentRepository;
    private final PunishmentCacheService punishmentCacheService;
    private final Lord plugin;
    private PunishmentService punishmentService;

    public PunishmentModule(ServiceRegistry registry) {
        this.registry = registry;
        this.plugin = registry.get(Lord.class);
        this.punishmentRepository = registry.get(PunishmentRepository.class);
        this.punishmentCacheService = registry.get(PunishmentCacheService.class);
    }

    @Override
    public void enable() {
        // Initialize and register the punishment service
        this.punishmentService = new PunishmentService(registry);
        this.registry.register(PunishmentService.class, punishmentService);
        
        this.registry.register(PunishmentModule.class, this);

        Bukkit.getPluginManager().registerEvents(new PunishmentListener(this.registry), this.plugin);

        System.out.println("[" + getName() + "] module has been enabled and listeners are registered.");
    }

    @Override
    public void disable() {
        this.registry.unregister(PunishmentModule.class);
        this.registry.unregister(PunishmentService.class);
    }

    @Override
    public String getName() {
        return "Punishment";
    }
}