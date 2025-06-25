package com.lord.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class CachedData {

    private final PermissionData permissionData;
    private final MetaData metaData;

}