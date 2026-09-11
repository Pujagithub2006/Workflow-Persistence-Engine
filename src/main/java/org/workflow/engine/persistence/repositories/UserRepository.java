package org.workflow.engine.persistence.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.workflow.engine.domain.model.User;
import org.workflow.engine.persistence.JpaUtil;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    public User save(User user) {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction etx = em.getTransaction();

        try {
            etx.begin();

            User merged = em.merge(user);

            etx.commit();

            logger.info("Saved user: {}", merged.getUsername());
            return merged;

        } catch (Exception e) {
            if(etx.isActive()) etx.rollback();
            throw new RuntimeException("Failed to save user", e);

        } finally {
            em.close();
        }
    }

    public Optional<User> findById(Long id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(User.class, id));
        } finally {
            em.close();
        }
    }

    public Optional<User> findByUsername(String username) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT u FROM User u WHERE u.username = :u", User.class)
                    .setParameter("u", username)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }

    public List<User> findAll() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u ORDER BY u.username", User.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void delete(Long id) {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction etx = em.getTransaction();

        try {
            etx.begin();

            User user = em.find(User.class, id);

            if(user!=null) em.remove(user);

            etx.commit();

            logger.info("Deleted user: {}", id);

        } catch (Exception e) {
            if (etx.isActive()) etx.rollback();
            throw new RuntimeException("Failed to delete user", e);
        } finally {
            em.close();
        }
    }
}
