package com.lord.rank;

import com.lord.module.Module;
import com.lord.rank.repositories.RankRepository;
import com.lord.services.ServiceRegistry;

public final class RankModule implements Module {

    private final ServiceRegistry registry;
    private final RankRepository rankRepository;
    private RankService rankService;

    public RankModule(ServiceRegistry registry) {
        this.registry = registry;
        this.rankRepository = registry.get(RankRepository.class);
    }

    @Override
    public void enable() {
        System.out.println("[" + getName() + "] module has been enabled.");
        
        // Initialize and register the rank service
        this.rankService = new RankService(registry);
        this.registry.register(RankService.class, rankService);

        // Check if database is empty and create default ranks if needed
        this.rankService.isEmpty().thenAccept(empty -> {
            if (empty) {
                System.out.println("[" + getName() + "] No ranks found in data source, creating default ranks...");
                rankService.createDefaultRanks();
            } else {
                System.out.println("[" + getName() + "] Ranks were loaded successfully from data source.");
            }
        });

        this.registry.register(RankModule.class, this);
    }

    @Override
    public void disable() {
        this.registry.unregister(RankModule.class);
        this.registry.unregister(RankService.class);
    }

    @Override
    public String getName() {
        return "Rank";
    }
}