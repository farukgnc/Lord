package com.lord.punishment.repositories.impl;

import com.lord.punishment.Punishment;
import com.lord.punishment.PunishmentStatusFilter;
import com.lord.punishment.PunishmentType;
import com.lord.punishment.repositories.PunishmentRepository;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class InMemoryPunishmentRepository implements PunishmentRepository {

    private final Map<UUID, Punishment> punishmentsById = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<List<Punishment>> findWithFilters(UUID playerUuid, PunishmentStatusFilter statusFilter, @Nullable PunishmentType typeFilter) {
        return CompletableFuture.supplyAsync(() -> punishmentsById.values().stream()
                .filter(p -> p.getPunishedUuid().equals(playerUuid))
                .filter(p -> typeFilter == null || p.getType() == typeFilter)
                .filter(p -> {
                    switch (statusFilter) {
                        case ACTIVE: return p.isActive();
                        case INACTIVE: return !p.isActive();
                        case ALL:
                        default: return true;
                    }
                })
                .sorted(Comparator.comparing(Punishment::getCreationTime).reversed())
                .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Void> save(Punishment punishment) {
        punishmentsById.put(punishment.getUniqueId(), punishment);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> delete(Punishment punishment) {
        punishmentsById.remove(punishment.getUniqueId());
        return CompletableFuture.completedFuture(null);
    }
}