package org.workflow.engine.persistence.repositories;

import org.workflow.engine.domain.model.Workspace;
import org.workflow.engine.persistence.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;

public class WorkspaceRepository {

    public Workspace save(Workspace workspace) {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Workspace merged = em.merge(workspace);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Failed to save workspace", e);
        } finally {
            em.close();
        }
    }

    public Optional<Workspace> findById(Long id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(Workspace.class, id));
        } finally {
            em.close();
        }
    }

    public List<Workspace> findAll() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT w FROM Workspace w ORDER BY w.name", Workspace.class)
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
            Workspace ws = em.find(Workspace.class, id);
            if (ws != null) em.remove(ws);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Failed to delete workspace", e);
        } finally {
            em.close();
        }
    }
}