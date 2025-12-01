package com.lofo.serenia;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Collections;
import java.util.Map;

public class TestResourceProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "quarkus.datasource.db-kind", "h2",
            "quarkus.datasource.jdbc.url", "jdbc:h2:mem:serenia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS PUBLIC\\;SET REFERENTIAL_INTEGRITY FALSE",
            "quarkus.datasource.username", "sa",
            "quarkus.datasource.password", "sa",
            "quarkus.datasource.jdbc.driver", "org.h2.Driver",
            "quarkus.datasource.jdbc.max-size", "16",
            "serenia.auth.max-users", "2",
            "openai.api.key", "sk-testtesttesttesttest",
            "openai.model", "gpt-4o-mini"
        );
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}

