package org.workflow.engine.persistence.repositories;

import jakarta.persistence.LockModeType;
import org.workflow.engine.domain.model.Issue;
import org.workflow.engine.domain.valueobject.IssueKey;
import org.workflow.engine.persistence.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.workflow.engine.persistence.TransactionManager;

import java.util.List;
import java.util.Optional;

public class IssueRepository {
    // Locks

    // load issue with a pessimistic write lock (use only when you need to serialize updates to a single issue).
    public Optional<Issue> findByIdForUpdate(Long id) {
        return TransactionManager.inTransaction(em ->
                Optional.ofNullable(em.find(Issue.class, id, LockModeType.PESSIMISTIC_WRITE))
        );
    }

    // Fetch-Join Methods

    // 1. fetch issue with reporter and assignee in ONE query (when we need both)
    public Optional<Issue> findByKeyWithUsers(String key) {
        EntityManager em = JpaUtil.getEntityManager();

        try {
            return em.createQuery(
                    "SELECT i FROM Issue i " +
                    "JOIN FETCH i.reporter " +
                            "LEFT JOIN FETCH i.assignee " +
                            "WHERE i.issueKey = :k",
                    Issue.class)
                    .setParameter("k", new IssueKey(key))
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }

    // 2. fetch issues with reporter and current state in ONE query (use this for board/listing views - kanban or sprint boards).
    public List<Issue> findByProjectWithDetails(Long projectId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT i FROM Issue i " +
                                    "JOIN FETCH i.reporter " +
                                    "LEFT JOIN FETCH i.assignee " +
                                    "JOIN FETCH i.currentState " +
                                    "WHERE i.project.id = :pid " +
                                    "ORDER BY i.id DESC",
                            Issue.class)
                    .setParameter("pid", projectId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public Issue save(Issue issue) {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Issue merged = em.merge(issue);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Failed to save issue", e);
        } finally {
            em.close();
        }
    }

    public Optional<Issue> findByKey(String key) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT i FROM Issue i WHERE i.issueKey = :k", Issue.class)
                    .setParameter("k", new IssueKey(key))
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }

    public List<Issue> findByProject(Long projectId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT i FROM Issue i WHERE i.project.id = :pid ORDER BY i.id DESC",
                            Issue.class)
                    .setParameter("pid", projectId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void delete(Long id) {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Issue i = em.find(Issue.class, id);
            if (i != null) em.remove(i);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Failed to delete issue", e);
        } finally {
            em.close();
        }
    }
}