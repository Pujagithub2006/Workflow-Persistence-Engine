package org.workflow.engine;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.workflow.engine.domain.enums.UserRole;
import org.workflow.engine.domain.model.User;
import org.workflow.engine.domain.valueobject.Email;
import org.workflow.engine.persistence.JpaUtil;
import org.workflow.engine.persistence.TransactionManager;
import org.workflow.engine.persistence.repositories.UserRepository;

import static org.assertj.core.api.Assertions.*;

public class TransactionTest {
    private final UserRepository userRepo = new UserRepository();

    @BeforeEach
    void clean() {
        TransactionManager.inTransaction(em -> {
            em.createQuery("DELETE FROM AuditLog").executeUpdate();
            em.createQuery("DELETE FROM Comment").executeUpdate();
            em.createQuery("DELETE FROM Issue").executeUpdate();
            em.createQuery("DELETE FROM Transition").executeUpdate();
            em.createQuery("DELETE FROM State").executeUpdate();
            em.createQuery("DELETE FROM Workflow").executeUpdate();
            em.createQuery("DELETE FROM Project").executeUpdate();
            em.createQuery("DELETE FROM Workspace").executeUpdate();
            em.createQuery("DELETE FROM User").executeUpdate();

            return "Database cleaned!";
        });
    }

    @AfterAll
    static void shutdown() {
        JpaUtil.shutdown();
    }

    // ---------- Commit ----------

    @Test
    void commitPersistsChanges() {
        User user = TransactionManager.inTransaction(em -> {
            User u = new User("commit",
                    new Email("commit@test.com"), "Commit", UserRole.DEVELOPER);
            em.persist(u);
            return u;
        });

        assertThat(userRepo.findById(user.getUserId())).isPresent();
    }

    // ---------- Rollback on exception ----------

    @Test
    void rollbackOnExceptionDiscardsChanges() {
        // pre-create one user
        userRepo.save(new User("existing",
                new Email("existing@test.com"), "Existing", UserRole.DEVELOPER));

        // attempt to create a duplicate username — the unique constraint fails
        assertThatThrownBy(() ->
                TransactionManager.inTransaction(em -> {
                    User dup = new User("existing",
                            new Email("different@test.com"), "Dup", UserRole.DEVELOPER);
                    em.persist(dup);
                    return dup;
                })
        ).isInstanceOf(RuntimeException.class);

        // only the original exists
        assertThat(userRepo.findAll()).hasSize(1);
    }

    // ---------- Atomicity across multiple operations ----------

    @Test
    void multipleOperationsAreAtomic() {
        // pre-create one user
        userRepo.save(new User("base",
                new Email("base@test.com"), "Base", UserRole.DEVELOPER));

        // attempt two inserts where the second fails
        assertThatThrownBy(() ->
                TransactionManager.inTransaction(em -> {
                    em.persist(new User("good",
                            new Email("good@test.com"), "Good", UserRole.DEVELOPER));
                    em.persist(new User("base",  // duplicate username — fails
                            new Email("base2@test.com"), "Base2", UserRole.DEVELOPER));
                    return "Multiple operations persisted!";
                })
        ).isInstanceOf(RuntimeException.class);

        // neither the "good" nor the "base2" user exists
        assertThat(userRepo.findAll()).hasSize(1);
        assertThat(userRepo.findByUsername("good")).isEmpty();
    }

    // ---------- Explicit rollback ----------

    @Test
    void explicitRollbackDiscardsChanges() {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction etx = em.getTransaction();
        try {
            etx.begin();
            em.persist(new User("rolled",
                    new Email("rolled@test.com"), "Rolled", UserRole.DEVELOPER));
            etx.rollback();
        } finally {
            em.close();
        }

        assertThat(userRepo.findByUsername("rolled")).isEmpty();
    }

    // ---------- Flush without commit ----------
    @Test
    void flushSendsSqlButRollbackUndoesIt() {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction etx = em.getTransaction();

        try {
            etx.begin();
            User user = new User("flushed", new Email("flushed@test.com"), "Flushed", UserRole.DEVELOPER);
            em.persist(user);

            em.flush(); // SQL sent to DB — but transaction still open

            assertThat(em.find(User.class, user.getUserId())).isNotNull(); // same session sees the entity

            etx.rollback(); // undo
        } finally {
            em.close();
        }

        assertThat(userRepo.findByUsername("flushed")).isEmpty(); // another session cannot find it now
    }

    // ---------- Transaction isolation from other EntityManagers
    @Test
    void uncommittedChangesAreInvisibleToOtherSessions() {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction etx = em.getTransaction();
        try {
            etx.begin();
            User user = new User("isolation",
                    new Email("isolation@test.com"), "Isolation", UserRole.DEVELOPER);
            em.persist(user);
            em.flush();  // SQL sent

            // another EntityManager can't see it yet
            assertThat(userRepo.findByUsername("isolation")).isEmpty();

            etx.commit();
        } finally {
            em.close();
        }

        // now visible
        assertThat(userRepo.findByUsername("isolation")).isPresent();
    }

    // ---------- Pessimistic locking ----------
    @Test
    void pessimisticWriteLockAcquiresAndReleases() {
        User user = userRepo.save(new User("locked",
                new Email("locked@test.com"), "Locked", UserRole.DEVELOPER));

        // lock, modify, release
        User updated = TransactionManager.inTransaction(em -> {
            User managed = em.find(User.class, user.getUserId(),
                    jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            managed.setDisplayName("Locked Update");
            return managed;
        });

        assertThat(updated.getDisplayName()).isEqualTo("Locked Update");

        User reloaded = userRepo.findById(user.getUserId()).orElseThrow();
        assertThat(reloaded.getDisplayName()).isEqualTo("Locked Update");
    }
}
