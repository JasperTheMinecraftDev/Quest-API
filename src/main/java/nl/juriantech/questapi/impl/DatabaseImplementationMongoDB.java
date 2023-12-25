package nl.juriantech.questapi.impl;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import nl.juriantech.questapi.interfaces.DatabaseInterface;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DatabaseImplementationMongoDB implements DatabaseInterface {
    private MongoClient mongoClient;
    private MongoDatabase database;

    @Override
    public boolean connect(String connectionString, String databaseName) {
        try {
            ConnectionString connString = new ConnectionString(connectionString);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .build();

            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase(databaseName);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public CompletableFuture<Void> saveData(String collection, String key, Object data) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            Document document = new Document("_id", key)
                    .append("data", data);
            database.getCollection(collection).insertOne(document);
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public CompletableFuture<Object> getData(String collection, String key) {
        CompletableFuture<Object> future = new CompletableFuture<>();

        try {
            Document query = new Document("_id", key);
            future.complete(database.getCollection(collection).find(query).first());
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public CompletableFuture<List<Document>> getAllDataFrom(String collectionName) {
        CompletableFuture<List<Document>> future = new CompletableFuture<>();

        try {
            MongoCollection<Document> collection = database.getCollection(collectionName);
            List<Document> documentList = new ArrayList<>();

            for (Document document : collection.find()) {
                documentList.add(document);
            }

            future.complete(documentList);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }


    @Override
    public CompletableFuture<Void> updateData(String collection, String key, Object newData) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            MongoCollection<Document> mongoCollection = database.getCollection(collection);
            Document query = new Document("_id", key);

            // Check if the document exists
            FindIterable<Document> findIterable = mongoCollection.find(query);
            Document existingDocument = findIterable.first();

            if (existingDocument != null) {
                Document update = new Document("$set", new Document("data", newData));
                mongoCollection.updateOne(query, update);
                future.complete(null);
            } else {
                CompletableFuture<Void> saveFuture = saveData(collection, key, newData);
                saveFuture.thenAccept(result -> future.complete(null))
                        .exceptionally(e -> {
                            future.completeExceptionally(e);
                            return null;
                        });
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public CompletableFuture<Void> deleteData(String collection, String key) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            Document query = new Document("_id", key);
            database.getCollection(collection).deleteOne(query);
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }
}