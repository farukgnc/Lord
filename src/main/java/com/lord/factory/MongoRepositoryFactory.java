package com.lord.data.factory;

import com.lord.Lord;
import com.lord.database.Mongo;
import com.lord.factory.RepositoryFactory;
import com.lord.grant.repositories.GrantRepository;
import com.lord.grant.repositories.impl.InMemoryGrantRepository;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.punishment.repositories.impl.InMemoryPunishmentRepository;
import com.lord.rank.Rank;
import com.lord.rank.repositories.RankRepository;
import com.lord.rank.repositories.impl.InMemoryRankRepository;
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
    public CompletableFuture<Boolean> setup() {
        mongo = new Mongo(registry);
        registry.register(Mongo.class, mongo);
        return mongo.connect();
    }

    @Override
    public void createRepositories() {
        registry.register(RankRepository.class, new InMemoryRankRepository());
        registry.register(GrantRepository.class, new InMemoryGrantRepository());
        registry.register(PunishmentRepository.class, new InMemoryPunishmentRepository());
    }

    @Override
    public void close() {
        if (this.mongo != null) {
            this.mongo.disconnect();
        }
    }
}