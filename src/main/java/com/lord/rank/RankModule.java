package com.lord.rank;

import com.lord.modules.Module;
import com.lord.rank.repositories.RankRepository;
import com.lord.services.ServiceRegistry;

public final class RankModule implements Module {

    private final RankRepository rankRepository;

    public RankModule(ServiceRegistry registry) {
        this.rankRepository = registry.get(RankRepository.class);
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
        if (rankRepository.findByName("default").isEmpty()) {
            Rank defaultRank = new Rank("default");
            defaultRank.setPrefix("[Player]");
            defaultRank.setPriority(1);

            rankRepository.save(defaultRank);
        }
    }
}