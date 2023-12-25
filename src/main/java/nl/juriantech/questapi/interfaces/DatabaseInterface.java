package nl.juriantech.questapi.interfaces;

import org.bson.Document;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DatabaseInterface {

    boolean connect(String connectionString, String databaseName);
    CompletableFuture<Void> saveData(String collection, String key, Object data);

    CompletableFuture<Object> getData(String collection, String key);
    CompletableFuture<List<Document>> getAllDataFrom(String collection);

    CompletableFuture<Void> updateData(String collection, String key, Object newData);

    CompletableFuture<Void> deleteData(String collection, String key);
}