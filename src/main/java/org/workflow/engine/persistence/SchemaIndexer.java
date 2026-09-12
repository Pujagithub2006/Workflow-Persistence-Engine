package org.workflow.engine.persistence;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.workflow.engine.bootstrap.DataBootstrapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class SchemaIndexer {
    private static final Logger logger = LoggerFactory.getLogger(SchemaIndexer.class);

    public static void applyIndexes() {
        String sql = readFile("sql/indexes.sql");
        EntityManager em = JpaUtil.getEntityManager();
        try {
            Connection conn = em.unwrap(Connection.class);
            try (Statement stmt = conn.createStatement()) {
                for (String statement : sql.split(";")) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        try {
                            stmt.execute(trimmed);
                        } catch (Exception e) {
                            if (!e.getMessage().contains("already exists")) {
                                logger.warn("Index failed: {}", e.getMessage());
                            }
                        }
                    }
                }
            }
            logger.info("Indexes applied");
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply indexes", e);
        } finally {
            em.close();
        }
    }

    private static String readFile(String path) {
        try (InputStream in = SchemaIndexer.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException(path + " not found");
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines()
                    .filter(line -> !line.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + path, e);
        }
    }

    public static void main(String[] args) {
        try {
            SchemaIndexer.applyIndexes();
            new DataBootstrapper().bootstrap();
        } finally {
            JpaUtil.shutdown();
        }
    }
}