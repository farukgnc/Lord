package com.lord.grant;

import com.lord.Lord;
import com.lord.grant.repositories.GrantRepository;
import com.lord.module.Module;
import com.lord.service.ServiceRegistry;

public final class GrantModule implements Module {

    private final ServiceRegistry registry;
    private GrantService grantService;

    public GrantModule(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void enable() {
        // Initialize and register the grant service
        this.grantService = new GrantService(registry);
        this.registry.register(GrantService.class, grantService);
        
        this.registry.register(GrantModule.class, this);
        System.out.println("[" + getName() + "] module has been enabled.");
    }

    @Override
    public void disable() {
        this.registry.unregister(GrantModule.class);
        this.registry.unregister(GrantService.class);
    }

    @Override
    public String getName() {
        return "Grant";
    }
}