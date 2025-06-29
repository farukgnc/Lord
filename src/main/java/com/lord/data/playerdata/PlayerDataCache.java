package com.lord.data.playerdata;

import com.lord.data.CachedData;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataCache {

    private final Map<UUID, CachedData> cache = new ConcurrentHashMap<>();

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
}