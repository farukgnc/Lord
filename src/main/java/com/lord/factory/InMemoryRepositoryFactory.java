package com.lord.factory;

import com.lord.database.Mongo;
import com.lord.grant.repositories.GrantRepository;
import com.lord.grant.repositories.impl.InMemoryGrantRepository;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.punishment.repositories.impl.InMemoryPunishmentRepository;
import com.lord.rank.repositories.RankRepository;
import com.lord.rank.repositories.impl.InMemoryRankRepository;
import com.lord.service.ServiceRegistry;

import java.util.concurrent.CompletableFuture;

public class InMemoryRepositoryFactory implements RepositoryFactory {

    private Mongo mongo;
    private final ServiceRegistry registry;

    public InMemoryRepositoryFactory(ServiceRegistry registry) {
        this.registry = registry;

        registry.register(RepositoryFactory.class, this);
    }

    @Override
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Void> createRepositories() {
        registry.register(RankRepository.class, new InMemoryRankRepository());
        registry.register(GrantRepository.class, new InMemoryGrantRepository());
        registry.register(PunishmentRepository.class, new InMemoryPunishmentRepository());

        return null;
    }

    @Override
    public void disconnect() {
    }
}