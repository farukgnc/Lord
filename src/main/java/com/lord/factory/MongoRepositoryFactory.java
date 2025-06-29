package com.lord.factory;

import com.lord.database.Mongo;
import com.lord.grant.repositories.GrantRepository;
import com.lord.grant.repositories.impl.InMemoryGrantRepository;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.punishment.repositories.impl.InMemoryPunishmentRepository;
import com.lord.rank.repositories.RankRepository;
import com.lord.rank.repositories.impl.MongoRankRepository;
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
    public void createRepositories() {
        MongoRankRepository mongoRankRepository = new MongoRankRepository(registry);

        // 3. Repository'nin iç önbelleğini doldurmasını sağla ve bekle.
        System.out.println("[Lord] Initializing RankRepository cache...");
        mongoRankRepository.loadAllRanks().join();

        // 4. Oluşturulan ve içi doldurulan repository'yi sisteme kaydet.
        registry.register(RankRepository.class, mongoRankRepository);


        // --- Diğer Repository'ler ---
        // TODO: Gelecekte Grant ve Punishment için de Mongo repository'leri yazılacak.
        registry.register(GrantRepository.class, new InMemoryGrantRepository());
        registry.register(PunishmentRepository.class, new InMemoryPunishmentRepository());
    }

    @Override
    public void disconnect() {
        if (this.mongo != null) {
            this.mongo.disconnect();
        }
    }
}