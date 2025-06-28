package com.lord.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlayerResolver {

    private static final Cache<String, UUID> timedCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private static final Pattern UUID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"(.*?)\"");

    /**
     * Finds a player by their name asynchronously, returning a CompletableFuture.
     * This is the recommended approach for BungeeCord networks.
     *
     * @param name The name of the player to find.
     * @return A CompletableFuture that will eventually contain the OfflinePlayer Optional.
     */
    public static CompletableFuture<Optional<UUID>> resolve(String name) {
        // 1. Önce online oyuncuları kontrol et (en hızlısı).
        Player onlinePlayer = Bukkit.getPlayer(name);
        if (onlinePlayer != null) {
            timedCache.put(name.toLowerCase(), onlinePlayer.getUniqueId());
            // Sonuç zaten belli olduğu için tamamlanmış bir Future döndür.
            return CompletableFuture.completedFuture(Optional.of(onlinePlayer.getUniqueId()));
        }

        // 2. Cache'i kontrol et.
        UUID cachedUuid = timedCache.getIfPresent(name.toLowerCase());
        if (cachedUuid != null) {
            // Sonuç zaten belli olduğu için tamamlanmış bir Future döndür.
            return CompletableFuture.completedFuture(Optional.of(cachedUuid));
        }

        // 3. Cache'te yoksa, asenkron olarak Mojang API'sine sor.
        // supplyAsync, bu işlemi arka plan thread'inde çalıştırır.
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000); // 5 saniye timeout
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line = reader.readLine();
                    reader.close();

                    if (line != null) {
                        Matcher matcher = UUID_PATTERN.matcher(line);
                        if (matcher.find()) {
                            String uuidString = matcher.group(1).replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                            UUID uuid = UUID.fromString(uuidString);
                            // Gelecekteki istekler için cache'e koy.
                            timedCache.put(name.toLowerCase(), uuid);
                            // ARTIK OFFLINEPLAYER DEĞİL, SADECE UUID DÖNÜYORUZ
                            return Optional.of(uuid);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Optional.empty(); // Oyuncu bulunamazsa boş Optional döndür.
        });
    }
}