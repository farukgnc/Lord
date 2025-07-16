package com.lord;

import com.lord.chat.ChatModule;
import com.lord.command.CommandModule;
import com.lord.config.impl.MainConfig;
import com.lord.config.impl.MessageConfig;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.data.playerdata.PlayerDataListener;
import com.lord.factory.InMemoryRepositoryFactory;
import com.lord.factory.MongoRepositoryFactory;
import com.lord.factory.RepositoryFactory;
import com.lord.grant.GrantModule;
import com.lord.menu.MenuManager;
import com.lord.module.ModuleManager;
import com.lord.punishment.PunishmentModule;
import com.lord.rank.RankModule;
import com.lord.redis.RedisModule;
import com.lord.services.ServiceRegistry;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Lord extends JavaPlugin {

    // TODO punishmentModule:48
    // TODO redis sistemi bağlandı fakat mesajı okuyan serverlar gerekli işlemleri yapmıyor
    // o kısımlar redis.sync paketi içinde

    private ServiceRegistry serviceRegistry;
    private ModuleManager moduleManager;
    private RepositoryFactory repositoryFactory;

    @Override
    public void onEnable() {
        getLogger().info("Lord Core eklentisi başlatılıyor...");

        // 1. Servis ve modül örneklerini tutacak merkezi kayıt defterini oluştur.
        this.serviceRegistry = new ServiceRegistry();
        this.serviceRegistry.register(Lord.class, this);

        this.serviceRegistry.register(MainConfig.class, new MainConfig(this));
        this.serviceRegistry.register(MessageConfig.class, new MessageConfig(this));

        repositoryFactory = new InMemoryRepositoryFactory(serviceRegistry);
        String databaseType = serviceRegistry.get(MainConfig.class).getDatabaseType();

        if (databaseType.equals("mongodb")) {
            repositoryFactory = new MongoRepositoryFactory(serviceRegistry);
        }

        boolean connected = repositoryFactory.connect().join(); // baslangıcta thread bloklayabiliriz sorun yok

        if (!connected) {
            getLogger().info("Database initialization failed! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        repositoryFactory.createRepositories().join(); // bunu da bloklamak zorundayız

        // 2. Düşük seviyeli servisleri ve veri depolarını (repository) kaydet.
        this.serviceRegistry.register(MenuManager.class, new MenuManager(this));
        this.serviceRegistry.register(PlayerDataCache.class, new PlayerDataCache(this.serviceRegistry));

        // 5. Modüllerin yaşam döngüsünü (enable/disable) yönetmek için ModuleManager'a kaydet.
        this.moduleManager = new ModuleManager();
        this.moduleManager.registerModule(new RedisModule(this.serviceRegistry));
        this.moduleManager.registerModule(new RankModule(this.serviceRegistry));
        this.moduleManager.registerModule(new GrantModule(this.serviceRegistry));
        this.moduleManager.registerModule(new PunishmentModule(this.serviceRegistry));
        this.moduleManager.registerModule(new ChatModule(this.serviceRegistry));
        this.moduleManager.registerModule(new CommandModule(this.serviceRegistry));

        // 6. Tüm modülleri etkinleştir.
        this.moduleManager.enableModules();

        // 7. Genel dinleyicileri (listener) kaydet.
        new PlayerDataListener(this.serviceRegistry);

        getLogger().info("Lord Core eklentisi başarıyla başlatıldı!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Lord Core eklentisi devre dışı bırakılıyor...");

        if (this.moduleManager != null) {
            this.moduleManager.disableModules();
        }

        if (this.repositoryFactory != null) {
            this.repositoryFactory.disconnect();
        }

        getLogger().info("Lord Core eklentisi başarıyla devre dışı bırakıldı.");
    }
}