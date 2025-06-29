package com.lord.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class PlayerResolver {

    private static final Cache<String, UUID> timedCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public static CompletableFuture<Optional<UUID>> resolve(String name) {
        Player onlinePlayer = Bukkit.getPlayer(name);
        if (onlinePlayer != null) {
            timedCache.put(name.toLowerCase(), onlinePlayer.getUniqueId());
            return CompletableFuture.completedFuture(Optional.of(onlinePlayer.getUniqueId()));
        }

        UUID cachedUuid = timedCache.getIfPresent(name.toLowerCase());
        if (cachedUuid != null) {
            return CompletableFuture.completedFuture(Optional.of(cachedUuid));
        }

        return CompletableFuture.supplyAsync(() -> {
            String url = "https://api.mojang.com/users/profiles/minecraft/" + name;
            try {
                URL uri = new URL(url);
                BufferedReader in = new BufferedReader(new InputStreamReader(uri.openStream()));
                JSONObject object = (JSONObject) JSONValue.parseWithException(in);
                UUID uuid = UUID.fromString(object.get("id").toString().replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                timedCache.put(name, uuid);
                return Optional.of(uuid);
            } catch(Exception exception) {
                exception.printStackTrace();
            }
            return Optional.empty();
        });
    }
}