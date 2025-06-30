package com.lord.punishment.repositories.impl;

import com.lord.punishment.Punishment;
import com.lord.punishment.PunishmentStatusFilter;
import com.lord.punishment.PunishmentType;
import com.lord.punishment.repositories.PunishmentRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MongoPunishmentRepository implements PunishmentRepository {

    private static final String COLLECTION_NAME = "punishments";
    private final MongoCollection<Document> collection;

    public MongoPunishmentRepository(MongoDatabase database) {
        this.collection = database.getCollection(COLLECTION_NAME);
    }

    @Override
    public CompletableFuture<List<Punishment>> findWithFilters(UUID playerUuid, PunishmentStatusFilter statusFilter, @Nullable PunishmentType typeFilter) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            List<Bson> filters = new ArrayList<>();
            filters.add(Filters.eq("punishedUuid", playerUuid));

            if (typeFilter != null) {
                filters.add(Filters.eq("type", typeFilter.name()));
            }

            Bson finalFilter = Filters.and(filters);

            collection.find(finalFilter)
                    .sort(Sorts.descending("creationTime"))
                    .forEach(doc -> {
                        Punishment punishment = documentToPunishment(doc);
                        switch (statusFilter) {
                            case ACTIVE:
                                if (punishment.isActive()) punishments.add(punishment);
                                break;
                            case INACTIVE:
                                if (!punishment.isActive()) punishments.add(punishment);
                                break;
                            case ALL:
                            default:
                                punishments.add(punishment);
                                break;
                        }
                    });
            return punishments;
        });
    }

    @Override
    public CompletableFuture<Void> save(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            Document doc = punishmentToDocument(punishment);
            collection.replaceOne(Filters.eq("_id", punishment.getUniqueId()), doc, new ReplaceOptions().upsert(true));
        });
    }

    @Override
    public CompletableFuture<Void> delete(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            collection.deleteOne(Filters.eq("_id", punishment.getUniqueId()));
        });
    }

    private Document punishmentToDocument(Punishment p) {
        Document doc = new Document("_id", p.getUniqueId())
                .append("type", p.getType().name())
                .append("punishedUuid", p.getPunishedUuid())
                .append("reason", p.getReason())
                .append("issuerUuid", p.getIssuerUuid())
                .append("creationTime", p.getCreationTime().toEpochMilli())
                .append("duration", p.getDuration().getSeconds())
                .append("pardoned", p.isPardoned());

        if (p.isPardoned()) {
            doc.append("pardonerUuid", p.getPardonerUuid());
            doc.append("pardonTime", p.getPardonTime().toEpochMilli());
        }
        return doc;
    }

    private Punishment documentToPunishment(Document doc) {
        Punishment punishment = new Punishment(
                doc.get("_id", UUID.class),
                PunishmentType.valueOf(doc.getString("type")),
                doc.get("punishedUuid", UUID.class),
                doc.getString("reason"),
                doc.get("issuerUuid", UUID.class),
                Instant.ofEpochMilli(doc.getLong("creationTime")),
                Duration.ofSeconds(doc.getLong("duration"))
        );

        if (doc.getBoolean("pardoned", false)) {
            punishment.setPardoned(true);
            punishment.setPardonerUuid(doc.get("pardonerUuid", UUID.class));
            punishment.setPardonTime(Instant.ofEpochMilli(doc.getLong("pardonTime")));
        }
        return punishment;
    }
}