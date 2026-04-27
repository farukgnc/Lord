package com.lord.rank.repositories.impl;

import com.lord.rank.Rank;
import com.lord.rank.repositories.RankRepository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryRankRepository implements RankRepository {

    private final Map<String, Rank> ranks = new ConcurrentHashMap<>();

    @Override
    public Optional<Rank> findByName(String name) {
        Rank rank = this.ranks.get(name.toLowerCase());
        return Optional.ofNullable(rank);
    }

    @Override
    public Set<Rank> getAllRanks() {
        return Set.copyOf(this.ranks.values());
    }

    @Override
    public CompletableFuture<Set<Rank>> loadAllRanks() {
        return CompletableFuture.completedFuture(getAllRanks());
    }

    @Override
    public CompletableFuture<Boolean> save(Rank rank) {
        this.ranks.put(rank.getName().toLowerCase(), rank);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> delete(String name) {
        this.ranks.remove(name.toLowerCase());
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return CompletableFuture.completedFuture(ranks.isEmpty());
    }
}