package com.lord.grant.repositories.impl;

import com.lord.grant.Grant;
import com.lord.grant.repositories.GrantRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MongoGrantRepository implements GrantRepository {

    private static final String COLLECTION_NAME = "grants";
    private final MongoCollection<Document> collection;

    public MongoGrantRepository(MongoDatabase database) {
        this.collection = database.getCollection(COLLECTION_NAME);
    }

    // Bu metodu GrantRepository arayüzüne eklemeyi unutma
    public CompletableFuture<Optional<Grant>> findById(UUID grantId) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.eq("_id", grantId)).first();
            return Optional.ofNullable(doc).map(this::documentToGrant);
        });
    }

    @Override
    public CompletableFuture<Set<Grant>> findByPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Set<Grant> grants = new HashSet<>();
            for (Document doc : collection.find(Filters.eq("granteeUuid", playerUuid))) {
                grants.add(documentToGrant(doc));
            }
            return grants;
        });
    }

    @Override
    public CompletableFuture<Boolean> save(Grant grant) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document doc = grantToDocument(grant);
                collection.replaceOne(Filters.eq("_id", grant.getUniqueId()), doc, new ReplaceOptions().upsert(true));
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> delete(Grant grant) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                collection.deleteOne(Filters.eq("_id", grant.getUniqueId()));
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    private Document grantToDocument(Grant grant) {
        return new Document("_id", grant.getUniqueId())
                .append("granteeUuid", grant.getGranteeUuid())
                .append("rankName", grant.getRankName())
                .append("issuerUuid", grant.getIssuerUuid())
                .append("creationTime", grant.getCreationTime().toEpochMilli())
                .append("duration", grant.getDuration().getSeconds());
    }

    private Grant documentToGrant(Document doc) {
        return new Grant(
                doc.get("_id", UUID.class),
                doc.get("granteeUuid", UUID.class),
                doc.getString("rankName"),
                doc.get("issuerUuid", UUID.class),
                Instant.ofEpochMilli(doc.getLong("creationTime")),
                Duration.ofSeconds(doc.getLong("duration"))
        );
    }
}