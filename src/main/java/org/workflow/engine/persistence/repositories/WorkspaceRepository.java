package org.workflow.engine.persistence.repositories;

import org.workflow.engine.domain.model.Workspace;
import org.workflow.engine.persistence.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class WorkspaceRepository {

    public Workspace save(Workspace workspace) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Workspace merged = session.merge(workspace);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw new RuntimeException("Failed to save workspace", e);
        }
    }

    public Optional<Workspace> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Workspace.class, id));
        }
    }

    public List<Workspace> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Workspace ORDER BY name", Workspace.class).list();
        }
    }

    public void delete(Long id) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Workspace ws = session.get(Workspace.class, id);
            if (ws != null) session.remove(ws);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw new RuntimeException("Failed to delete workspace", e);
        }
    }
}