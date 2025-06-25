package com.lord.data;

import lombok.Getter;
import java.util.Map;

@Getter
public final class PermissionData {

    private final Map<String, Boolean> permissions;

    public PermissionData(Map<String, Boolean> permissions) {
        this.permissions = Map.copyOf(permissions);
    }

    public boolean hasPermission(String node) {
        String permission = node.toLowerCase();

        // 1. En spesifik kural: Direkt eşleşme var mı?
        // Bu, hem "essentials.fly" -> true hem de "essentials.fly" -> false (negatif izin) durumlarını yakalar.
        if (this.permissions.containsKey(permission)) {
            return this.permissions.get(permission);
        }

        // 2. Wildcard kontrolü: Spesifikten genele doğru kontrol et.
        String[] parts = permission.split("\\.");
        StringBuilder parent = new StringBuilder();

        // "a.b.c" izni için önce "a.b.*", sonra "a.*" kontrolü yapılır.
        for (int i = 0; i < parts.length - 1; i++) {
            parent.append(parts[i]).append(".");

            String wildcard = parent + "*";
            if (this.permissions.containsKey(wildcard)) {
                return this.permissions.get(wildcard);
            }
        }

        // 3. En genel kural: Global wildcard "*" var mı?
        // Bu, en düşük öncelikli kuraldır.
        return this.permissions.getOrDefault("*", false);
    }
}