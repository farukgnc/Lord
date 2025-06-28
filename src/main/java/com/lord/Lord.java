package com.lord;

import com.lord.command.CommandModule;
import com.lord.config.impl.MainConfig;
import com.lord.config.impl.MessageConfig;
import com.lord.data.factory.MongoRepositoryFactory;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.data.playerdata.PlayerDataListener;
import com.lord.database.Database;
import com.lord.factory.InMemoryRepositoryFactory;
import com.lord.factory.RepositoryFactory;
import com.lord.grant.repositories.GrantRepository;
import com.lord.grant.repositories.impl.InMemoryGrantRepository;
import com.lord.menu.MenuManager;
import com.lord.module.ModuleManager;
import com.lord.punishment.PunishmentModule;
import com.lord.punishment.repositories.PunishmentRepository;
import com.lord.punishment.repositories.impl.InMemoryPunishmentRepository;
import com.lord.rank.RankModule;
import com.lord.rank.repositories.RankRepository;
import com.lord.rank.repositories.impl.InMemoryRankRepository;
import com.lord.services.ChatInputManager;
import com.lord.services.ServiceRegistry;
import lombok.Data;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

@Getter
public final class Lord extends JavaPlugin {

    private ServiceRegistry serviceRegistry;
    private ModuleManager moduleManager;

    @Override
    public void onEnable() {
        getLogger().info("Lord Core eklentisi başlatılıyor...");

        // 1. Servis ve modül örneklerini tutacak merkezi kayıt defterini oluştur.
        this.serviceRegistry = new ServiceRegistry();
        this.serviceRegistry.register(Lord.class, this);

        this.serviceRegistry.register(MainConfig.class, new MainConfig(this));
        this.serviceRegistry.register(MessageConfig.class, new MessageConfig(this));

        RepositoryFactory repositoryFactory = new InMemoryRepositoryFactory(serviceRegistry);
        String databaseType = serviceRegistry.get(MainConfig.class).getDatabaseType();

        if (databaseType.equals("mongodb")) {
            repositoryFactory = new MongoRepositoryFactory(serviceRegistry);
        }

        repositoryFactory.setup().thenAccept((connected) -> {
            if (!connected) {
                getLogger().info("Database initialization failed! Disabling plugin.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // 2. Düşük seviyeli servisleri ve veri depolarını (repository) kaydet.
            this.serviceRegistry.register(MenuManager.class, new MenuManager(this));
            this.serviceRegistry.register(ChatInputManager.class, new ChatInputManager(this));
            this.serviceRegistry.register(RankRepository.class, new InMemoryRankRepository());
            this.serviceRegistry.register(GrantRepository.class, new InMemoryGrantRepository());
            this.serviceRegistry.register(PunishmentRepository.class, new InMemoryPunishmentRepository());
            this.serviceRegistry.register(PlayerDataCache.class, new PlayerDataCache(this.serviceRegistry));

            // 5. Modüllerin yaşam döngüsünü (enable/disable) yönetmek için ModuleManager'a kaydet.
            this.moduleManager = new ModuleManager();
            this.moduleManager.registerModule(new RankModule(this.serviceRegistry));
            this.moduleManager.registerModule(new PunishmentModule(this.serviceRegistry));
            this.moduleManager.registerModule(new CommandModule(this.serviceRegistry));

            // 6. Tüm modülleri etkinleştir.
            this.moduleManager.enableModules();

            // 7. Genel dinleyicileri (listener) kaydet.
            new PlayerDataListener(this.serviceRegistry);

            getLogger().info("Lord Core eklentisi başarıyla başlatıldı!");
        });
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