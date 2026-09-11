package org.workflow.engine.persistence.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.workflow.engine.domain.model.User;
import org.workflow.engine.persistence.JpaUtil;
import org.workflow.engine.persistence.TransactionManager;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    // load user with a pessimistic write lock (other writers block until this transaction commits).
    public Optional<User> findByIdForUpdate(Long id) {
        return TransactionManager.inTransaction(em->
           Optional.ofNullable(em.find(User.class, id, LockModeType.PESSIMISTIC_WRITE))
        );
    }

    public User save(User user) {
        return TransactionManager.inTransaction(em->{
            User merged = em.merge(user);
            logger.info("Saved user: {}", merged.getUsername());
            return merged;
        });
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
        TransactionManager.inTransaction(em->{
            User user = em.find(User.class, id);
            if (user != null) em.remove(user);
            logger.info("Deleted user: {}", id);
            return "User deleted!";
        });
    }
}
