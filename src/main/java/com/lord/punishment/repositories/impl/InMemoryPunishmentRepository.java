package com.lord.punishment.repositories.impl;

import com.lord.punishment.Punishment;
import com.lord.punishment.PunishmentType;
import com.lord.punishment.repositories.PunishmentRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class InMemoryPunishmentRepository implements PunishmentRepository {

    // Ana Depo: Tüm cezaları kendi ID'leri ile saklar.
    private final Map<UUID, Punishment> punishmentsById = new ConcurrentHashMap<>();

    // İndeks 1: Oyuncu UUID'sini, o oyuncuya ait tüm cezalara (geçmiş dahil) eşler.
    private final Map<UUID, Set<Punishment>> punishmentsByPlayer = new ConcurrentHashMap<>();

    @Override
    public Optional<Punishment> findById(UUID punishmentId) {
        return Optional.ofNullable(this.punishmentsById.get(punishmentId));
    }

    @Override
    public Set<Punishment> findByPlayer(UUID playerUuid) {
        return Collections.unmodifiableSet(this.punishmentsByPlayer.getOrDefault(playerUuid, Collections.emptySet()));
    }

    @Override
    public Set<Punishment> findActiveByType(UUID playerUuid, PunishmentType type) {
        // İndeks yerine, oyuncunun tüm cezalarını alıp, aktif ve doğru türde olanları filtreleyelim.
        // Bu, daha az karmaşık bir yapı sunar ve performans bu ölçekte yeterlidir.
        return this.punishmentsByPlayer.getOrDefault(playerUuid, Collections.emptySet())
                .stream()
                .filter(p -> p.getType() == type && p.isActive())
                .collect(Collectors.toSet());
    }

    @Override
    public void save(Punishment punishment) {
        // 1. Ana depoya ekle/güncelle.
        this.punishmentsById.put(punishment.getUniqueId(), punishment);

        // 2. Oyuncu indeksini güncelle.
        Set<Punishment> playerPunishments = this.punishmentsByPlayer.computeIfAbsent(punishment.getPunishedUuid(), k -> new HashSet<>());

        // Önce eski versiyonu (varsa) kaldırıp yenisini eklemek, güncelleme durumları için en güvenli yoldur.
        playerPunishments.remove(punishment); // remove() metodu doğru çalışmak için Punishment sınıfında equals/hashCode gerekir.
        playerPunishments.add(punishment);
    }

    @Override
    public void delete(Punishment punishment) {
        // 1. Ana depodan kaldır.
        this.punishmentsById.remove(punishment.getUniqueId());

        // 2. Oyuncu indeksinden kaldır.
        Set<Punishment> playerPunishments = this.punishmentsByPlayer.get(punishment.getPunishedUuid());
        if (playerPunishments != null) {
            playerPunishments.remove(punishment);

            if (playerPunishments.isEmpty()) {
                this.punishmentsByPlayer.remove(punishment.getPunishedUuid());
            }
        }
    }
}
