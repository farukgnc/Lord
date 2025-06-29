package com.lord.rank.repositories.impl;

import com.lord.Lord;
import com.lord.database.Mongo;
import com.lord.rank.Rank;
import com.lord.rank.repositories.RankRepository;
import com.lord.services.ServiceRegistry;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class MongoRankRepository implements RankRepository {

    private static final String COLLECTION_NAME = "ranks";

    private final Map<String, Rank> ranks = new ConcurrentHashMap<>();
    private final MongoCollection<Document> collection;

    public MongoRankRepository(ServiceRegistry registry) {
        Mongo mongoService = registry.get(Mongo.class);
        MongoDatabase database = mongoService.getDatabase();
        this.collection = database.getCollection(COLLECTION_NAME);
    }

    @Override
    public CompletableFuture<Set<Rank>> loadAllRanks() {
        // Bu metot, arka planda tüm rank'ları veritabanından okuyup önbelleğe alır.
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("[Lord] Loading all ranks from MongoDB into cache...");
            this.ranks.clear();
            for (Document doc : this.collection.find()) {
                Rank rank = documentToRank(doc);
                this.ranks.put(rank.getName().toLowerCase(), rank);
            }
            System.out.println("[Lord] " + this.ranks.size() + " ranks loaded successfully.");
            return getAllRanks(); // Yüklenen rank'ların bir kopyasını döndür
        });
    }

    @Override
    public Optional<Rank> findByName(String name) {
        // Bu metot HIZLIDIR. Veritabanına gitmez, sadece bellekteki haritaya bakar.
        return Optional.ofNullable(this.ranks.get(name.toLowerCase()));
    }

    @Override
    public Set<Rank> getAllRanks() {
        // Bu metot HIZLIDIR. Sadece bellekteki rank'ların bir kopyasını döner.
        return new HashSet<>(this.ranks.values());
    }

    @Override
    public void save(Rank rank) {
        // 1. Belleği anında güncelle.
        this.ranks.put(rank.getName().toLowerCase(), rank);

        // 2. Veritabanına yazma işlemini arka plana at, sunucuyu yavaşlatma.
        CompletableFuture.runAsync(() -> {
            Document doc = rankToDocument(rank);
            this.collection.replaceOne(
                    Filters.eq("_id", rank.getName().toLowerCase()),
                    doc,
                    new ReplaceOptions().upsert(true)
            );
        });
    }

    @Override
    public void delete(String name) {
        // 1. Belleği anında güncelle.
        this.ranks.remove(name.toLowerCase());

        // 2. Veritabanından silme işlemini arka plana at.
        CompletableFuture.runAsync(() -> {
            this.collection.deleteOne(Filters.eq("_id", name.toLowerCase()));
        });
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        // supplyAsync, bir sonuç (bu durumda Boolean) döndüren asenkron bir işlem başlatır.
        // Bu işlem, MongoDB'ye "bu koleksiyonda kaç döküman var?" sorgusunu gönderir.
        // Bu bir ağ işlemi olduğu için arka planda çalıştırılır ve ana thread'i asla bloklamaz.
        return CompletableFuture.supplyAsync(() -> this.collection.countDocuments() == 0);
    }

    // --- Veri Dönüştürme Metotları ---

    private Document rankToDocument(Rank rank) {
        return new Document("_id", rank.getName().toLowerCase())
                .append("name", rank.getName())
                .append("priority", rank.getPriority())
                .append("prefix", rank.getPrefix())
                .append("suffix", rank.getSuffix())
                .append("permissions", new ArrayList<>(rank.getPermissions()))
                .append("inheritance", new ArrayList<>(rank.getParentRankNames()));
    }

    private Rank documentToRank(Document doc) {
        Rank rank = new Rank(doc.getString("name"));
        rank.setPriority(doc.getInteger("priority", 1));
        rank.setPrefix(doc.getString("prefix"));
        rank.setSuffix(doc.getString("suffix"));
        rank.getPermissions().addAll(doc.getList("permissions", String.class, new ArrayList<>()));
        rank.getParentRankNames().addAll(doc.getList("inheritance", String.class, new ArrayList<>()));
        return rank;
    }
}