package org.workflow.engine.persistence.repositories;

import org.workflow.engine.domain.model.State;
import org.workflow.engine.persistence.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class StateRepository {

    public State save(State state) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            State merged = session.merge(state);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw new RuntimeException("Failed to save state", e);
        }
    }

    public Optional<State> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(State.class, id));
        }
    }

    public List<State> findByWorkflow(Long workflowId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM State WHERE workflow.id = :wid ORDER BY id", State.class)
                    .setParameter("wid", workflowId)
                    .list();
        }
    }
}