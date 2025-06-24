package com.lord.modules.impl;

import com.lord.data.ranks.Rank;
import com.lord.modules.Module;
import com.lord.repositories.RankRepository;
import com.lord.services.ServiceRegistry;

public final class RankModule implements Module {

    private final ServiceRegistry registry;

    public RankModule(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void enable() {
        System.out.println("[" + getName() + "] Creating default ranks...");
        createDefaultRanks();
        System.out.println("[" + getName() + "] Default ranks have been loaded.");
    }

    @Override
    public void disable() {
    }

    @Override
    public String getName() {
        return "Rank";
    }

    private void createDefaultRanks() {
        RankRepository rankRepository = this.registry.get(RankRepository.class);

        if (rankRepository.findByName("default").isEmpty()) {
            Rank defaultRank = new Rank("default");
            defaultRank.setPrefix("[Player]");
            defaultRank.setPriority(1);

            rankRepository.save(defaultRank);
        }
    }
}