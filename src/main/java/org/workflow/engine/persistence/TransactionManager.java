package org.workflow.engine.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class TransactionManager {
    private static final Logger logger = LoggerFactory.getLogger(TransactionManager.class);

    public static <T> T inTransaction(Function<EntityManager, T> work) {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction etx = em.getTransaction();

        try {
            etx.begin();

            T result = work.apply(em);

            etx.commit();
            return result;

        } catch (Exception e) {
            if (etx.isActive()) {
                etx.rollback();
                logger.warn("Rolled back: {}", e.getMessage());
            }
            throw new RuntimeException("Transaction failed", e);

        } finally {
            em.close();
        }
    }

    public static void inTransaction(Runnable work) {
        inTransaction(em->{
            work.run();
            return null;
        });
    }
}
