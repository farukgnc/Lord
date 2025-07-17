package com.lord.data.playerdata;

import com.lord.data.CachedData;
import com.lord.grant.GrantCacheService;
import com.lord.service.ServiceRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataCache {

    private final Map<UUID, CachedData> cache = new ConcurrentHashMap<>();
    private final GrantCacheService grantCacheService;
    private final PlayerDataCalculator calculator;

    public PlayerDataCache(ServiceRegistry registry) {
        this.grantCacheService = registry.get(GrantCacheService.class);
        this.calculator = new PlayerDataCalculator(registry);
    }

    /**
     * Bir oyuncunun önceden yüklenmiş ve hesaplanmış verilerini önbellekten alır.
     * Bu metot senkrondur ve herhangi bir hesaplama veya veritabanı işlemi tetiklemez.
     * @param playerUuid Oyuncunun UUID'si.
     * @return Oyuncunun CachedData'sını içeren bir Optional, önbellekte yoksa boş.
     */
    public Optional<CachedData> getPlayerData(UUID playerUuid) {
        return Optional.ofNullable(this.cache.get(playerUuid));
    }

    /**
     * Bir oyuncu için önceden hesaplanmış veriyi önbelleğe alır.
     * Bu metot, genellikle oyuncu giriş yaptığında PlayerDataListener tarafından çağrılır.
     * @param playerUuid Oyuncunun UUID'si.
     * @param data Önbelleğe alınacak, önceden hesaplanmış veri.
     */
    public void cacheData(UUID playerUuid, CachedData data) {
        this.cache.put(playerUuid, data);
    }

    /**
     * Bir oyuncunun verilerini önbellekten kaldırır.
     * Bu, oyuncu sunucudan çıktığında veya grant'ları değiştiğinde çağrılır.
     * @param playerUuid Geçersiz kılınacak oyuncunun UUID'si.
     */
    public void invalidate(UUID playerUuid) {
        this.cache.remove(playerUuid);
    }

    /**
     * --- YENİ VE KRİTİK METOT ---
     * Bir oyuncunun verilerini veritabanından çekerek önbelleği asenkron olarak yeniler.
     * Bu, bir oyuncunun grant'ları veya cezaları değiştiğinde çağrılır.
     * @param playerUuid Verileri yenilenecek oyuncunun UUID'si.
     * @return Yenileme işlemi tamamlandığında sona erecek bir CompletableFuture.
     */
    public CompletableFuture<Void> refreshPlayerData(UUID playerUuid) {
        // 1. Oyuncunun en güncel grant'larını önbellekten (veya gerekirse veritabanından) çek.
        return this.grantCacheService.getGrants(playerUuid)
                .thenAcceptAsync(grants -> {
                    // 2. Gelen grant'larla veriyi yeniden hesapla.
                    CachedData newCachedData = this.calculator.calculate(grants);
                    // 3. Hesaplanan taze veriyi önbelleğe koy.
                    this.cacheData(playerUuid, newCachedData);
                });
    }

    // ne kadar verimli bilmiyorum
    public CompletableFuture<Void> refreshPlayerDataCache() {
        return CompletableFuture.runAsync(() -> {
            for (UUID uuid: cache.keySet()) {
                refreshPlayerData(uuid);
            }
        });
    }
}