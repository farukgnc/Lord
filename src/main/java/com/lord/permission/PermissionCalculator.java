package com.lord.permission;

import com.lord.data.cached.CachedData;
import com.lord.data.cached.MetaData;
import com.lord.data.cached.PermissionData;
import com.lord.data.grants.Grant;
import com.lord.data.ranks.Rank;
import com.lord.repositories.GrantRepository;
import com.lord.repositories.RankRepository;
import com.lord.services.ServiceRegistry;

import java.util.*;
import java.util.stream.Collectors;

public final class PermissionCalculator {

    private final GrantRepository grantRepository;
    private final RankRepository rankRepository;

    public PermissionCalculator(ServiceRegistry registry) {
        this.grantRepository = registry.get(GrantRepository.class);
        this.rankRepository = registry.get(RankRepository.class);
    }

    public CachedData calculate(UUID playerUuid) {
        // 1. Oyuncunun aktif grant'lerinden başlangıç rütbelerini bul.
        Set<Rank> initialRanks = this.grantRepository.findByPlayer(playerUuid)
                .stream()
                .filter(Grant::isActive)
                .map(grant -> this.rankRepository.findByName(grant.getRankName()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        // 2. Bu rütbelerden yola çıkarak tüm miras ağacını çöz ve önceliğe göre sırala.
        List<Rank> sortedInheritedRanks = resolveInheritance(initialRanks);

        // 3. Sıralı listeden nihai izinleri ve meta verileri hesapla.
        Map<String, Boolean> permissions = new HashMap<>();
        String prefix = null;
        String suffix = null;

        for (Rank rank : sortedInheritedRanks) {
            // İzinleri topla. Eğer bir izin zaten eklenmişse (daha yüksek öncelikli bir rütbeden gelmişse), es geç.
            for (String permission : rank.getPermissions()) {
                permissions.putIfAbsent(permission.toLowerCase(), true);
            }

            // İlk bulduğun prefix'i al (çünkü liste en yüksek öncelikliden başlıyor).
            if (prefix == null && rank.getPrefix() != null) {
                prefix = rank.getPrefix();
            }

            // İlk bulduğun suffix'i al.
            if (suffix == null && rank.getSuffix() != null) {
                suffix = rank.getSuffix();
            }
        }

        // 4. Hesaplanan verilerden yeni önbellek nesnelerini oluştur.
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