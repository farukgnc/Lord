package com.lord.grant;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.lord.grant.repositories.GrantRepository;
import com.lord.service.ServiceRegistry;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class GrantCacheService {

    private final GrantRepository grantRepository;

    // Bu, Google Guava'nın akıllı önbelleğidir.
    private final LoadingCache<UUID, CompletableFuture<Set<Grant>>> grantCache;

    public GrantCacheService(ServiceRegistry registry) {
        this.grantRepository = registry.get(GrantRepository.class);

        // Önbelleği yapılandırıyoruz.
        this.grantCache = CacheBuilder.newBuilder()
                // Bir oyuncunun grant verisine 1 saat boyunca hiç erişilmezse,
                // o veriyi önbellekten otomatik olarak sil.
                .expireAfterAccess(1, TimeUnit.HOURS)
                // Bu, önbelleğin "beynidir".
                // Önbellekte bir veri bulunamadığında ne yapılacağını tanımlar.
                .build(new CacheLoader<>() {
                    @Override
                    public CompletableFuture<Set<Grant>> load(UUID playerUuid) {
                        // Eğer veri önbellekte yoksa, otomatik olarak veritabanından çek.
                        return grantRepository.findByPlayer(playerUuid);
                    }
                });
    }

    /**
     * Bir oyuncunun grant'larını önbellekten (veya gerekirse veritabanından) getirir.
     * @param playerUuid Oyuncunun UUID'si.
     * @return Grant setini içeren bir CompletableFuture.
     */
    public CompletableFuture<Set<Grant>> getGrants(UUID playerUuid) {
        // .get() metodu, anahtarı kullanarak veriyi önbellekten alır.
        // Eğer veri yoksa, yukarıda tanımladığımız CacheLoader'ı otomatik olarak tetikler.
        return this.grantCache.getUnchecked(playerUuid);
    }

    /**
     * Bir oyuncunun önbelleğini manuel olarak geçersiz kılar. (Oyuncu çıktığında kullanılır)
     * @param playerUuid Oyuncunun UUID'si.
     */
    public void invalidate(UUID playerUuid) {
        this.grantCache.invalidate(playerUuid);
    }
}