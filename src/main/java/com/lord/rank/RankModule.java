package com.lord.rank;

import com.lord.module.Module;
import com.lord.rank.exceptions.RankAlreadyExistsException;
import com.lord.rank.repositories.RankRepository;
import com.lord.services.ServiceRegistry;

import java.util.Set;

public final class RankModule implements Module {

    private final ServiceRegistry registry;
    private final RankRepository rankRepository;

    public RankModule(ServiceRegistry registry) {
        this.registry = registry;
        this.rankRepository = registry.get(RankRepository.class);
    }

    @Override
    public void enable() {
        System.out.println("[" + getName() + "] Creating default ranks...");
        createDefaultRanks();
        System.out.println("[" + getName() + "] Default ranks have been loaded.");

        this.registry.register(RankModule.class, this);
    }

    @Override
    public void disable() {
        this.registry.unregister(RankModule.class);
    }

    @Override
    public String getName() {
        return "Rank";
    }

    private void createDefaultRanks() {
        try {
            // createRank metodu zaten var olup olmadığını kontrol ettiği için,
            // bizim burada tekrar if bloğu ile kontrol etmemize gerek kalmadı.
            createRank("default", 1, "[Player]", null, Set.of());
            createRank("mod", 100, "<gray>[Mod]", null, Set.of("default"));
            createRank("admin", 200, "<red>[Admin]", null, Set.of("mod"));
            createRank("owner", 999, "<dark_red>[Owner]", null, Set.of("admin"));
        } catch (RankAlreadyExistsException e) {
            // Sunucu yeniden başlatıldığında veya reload atıldığında bu rütbeler zaten var olacaktır.
            // Bu beklenen bir durum olduğu için, bu hatayı görmezden geliyoruz ve konsolu kirletmiyoruz.
        }
    }

    public Rank createRank(String name, int priority, String prefix, String suffix, Set<String> parents) throws RankAlreadyExistsException {
        if (this.rankRepository.findByName(name).isPresent()) {
            throw new RankAlreadyExistsException("A rank with name " + name + " already exists.");
        }

        Rank newRank = new Rank(name);
        newRank.setPriority(priority);
        if (prefix != null) newRank.setPrefix(prefix);
        if (suffix != null) newRank.setSuffix(suffix);
        if (parents != null) newRank.getParentRankNames().addAll(parents);

        this.rankRepository.save(newRank);
        return newRank;
    }
}