package com.lord.data.playerdata;

import com.lord.data.CachedData;
import com.lord.data.MetaData;
import com.lord.data.PermissionData;
import com.lord.grant.Grant;
import com.lord.rank.Rank;
import com.lord.rank.repositories.RankRepository;
import com.lord.service.ServiceRegistry;

import java.util.*;
import java.util.stream.Collectors;

public final class PlayerDataCalculator {

    private final RankRepository rankRepository;

    public PlayerDataCalculator(ServiceRegistry registry) {
        // Sadece rank'ları çözümlemek için RankRepository'e ihtiyacı var.
        // RankRepository'nin okuma metotlarının hızlı (bellekten) olduğu varsayılır.
        this.rankRepository = registry.get(RankRepository.class);
    }

    /**
     * Önceden getirilmiş bir grant setine dayanarak bir oyuncunun tüm önbellek verilerini hesaplar.
     * @param grants Oyuncunun veritabanından çekilmiş olan grant'ları.
     * @return Hesaplanan CachedData nesnesi.
     */
    public CachedData calculate(Set<Grant> grants) {
        Set<Rank> initialRanks = grants.stream()
                .filter(Grant::isActive)
                .map(grant -> this.rankRepository.findByName(grant.getRankName()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        List<Rank> sortedInheritedRanks = resolveInheritance(initialRanks);

        Map<String, Boolean> permissions = new HashMap<>();
        String prefix = null;
        String suffix = null;

        for (Rank rank : sortedInheritedRanks) {
            for (String permission : rank.getPermissions()) {
                boolean value = true;
                String node = permission.toLowerCase();
                if (node.startsWith("-")) {
                    node = node.substring(1);
                    value = false;
                }
                permissions.putIfAbsent(node, value);
            }

            if (prefix == null && rank.getPrefix() != null) {
                prefix = rank.getPrefix();
            }

            if (suffix == null && rank.getSuffix() != null) {
                suffix = rank.getSuffix();
            }
        }

        PermissionData permissionData = new PermissionData(permissions);
        MetaData metaData = new MetaData(prefix, suffix, sortedInheritedRanks.isEmpty() ? null : sortedInheritedRanks.get(0).getName());

        // Hesaplanan verilerle birlikte, bu hesaba kaynak olan orijinal grant'ları da sakla.
        return new CachedData(permissionData, metaData);
    }

    private List<Rank> resolveInheritance(Set<Rank> initialRanks) {
        Map<String, Rank> resolvedRanks = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Deque<Rank> stack = new ArrayDeque<>(initialRanks);

        while (!stack.isEmpty()) {
            Rank current = stack.pop();

            if (!visited.add(current.getName().toLowerCase())) {
                continue;
            }

            resolvedRanks.put(current.getName().toLowerCase(), current);

            for (String parentName : current.getParentRankNames()) {
                this.rankRepository.findByName(parentName)
                        .ifPresent(stack::push);
            }
        }

        return resolvedRanks.values().stream()
                .sorted(Comparator.comparingInt(Rank::getPriority).reversed())
                .collect(Collectors.toList());
    }
}