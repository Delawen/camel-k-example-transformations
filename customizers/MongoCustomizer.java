package customizers;

// camel-k: dependency=camel:mongodb

import org.apache.camel.BindToRegistry;
import org.apache.camel.PropertyInject;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class MongoCustomizer {

    @BindToRegistry("mongoBean")
    public static MongoClient initializeMongoDB(
        @PropertyInject("mongodb.user") String user,
        @PropertyInject("mongodb.password") String password,
        @PropertyInject("mongodb.host") String host,
        @PropertyInject("mongodb.database") String database) {
            return MongoClients.create(String.format("mongodb://%s:%s@%s/%s", user, password, host, database));
    }

}
