package com.lord.config.impl;

import com.lord.config.Configuration;
import com.lord.config.annotations.ConfigData;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class MainConfig extends Configuration {

    @Getter
    private static MainConfig instance;

    public MainConfig(JavaPlugin plugin) {
        super(plugin, "config.yml");
        instance = this;
        load();
    }

    @ConfigData("databaseType")
    private String databaseType = "mongodb";

    @ConfigData("mongo.connection-string")
    private String mongoConnectionString = "mongodb+srv://titan:12345a@titan.e4ojfqr.mongodb.net/?retryWrites=true&w=majority&appName=lord";

    @ConfigData("mongo.database-name")
    private String mongoDatabaseName = "lord";

    @ConfigData("redis.host")
    private String redisHost = "localhost";

    @ConfigData("redis.port")
    private int redisPort = 6379;

    @ConfigData("redis.password")
    private String redisPassword = "";

    @ConfigData("server-id")
    private String serverId = "lobby-1";

    @ConfigData("chat.format")
    private String chatFormat = "<prefix><gray><player_name></gray><suffix><dark_gray>: <white><message>";

    @ConfigData("chat.spam-filter.enabled")
    private boolean chatSpamFilterEnabled = true;

    @ConfigData("chat.spam-filter.cooldown-seconds")
    private double chatSpamFilterCooldown = 3.0;
}
