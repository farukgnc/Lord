package com.lord.data;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CachedMetaDataTest {

    @Test
    void shouldExposeCachedDataAndMetadata() {
        PermissionData permissionData = new PermissionData(Map.of("example.node", true));
        MetaData metaData = new MetaData("[X]", "[/X]", "default");
        CachedData cachedData = new CachedData(permissionData, metaData);

        assertThat(cachedData.getPermissionData().hasPermission("example.node")).isTrue();
        assertThat(cachedData.getMetaData().getPrefix()).isEqualTo("[X]");
        assertThat(cachedData.getMetaData().getSuffix()).isEqualTo("[/X]");
        assertThat(cachedData.getMetaData().getPrimaryRank()).isEqualTo("default");
    }
}
