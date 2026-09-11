package org.workflow.engine.persistence.repositories;

import org.workflow.engine.domain.model.State;
import org.workflow.engine.persistence.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;

public class StateRepository {

    public State save(State state) {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            State merged = em.merge(state);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Failed to save state", e);
        } finally {
            em.close();
        }
    }

    public Optional<State> findById(Long id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(State.class, id));
        } finally {
            em.close();
        }
    }

    public List<State> findByWorkflow(Long workflowId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT s FROM State s WHERE s.workflow.id = :wid ORDER BY s.id",
                            State.class)
                    .setParameter("wid", workflowId)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}