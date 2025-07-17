package com.lord.database;

import com.lord.Lord;
import com.lord.config.impl.MainConfig;
import com.lord.service.ServiceRegistry;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.bson.UuidRepresentation;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class Mongo {

    private final Lord plugin;

    MainConfig mainConfig;

    @Getter
    private MongoClient client;

    @Getter
    private MongoDatabase database;

    public Mongo(ServiceRegistry registry) {
        this.mainConfig = registry.get(MainConfig.class);
        this.plugin = registry.get(Lord.class);
    }

    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String connectionString = mainConfig.getMongoConnectionString();
                String databaseName = mainConfig.getMongoDatabaseName();

                if (connectionString == null || connectionString.isEmpty()) {
                    plugin.getLogger().log(Level.SEVERE, "MongoDB bağlantı dizesi yapılandırmada bulunamadı!");
                    return false;
                }
                if (databaseName == null || databaseName.isEmpty()) {
                    plugin.getLogger().log(Level.SEVERE, "MongoDB veritabanı adı yapılandırmada bulunamadı!");
                    return false;
                }

                ConnectionString uri = new ConnectionString(connectionString);
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(uri)
                        .uuidRepresentation(UuidRepresentation.STANDARD)
                        .build();
                this.client = MongoClients.create(settings);
                // ConnectionString'de DB adı varsa onu kullan, yoksa parametreden geleni kullan
                String finalDbName = uri.getDatabase() != null ? uri.getDatabase() : databaseName;
                this.database = this.client.getDatabase(finalDbName);

                plugin.getLogger().log(Level.INFO, "MongoDB bağlantısı kuruldu. DB: " + finalDbName);
                return true;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "MongoDB bağlantısı sırasında bir hata oluştu!", e);
                this.client = null; // Bağlantı hatasında istemciyi null yap
                this.database = null; // Bağlantı hatasında veritabanını null yap
                return false;
            }
        });
    }

    public void disconnect() {
        if (client != null) {
            client.close();
            plugin.getLogger().log(Level.INFO, "MongoDB bağlantısı kapatıldı.");
        }
    }
}
