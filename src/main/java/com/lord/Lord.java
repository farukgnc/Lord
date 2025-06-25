package com.lord;

import com.lord.menu.MenuManager;
import com.lord.modules.ModuleManager;
import com.lord.command.CommandModule;
import com.lord.rank.RankModule;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.data.playerdata.PlayerDataListener;
import com.lord.grant.repositories.GrantRepository;
import com.lord.rank.repositories.RankRepository;
import com.lord.grant.repositories.impl.InMemoryGrantRepository;
import com.lord.rank.repositories.impl.InMemoryRankRepository;
import com.lord.services.ChatInputManager;
import com.lord.services.ServiceRegistry;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Lord extends JavaPlugin {

    private ServiceRegistry serviceRegistry;
    private ModuleManager moduleManager;

    @Override
    public void onEnable() {
        getLogger().info("Lord Core eklentisi başlatılıyor...");

        this.serviceRegistry = new ServiceRegistry();

        this.serviceRegistry.register(Lord.class, this);
        this.serviceRegistry.register(RankRepository.class, new InMemoryRankRepository());
        this.serviceRegistry.register(GrantRepository.class, new InMemoryGrantRepository());
        this.serviceRegistry.register(MenuManager.class, new MenuManager(this));
        this.serviceRegistry.register(PlayerDataCache.class, new PlayerDataCache(this.serviceRegistry));
        this.serviceRegistry.register(ChatInputManager.class, new ChatInputManager(this));

        this.moduleManager = new ModuleManager();

        this.moduleManager.registerModule(new RankModule(this.serviceRegistry));
        this.moduleManager.registerModule(new CommandModule(this.serviceRegistry));

        this.moduleManager.enableModules();

        new PlayerDataListener(this.serviceRegistry);

        getLogger().info("Lord Core eklentisi başarıyla başlatıldı!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Lord Core eklentisi devre dışı bırakılıyor...");

        if (this.moduleManager != null) {
            this.moduleManager.disableModules();
        }

        getLogger().info("Lord Core eklentisi başarıyla devre dışı bırakıldı.");
    }
}