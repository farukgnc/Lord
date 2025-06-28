package com.lord.factory;

import com.lord.grant.repositories.GrantRepository;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.rank.repositories.RankRepository;

import java.util.concurrent.CompletableFuture;

// Bu arayüz, tüm repository'lerimizi üreten bir fabrikanın kontratıdır.
public interface RepositoryFactory {

    CompletableFuture<Boolean> setup();

    void createRepositories();

    /**
     * Fabrikanın kullandığı tüm kaynakları (örn: veritabanı bağlantısı) kapatır.
     */
    void close();
}