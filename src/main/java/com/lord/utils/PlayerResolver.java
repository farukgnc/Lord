package com.lord.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class PlayerResolver {

    // İsimden UUID'ye çözümleme için önbellek
    private static final Cache<String, UUID> nameToUuidCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    // UUID'den isme çözümleme için önbellek
    private static final Cache<UUID, String> uuidToNameCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    /**
     * Bir oyuncu isminden UUID'yi asenkron olarak çözer.
     * Önce online oyuncuları, sonra önbelleği, sonra Mojang API'sini kontrol eder.
     */
    public static CompletableFuture<Optional<UUID>> resolveUUID(String name) {
        Player onlinePlayer = Bukkit.getPlayerExact(name);
        if (onlinePlayer != null) {
            UUID uuid = onlinePlayer.getUniqueId();
            nameToUuidCache.put(name.toLowerCase(), uuid);
            uuidToNameCache.put(uuid, onlinePlayer.getName());
            return CompletableFuture.completedFuture(Optional.of(uuid));
        }

        UUID cachedUuid = nameToUuidCache.getIfPresent(name.toLowerCase());
        if (cachedUuid != null) {
            return CompletableFuture.completedFuture(Optional.of(cachedUuid));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                JSONObject data = (JSONObject) JSONValue.parseWithException(in);
                String realName = (String) data.get("name");
                String id = (String) data.get("id");
                UUID uuid = UUID.fromString(id.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));

                nameToUuidCache.put(name.toLowerCase(), uuid);
                uuidToNameCache.put(uuid, realName);
                return Optional.of(uuid);
            } catch (Exception e) {
                // Oyuncu bulunamadığında veya bir hata olduğunda loglamaya gerek yok.
                return Optional.empty();
            }
        });
    }

    /**
     * Bir UUID'den oyuncu ismini asenkron olarak çözer.
     * Önce online oyuncuları, sonra önbelleği, sonra Mojang API'sini kontrol eder.
     */
    public static CompletableFuture<Optional<String>> resolveName(UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            uuidToNameCache.put(uuid, onlinePlayer.getName());
            return CompletableFuture.completedFuture(Optional.of(onlinePlayer.getName()));
        }

        String cachedName = uuidToNameCache.getIfPresent(uuid);
        if (cachedName != null) {
            return CompletableFuture.completedFuture(Optional.of(cachedName));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", ""));
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                JSONObject data = (JSONObject) JSONValue.parseWithException(in);
                String name = (String) data.get("name");

                uuidToNameCache.put(uuid, name);
                nameToUuidCache.put(name.toLowerCase(), uuid);
                return Optional.of(name);
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }
}
