package com.lord.data.playerdata;

import com.lord.data.CachedData;
import com.lord.data.MetaData;
import com.lord.data.PermissionData;
import com.lord.grant.Grant;
import com.lord.rank.Rank;
import com.lord.grant.repositories.GrantRepository;
import com.lord.rank.repositories.RankRepository;
import com.lord.services.ServiceRegistry;

import java.util.*;
import java.util.stream.Collectors;

public final class PlayerDataCalculator {

    private final GrantRepository grantRepository;
    private final RankRepository rankRepository;

    public PlayerDataCalculator(ServiceRegistry registry) {
        this.grantRepository = registry.get(GrantRepository.class);
        this.rankRepository = registry.get(RankRepository.class);
    }

    public CachedData calculate(UUID playerUuid) {
        Set<Rank> initialRanks = this.grantRepository.findByPlayer(playerUuid)
                .stream()
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
                // --- GÜNCELLENEN KISIM BAŞLANGICI ---
                boolean value = true;
                String node = permission.toLowerCase();

                // Eğer izin '-' ile başlıyorsa, bu negatif bir izindir.
                if (node.startsWith("-")) {
                    node = node.substring(1); // '-' işaretini kaldır
                    value = false;
                }

                // İzin haritasına ekle. Eğer zaten varsa (daha yüksek öncelikli bir rütbeden) dokunma.
                permissions.putIfAbsent(node, value);
                // --- GÜNCELLENEN KISIM BİTİŞİ ---
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

        return new CachedData(permissionData, metaData);
    }

    private List<Rank> resolveInheritance(Set<Rank> initialRanks) {
        // Gezilen tüm rütbeleri ve isimlerini takip etmek için
        Map<String, Rank> resolvedRanks = new HashMap<>();
        // Döngüsel mirasları engellemek için ziyaret edilenleri işaretle
        Set<String> visited = new HashSet<>();
        // Gezinme için bir yığın (stack)
        Deque<Rank> stack = new ArrayDeque<>(initialRanks);

        while (!stack.isEmpty()) {
            Rank current = stack.pop();

            if (!visited.add(current.getName().toLowerCase())) {
                continue; // Bu rütbeyi zaten gezdik, döngüyü kır.
            }

            resolvedRanks.put(current.getName().toLowerCase(), current);

            // Bu rütbenin miras aldığı üst rütbeleri bul ve yığına ekle.
            for (String parentName : current.getParentRankNames()) {
                this.rankRepository.findByName(parentName)
                        .ifPresent(stack::push); // Eğer üst rütbe varsa yığına ekle.
            }
        }

        // Tüm bulunan rütbeleri öncelik sırasına göre yüksekten düşüğe doğru sırala.
        return resolvedRanks.values().stream()
                .sorted(Comparator.comparingInt(Rank::getPriority).reversed())
                .collect(Collectors.toList());
    }
}