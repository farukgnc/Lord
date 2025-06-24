package com.lord.repositories.impl;

import com.lord.data.ranks.Rank;
import com.lord.repositories.RankRepository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    public void save(Rank rank) {
        this.ranks.put(rank.getName().toLowerCase(), rank);
    }

    @Override
    public void delete(String name) {
        this.ranks.remove(name.toLowerCase());
    }
}