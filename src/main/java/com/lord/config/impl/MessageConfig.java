package com.lord.config.impl;

import com.lord.config.Configuration;
import com.lord.config.annotations.ConfigData;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class MessageConfig extends Configuration {

    @Getter
    private static MessageConfig instance;

    public MessageConfig(JavaPlugin plugin) {
        super(plugin, "messages.yml");
        instance = this;
        load();
    }

    @ConfigData("prefix")
    private final String prefix = "<cyan><bold>[LORD]</bold></cyan>";

    @ConfigData("no-permission")
    private final String noPermission = "<red>You do not have permission to execute this command.";

}
