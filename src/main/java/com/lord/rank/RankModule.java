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
            Rank defaultRank = new Rank("Default");
            defaultRank.setPrefix("[Player]");
            defaultRank.setPriority(1);

            rankRepository.save(defaultRank);
        }

        if (rankRepository.findByName("mod").isEmpty()) {
            Rank defaultRank = new Rank("Mod");
            defaultRank.setPrefix("[Mod]");
            defaultRank.setPriority(100);

            rankRepository.save(defaultRank);
        }

        if (rankRepository.findByName("admin").isEmpty()) {
            Rank defaultRank = new Rank("Admin");
            defaultRank.setPrefix("[Admin]");
            defaultRank.setPriority(200);

            rankRepository.save(defaultRank);
        }

        if (rankRepository.findByName("owner").isEmpty()) {
            Rank defaultRank = new Rank("Owner");
            defaultRank.setPrefix("[Owner]");
            defaultRank.setPriority(300);

            rankRepository.save(defaultRank);
        }
    }
}