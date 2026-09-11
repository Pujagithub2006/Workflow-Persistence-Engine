package org.workflow.engine.persistence.repositories;

import org.workflow.engine.domain.model.Project;
import org.workflow.engine.persistence.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class ProjectRepository {

    public Project save(Project project) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Project merged = session.merge(project);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw new RuntimeException("Failed to save project", e);
        }
    }

    public Optional<Project> findByKey(String key) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Project WHERE key = :k", Project.class)
                    .setParameter("k", key)
                    .uniqueResultOptional();
        }
    }

    public List<Project> findByWorkspace(Long workspaceId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Project WHERE workspace.id = :wid ORDER BY name", Project.class)
                    .setParameter("wid", workspaceId)
                    .list();
        }
    }

    public void delete(Long id) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Project p = session.get(Project.class, id);
            if (p != null) session.remove(p);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw new RuntimeException("Failed to delete project", e);
        }
    }
}