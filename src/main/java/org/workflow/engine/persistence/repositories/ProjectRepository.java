package org.workflow.engine.persistence.repositories;

import org.workflow.engine.domain.model.Project;
import org.workflow.engine.persistence.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;

public class ProjectRepository {

    public Project save(Project project) {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Project merged = em.merge(project);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Failed to save project", e);
        } finally {
            em.close();
        }
    }

    public Optional<Project> findByKey(String key) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT p FROM Project p WHERE p.key = :k", Project.class)
                    .setParameter("k", key)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }

    public List<Project> findByWorkspace(Long workspaceId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT p FROM Project p WHERE p.workspace.id = :wid ORDER BY p.name",
                            Project.class)
                    .setParameter("wid", workspaceId)
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
            Project p = em.find(Project.class, id);
            if (p != null) em.remove(p);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Failed to delete project", e);
        } finally {
            em.close();
        }
    }
}