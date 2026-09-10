package org.workflow.engine.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class SchemaInitializer {

    private static final Logger logger = LoggerFactory.getLogger(SchemaInitializer.class);

    public static void initialize() {
        String sql = readSchemaFile();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            logger.info("Schema initialized successfully");

        } catch (Exception e) {
            throw new RuntimeException("Schema initialization failed", e);
        }
    }

    private static String readSchemaFile() {
        try (InputStream in = SchemaInitializer.class.getClassLoader()
                .getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new RuntimeException("schema.sql not found on classpath");
            }
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines()
                    .filter(line -> !line.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read schema.sql", e);
        }
    }
}
