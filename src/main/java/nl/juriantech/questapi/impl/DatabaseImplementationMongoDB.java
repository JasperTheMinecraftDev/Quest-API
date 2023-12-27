package nl.juriantech.questapi.impl;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import nl.juriantech.questapi.interfaces.DatabaseInterface;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document document = new Document("_id", key)
                        .append("data", data);
                database.getCollection(collection).insertOne(document);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Object> getData(String collection, String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document query = new Document("_id", key);
                return database.getCollection(collection).find(query).first();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<List<Document>> getAllDataFrom(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MongoCollection<Document> collection = database.getCollection(collectionName);
                List<Document> documentList = new ArrayList<>();

                for (Document document : collection.find()) {
                    documentList.add(document);
                }

                return documentList;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateData(String collection, String key, Object newData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MongoCollection<Document> mongoCollection = database.getCollection(collection);
                Document query = new Document("_id", key);

                FindIterable<Document> findIterable = mongoCollection.find(query);
                Document existingDocument = findIterable.first();

                if (existingDocument != null) {
                    Document update = new Document("$set", new Document("data", newData));
                    mongoCollection.updateOne(query, update);
                } else {
                    saveData(collection, key, newData).join();
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> deleteData(String collection, String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document query = new Document("_id", key);
                database.getCollection(collection).deleteOne(query);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return null;
        });
    }
}