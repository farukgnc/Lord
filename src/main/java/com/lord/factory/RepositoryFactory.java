package com.lord.factory;

import com.lord.grant.repositories.GrantRepository;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.rank.repositories.RankRepository;

import java.util.concurrent.CompletableFuture;

// Bu arayüz, tüm repository'lerimizi üreten bir fabrikanın kontratıdır.
public interface RepositoryFactory {

    CompletableFuture<Boolean> setup();

    /**
     * İlgili veritabanı türü için RankRepository örneğini oluşturur ve döndürür.
     */
    void createRankRepository();

    /**
     * İlgili veritabanı türü için GrantRepository örneğini oluşturur ve döndürür.
     */
    void createGrantRepository();

    /**
     * İlgili veritabanı türü için PunishmentRepository örneğini oluşturur ve döndürür.
     */
    void createPunishmentRepository();

    /**
     * Fabrikanın kullandığı tüm kaynakları (örn: veritabanı bağlantısı) kapatır.
     */
    void close();
}