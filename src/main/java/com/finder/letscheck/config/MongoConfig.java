package com.finder.letscheck.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Override
    protected String getDatabaseName() {
        return "letscheck";
    }

    @Bean
    @Override
    public com.mongodb.client.MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString("mongodb+srv://spotzy_db_admin:D56sLcYHHc9jKVfy@spotzydb.9klgvlp.mongodb.net/letscheck");

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder ->
                        builder.maxSize(50).minSize(10))
                .build();

        return com.mongodb.client.MongoClients.create(settings);
    }
}