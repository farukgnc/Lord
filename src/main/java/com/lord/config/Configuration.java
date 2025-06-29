package com.lord.config;

import com.lord.config.annotations.ConfigData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public abstract class Configuration {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public Configuration(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
    }

    public void load() {
        // 1. Dosyanın diskte var olup olmadığını kontrol et.
        if (!file.exists()) {
            // Eğer yoksa, önce plugin'in klasörünü oluşturduğundan emin ol.
            plugin.getDataFolder().mkdirs();
            try {
                // Sonra boş dosyayı oluştur. saveResource'a artık ihtiyacımız yok.
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create config file: " + file.getName());
                e.printStackTrace();
                return;
            }
        }

        // 2. Dosyayı YamlConfiguration olarak yükle.
        this.config = YamlConfiguration.loadConfiguration(file);
        boolean needsSave = false;

        // 3. Bu sınıfı miras alan alt sınıfın (örn: MainConfig) alanlarını tara.
        for (Field field : this.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(ConfigData.class)) {
                continue;
            }

            try {
                // Özel (private) alanlara erişimi aç.
                field.setAccessible(true);
                String path = field.getAnnotation(ConfigData.class).value();

                // 4. Eğer dosyada bu path'e ait bir değer YOKSA...
                if (!config.contains(path)) {
                    // Java sınıfındaki varsayılan değeri al ve config nesnesine koy.
                    config.set(path, field.get(this));
                    // Dosyanın daha sonra kaydedilmesi gerektiğini işaretle.
                    needsSave = true;
                }

                // 5. Her durumda, config'deki son değeri (ya dosyadan gelen ya da yeni eklenen)
                // Java sınıfındaki alana ata.
                // Bu, config'in her zaman en güncel halinin kodda olmasını sağlar.
                field.set(this, config.get(path));

            } catch (IllegalAccessException e) {
                plugin.getLogger().warning("Failed to access config field: " + field.getName());
                e.printStackTrace();
            }
        }

        // 6. Eğer config'e yeni varsayılan değerler eklendiyse, dosyayı kaydet.
        if (needsSave) {
            save();
        }
    }

    public void save() {
        try {
            this.config.save(this.file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config file: " + file.getName());
            e.printStackTrace();
        }
    }
}