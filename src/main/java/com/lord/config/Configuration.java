package com.lord.config;

import com.lord.config.annotations.ConfigData;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Tüm config sınıflarının extend edeceği ana sınıf.
 * Dosya I/O ve Reflection işlemlerini otomatik olarak yönetir.
 */
@Getter
public abstract class Configuration {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;
    private boolean loaded = false;

    public Configuration(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
    }

    /**
     * Config dosyasını yükler. Eğer dosya yoksa, sınıftaki varsayılan
     * değerlerle yeni bir dosya oluşturur.
     */
    public void load() {
        if (!file.exists()) {
            // Plugin klasörünün var olduğundan emin ol
            plugin.getDataFolder().mkdirs();
            // Varsayılan dosyayı JAR'dan kopyala (eğer varsa)
            plugin.saveResource(file.getName(), false);
        }

        this.config = YamlConfiguration.loadConfiguration(file);
        boolean needsSave = false;

        // Bu sınıfı extend eden alt sınıfın (örn: MessageConfig) alanlarını tara
        for (Field field : this.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(ConfigData.class)) {
                continue;
            }

            try {
                // Özel (private) alanlara erişimi aç
                field.setAccessible(true);
                String path = field.getAnnotation(ConfigData.class).value();

                if (config.contains(path)) {
                    // Eğer config'de değer varsa, Java'daki alana o değeri ata
                    field.set(this, config.get(path));
                } else {
                    // Eğer config'de değer yoksa, Java'daki varsayılan değeri config'e yaz
                    config.set(path, field.get(this));
                    needsSave = true;
                }
            } catch (IllegalAccessException e) {
                plugin.getLogger().warning("Failed to access config field: " + field.getName());
                e.printStackTrace();
            }
        }

        // Eğer yeni varsayılan değerler eklendiyse, dosyayı kaydet
        if (needsSave) {
            save();
        }
        this.loaded = true;
    }

    /**
     * Yapılan değişiklikleri dosyaya kaydeder.
     */
    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config file: " + file.getName());
            e.printStackTrace();
        }
    }
}
