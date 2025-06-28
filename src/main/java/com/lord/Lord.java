package com.lord;

import com.lord.command.CommandModule;
import com.lord.data.playerdata.PlayerDataCache;
import com.lord.data.playerdata.PlayerDataListener;
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
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

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

        // 2. Düşük seviyeli servisleri ve veri depolarını (repository) kaydet.
        this.serviceRegistry.register(MenuManager.class, new MenuManager(this));
        this.serviceRegistry.register(ChatInputManager.class, new ChatInputManager(this));
        this.serviceRegistry.register(RankRepository.class, new InMemoryRankRepository());
        this.serviceRegistry.register(GrantRepository.class, new InMemoryGrantRepository());
        this.serviceRegistry.register(PunishmentRepository.class, new InMemoryPunishmentRepository());
        this.serviceRegistry.register(PlayerDataCache.class, new PlayerDataCache(this.serviceRegistry));

        // 3. Yüksek seviyeli modülleri oluştur.
        RankModule rankModule = new RankModule(this.serviceRegistry);
        PunishmentModule punishmentModule = new PunishmentModule(this.serviceRegistry);
        CommandModule commandModule = new CommandModule(this.serviceRegistry);

        // 4. Modülleri, diğer bileşenlerin onlara erişebilmesi için servislere kaydet.
        // Bu, modüller arası iletişimi sağlar.
        this.serviceRegistry.register(RankModule.class, rankModule);
        this.serviceRegistry.register(PunishmentModule.class, punishmentModule);
        this.serviceRegistry.register(CommandModule.class, commandModule);

        // 5. Modüllerin yaşam döngüsünü (enable/disable) yönetmek için ModuleManager'a kaydet.
        this.moduleManager = new ModuleManager();
        this.moduleManager.registerModule(rankModule);
        this.moduleManager.registerModule(punishmentModule);
        this.moduleManager.registerModule(commandModule);

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

        getLogger().info("Lord Core eklentisi başarıyla devre dışı bırakıldı.");
    }
}