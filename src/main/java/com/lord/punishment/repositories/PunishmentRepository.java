package com.lord.punishment.repositories;

import com.lord.punishment.Punishment;
import com.lord.punishment.enums.PunishmentStatusFilter;
import com.lord.punishment.enums.PunishmentType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PunishmentRepository {

    /**
     * Bir oyuncunun cezalarını, belirli filtrelere göre asenkron olarak bulur.
     * @param playerUuid Oyuncunun UUID'si.
     * @param statusFilter Durum filtresi (ALL, ACTIVE, INACTIVE).
     * @param typeFilter Ceza türü filtresi (null ise tüm türler).
     * @return Filtrelenmiş ve tarihe göre sıralanmış ceza listesini içeren bir CompletableFuture.
     */
    CompletableFuture<List<Punishment>> findWithFilters(UUID playerUuid, PunishmentStatusFilter statusFilter, @Nullable PunishmentType typeFilter);

    // Diğer metotlar (save, delete) aynı kalır.
    CompletableFuture<Void> save(Punishment punishment);
    CompletableFuture<Void> delete(Punishment punishment);
}