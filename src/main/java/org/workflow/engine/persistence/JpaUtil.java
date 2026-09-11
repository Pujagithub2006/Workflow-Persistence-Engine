package org.workflow.engine.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JpaUtil {
    private static final Logger logger = LoggerFactory.getLogger(JpaUtil.class);
    private static final EntityManagerFactory emf;

    static {
        try {
            emf = Persistence.createEntityManagerFactory("workflow-pu");
            logger.info("EntityManagerFactory created");

        } catch (Exception e) {
            logger.error("Failed to create EntityManagerFactory", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public static void shutdown() {
        if(emf!=null && emf.isOpen()) emf.close();
    }
}
