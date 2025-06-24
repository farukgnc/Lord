package com.lord.data.cached;

import lombok.Getter;
import java.util.Map;

@Getter
public final class PermissionData {

    private final Map<String, Boolean> permissions;

    public PermissionData(Map<String, Boolean> permissions) {
        this.permissions = Map.copyOf(permissions);
    }

    public boolean hasPermission(String node) {
        // TODO: Implement full logic with wildcards.
        return this.permissions.getOrDefault(node.toLowerCase(), false);
    }
}