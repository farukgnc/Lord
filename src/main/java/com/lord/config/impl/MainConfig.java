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
    private final String databaseType = "mongodb";

    @ConfigData("mongo.connection-string")
    private final String mongoConnectionString = "mongodb+srv://titan:12345a@titan.e4ojfqr.mongodb.net/?retryWrites=true&w=majority&appName=lord";

    @ConfigData("mongo.database-name")
    private final String mongoDatabaseName = "lord";

    @ConfigData("redis.uri")
    private final String redisUri = "redis://localhost:6379";

    @ConfigData("server-id")
    private final String serverId = "lobby-1";
}
