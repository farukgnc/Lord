package com.lord.punishment;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.lord.punishment.enums.PunishmentStatusFilter;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.services.ServiceRegistry;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class PunishmentCacheService {

    private final PunishmentRepository punishmentRepository;
    private final LoadingCache<UUID, CompletableFuture<List<Punishment>>> punishmentCache;

    public PunishmentCacheService(ServiceRegistry registry) {
        this.punishmentRepository = registry.get(PunishmentRepository.class);

        this.punishmentCache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public CompletableFuture<List<Punishment>> load(UUID playerUuid) {
                        // Önbellekte veri bulunamadığında, repository'nin yeni ve esnek
                        // findWithFilters metodunu kullanarak oyuncunun TÜM cezalarını getir.
                        return punishmentRepository.findWithFilters(playerUuid, PunishmentStatusFilter.ALL, null);
                    }
                });
    }

    /**
     * Bir oyuncunun tüm ceza geçmişini önbellekten (veya gerekirse veritabanından) getirir.
     */
    public CompletableFuture<List<Punishment>> getPunishments(UUID playerUuid) {
        return this.punishmentCache.getUnchecked(playerUuid);
    }

    /**
     * Bir oyuncunun önbelleğini manuel olarak geçersiz kılar.
     */
    public void invalidate(UUID playerUuid) {
        this.punishmentCache.invalidate(playerUuid);
    }
}