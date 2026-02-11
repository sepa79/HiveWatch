package io.pockethive.hivewatch.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class ContextLoadsTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("hive_watch_test")
        .withUsername("hive_watch")
        .withPassword("hive_watch");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("HW_DB_URL", postgres::getJdbcUrl);
        registry.add("HW_DB_USER", postgres::getUsername);
        registry.add("HW_DB_PASSWORD", postgres::getPassword);
    }

    @Test
    void contextLoads() {
    }
}
