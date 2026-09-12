package org.workflow.engine.persistence.repositories;

import org.workflow.engine.domain.enums.IssuePriority;
import org.workflow.engine.domain.model.User;
import org.workflow.engine.persistence.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BatchRepository {
    private static final Logger logger = LoggerFactory.getLogger(BatchRepository.class);
    private static final int BATCH_SIZE = 50;

    /**
     * Insert many users in batches.
     * Without batching: N INSERT statements.
     * With batching: N / BATCH_SIZE round trips.
     */
    public void batchInsertUsers(List<User> users) {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            for (int i = 0; i < users.size(); i++) {
                em.persist(users.get(i));
                if ((i + 1) % BATCH_SIZE == 0) {
                    em.flush();
                    em.clear();   // IMPORTANT — see comment below
                }
            }
            tx.commit();
            logger.info("Batch inserted {} users", users.size());
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Batch insert failed", e);
        } finally {
            em.close();
        }
    }

    /**
     * Bulk update without loading entities.
     * Without bulk: SELECT all matching + N UPDATEs.
     * With bulk: 1 UPDATE.
     */
    public int bulkUpdatePriority(Long projectId, IssuePriority from, IssuePriority to) {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            int updated = em.createQuery(
                            "UPDATE Issue i SET i.priority = :to " +
                                    "WHERE i.project.id = :pid AND i.priority = :from")
                    .setParameter("to", to)
                    .setParameter("from", from)
                    .setParameter("pid", projectId)
                    .executeUpdate();
            tx.commit();
            logger.info("Bulk updated {} issues", updated);
            return updated;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Bulk update failed", e);
        } finally {
            em.close();
        }
    }

    /**
     * Bulk delete without loading entities.
     */
    public int bulkDeleteIssuesByProject(Long projectId) {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            int deleted = em.createQuery("DELETE FROM Issue i WHERE i.project.id = :pid")
                    .setParameter("pid", projectId)
                    .executeUpdate();
            tx.commit();
            logger.info("Bulk deleted {} issues", deleted);
            return deleted;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Bulk delete failed", e);
        } finally {
            em.close();
        }
    }
}