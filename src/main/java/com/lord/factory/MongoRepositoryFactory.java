package com.lord.factory;

import com.lord.database.Mongo;
import com.lord.grant.repositories.GrantRepository;
import com.lord.grant.repositories.impl.InMemoryGrantRepository;
import com.lord.grant.repositories.impl.MongoGrantRepository;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.punishment.repositories.impl.InMemoryPunishmentRepository;
import com.lord.rank.repositories.RankRepository;
import com.lord.rank.repositories.impl.MongoRankRepository;
import com.lord.services.GrantCacheService;
import com.lord.services.ServiceRegistry;

import java.util.concurrent.CompletableFuture;

public class MongoRepositoryFactory implements RepositoryFactory {

    private Mongo mongo;
    private final ServiceRegistry registry;

    public MongoRepositoryFactory(ServiceRegistry registry) {
        this.registry = registry;
        registry.register(RepositoryFactory.class, this);
    }

    @Override
    public CompletableFuture<Boolean> connect() {
        mongo = new Mongo(registry);
        registry.register(Mongo.class, mongo);
        return mongo.connect();
    }

    @Override
    public CompletableFuture<Void> createRepositories() {
        MongoRankRepository mongoRankRepository = new MongoRankRepository(registry);
        registry.register(RankRepository.class, mongoRankRepository);

        System.out.println("[Lord] Initializing RankRepository cache...");

        return mongoRankRepository.loadAllRanks().thenRun(() -> {
            registry.register(GrantRepository.class, new MongoGrantRepository(mongo.getDatabase()));

            GrantCacheService grantCacheService = new GrantCacheService(registry);
            registry.register(GrantCacheService.class, grantCacheService);

            registry.register(PunishmentRepository.class, new InMemoryPunishmentRepository());
        });
    }

    @Override
    public void disconnect() {
        if (this.mongo != null) {
            this.mongo.disconnect();
        }
    }
}