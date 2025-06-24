package com.lord;

import com.lord.menu.MenuManager;
import com.lord.modules.ModuleManager;
import com.lord.modules.impl.CommandModule;
import com.lord.modules.impl.RankModule;
import com.lord.permission.PermissionCache;
import com.lord.permission.PermissionListener;
import com.lord.repositories.GrantRepository;
import com.lord.repositories.RankRepository;
import com.lord.repositories.impl.InMemoryGrantRepository;
import com.lord.repositories.impl.InMemoryRankRepository;
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
        this.serviceRegistry.register(PermissionCache.class, new PermissionCache(this.serviceRegistry));

        this.moduleManager = new ModuleManager();

        this.moduleManager.registerModule(new RankModule(this.serviceRegistry));
        this.moduleManager.registerModule(new CommandModule(this.serviceRegistry));

        this.moduleManager.enableModules();

        new PermissionListener(this.serviceRegistry);

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