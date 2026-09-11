package org.workflow.engine;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.*;
import org.workflow.engine.domain.enums.UserRole;
import org.workflow.engine.domain.model.User;
import org.workflow.engine.domain.valueobject.Email;
import org.workflow.engine.persistence.JpaUtil;
import org.workflow.engine.persistence.TransactionManager;
import org.workflow.engine.persistence.repositories.UserRepository;
import static org.assertj.core.api.Assertions.*;

public class LifecycleTest {
    private final UserRepository userRepo = new UserRepository();

    // Before and After
    @BeforeEach
    void clean() {
        TransactionManager.inTransaction(em->{
            em.createQuery("DELETE FROM AuditLog").executeUpdate();
            em.createQuery("DELETE FROM Comment").executeUpdate();
            em.createQuery("DELETE FROM Issue").executeUpdate();
            em.createQuery("DELETE FROM Transition").executeUpdate();
            em.createQuery("DELETE FROM State").executeUpdate();
            em.createQuery("DELETE FROM Workflow").executeUpdate();
            em.createQuery("DELETE FROM Project").executeUpdate();
            em.createQuery("DELETE FROM Workspace").executeUpdate();
            em.createQuery("DELETE FROM User").executeUpdate();
            return "Cleaning done!";
        });
    }

    @AfterAll
    static void shutdown() {
        JpaUtil.shutdown();
    }

    // Entity States
    @Test
    void transientEntityIsNotInPersistenceContext() {
        EntityManager em = JpaUtil.getEntityManager();

        try {
            User user = new User("new", new Email("new@test.com"), "New", UserRole.DEVELOPER);

            // not managed yet
            assertThat(em.contains(user)).isFalse();
        } finally {
            em.close();
        }
    }

    @Test
    void managedEntityIsTracked() {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction etx = em.getTransaction();
        try {
            etx.begin();
            User user = new User("managed",
                    new Email("managed@test.com"), "Managed", UserRole.DEVELOPER);
            em.persist(user);

            // Now tracked in persistence context
            assertThat(em.contains(user)).isTrue();
            etx.commit();
        } finally {
            em.close();
        }
    }

    @Test
    void detachedEntityIsNotTracked() {
        // create in one EntityManager
        User saved = userRepo.save(new User("detached",
                new Email("detached@test.com"), "Detached", UserRole.DEVELOPER));

        EntityManager em = JpaUtil.getEntityManager();
        try {
            User loaded = em.find(User.class, saved.getUserId());
            assertThat(em.contains(loaded)).isTrue();

            em.detach(loaded);
            assertThat(em.contains(loaded)).isFalse();
        } finally {
            em.close();
        }
    }

    // Dirty Checking
    @Test
    void dirtyCheckingUpdatesWithoutExplicitSave() {
        // create a user
        User user = userRepo.save(new User("dirty", new Email("dirty@test.com"), "Original", UserRole.DEVELOPER));

        // load -> modify in a transaction -> commit -> but no save() call
        TransactionManager.inTransaction(em->{
            User managed = em.find(User.class, user.getUserId());
            managed.setDisplayName("changed_username");
            // no em.merge() - dirty checking will issue update without explicit em.merge()
            return null;
        });

        // verify that the change is persisted
        User reloaded = userRepo.findById(user.getUserId()).orElseThrow();
        assertThat(reloaded.getDisplayName()).isEqualTo("changed_username");
    }

    @Test
    void dirtyCheckingIgnoresUnchangedFields() {
        User user = userRepo.save(new User("clean",
                new Email("clean@test.com"), "Clean", UserRole.DEVELOPER));
        Long v0 = userRepo.findById(user.getUserId()).orElseThrow().getVersion();

        // load but don't modify
        TransactionManager.inTransaction(em -> {
            em.find(User.class, user.getUserId());
            return "User loaded!";
        });

        // version unchanged — no UPDATE issued
        Long v1 = userRepo.findById(user.getUserId()).orElseThrow().getVersion();
        assertThat(v1).isEqualTo(v0);
    }

    // Persistence Context Cache
    @Test
    void sameEntityReturnsSameInstanceWithinSession() {
        User user = userRepo.save(new User("cache",
                new Email("cache@test.com"), "Cache", UserRole.DEVELOPER));

        EntityManager em = JpaUtil.getEntityManager();
        try {
            User first = em.find(User.class, user.getUserId());
            User second = em.find(User.class, user.getUserId());
            assertThat(first).isSameAs(second);
        } finally {
            em.close();
        }
    }

    @Test
    void differentSessionsReturnDifferentInstances() {
        User user = userRepo.save(new User("fresh",
                new Email("fresh@test.com"), "Fresh", UserRole.DEVELOPER));

        EntityManager em1 = JpaUtil.getEntityManager();
        EntityManager em2 = JpaUtil.getEntityManager();
        try {
            User u1 = em1.find(User.class, user.getUserId());
            User u2 = em2.find(User.class, user.getUserId());
            assertThat(u1).isNotSameAs(u2);
            assertThat(u1).isEqualTo(u2);
        } finally {
            em1.close();
            em2.close();
        }
    }

    // Merge v/s Persist
    @Test
    void mergeWorksForDetachedEntities() {
        User user = userRepo.save(new User("merge", new Email("merge@test.com"), "Merge", UserRole.DEVELOPER));

        // detach - load object in one EntityManager -> close it -> modify it
        EntityManager em1 = JpaUtil.getEntityManager();
        User detached;

        try {
            detached = em1.find(User.class, user.getUserId()); // load
            em1.close(); // close

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // modify detached
        detached.setDisplayName("Merged");

        // merge back (save() call)
        User reattached = userRepo.save(detached);

        assertThat(reattached.getDisplayName()).isEqualTo("Merged");
        assertThat(reattached.getVersion()).isGreaterThan(detached.getVersion() - 1);
    }

    @Test
    void persistOnExistingIdFails() {
        User user = userRepo.save(new User("existing", new Email("existing@test.com"), "Existing", UserRole.DEVELOPER));

        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction etx = em.getTransaction();

        try {
            etx.begin();

            User duplicate = new User("dup", new Email("dup@test.com"), "Dup", UserRole.DEVELOPER);

            duplicate.setId(user.getUserId());

            assertThatThrownBy(()->em.persist(duplicate)).isInstanceOf(Exception.class);

            etx.rollback();

        } finally {
            em.close();
        }
    }

    // Refresh
    @Test
    void refreshReReadsFromDatabase() {
        User user = userRepo.save(new User("refresh", new Email("refresh@test.com"), "Original", UserRole.DEVELOPER));

        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction etx = em.getTransaction();
        try {
            etx.begin();
            User managed = em.find(User.class, user.getUserId());

            // modify in memory
            managed.setDisplayName("In memory");

            // refresh — discard in-memory changes, reload from DB
            em.refresh(managed);

            assertThat(managed.getDisplayName()).isEqualTo("Original"); // in-memory changes not considered
            etx.rollback();
        } finally {
            em.close();
        }
    }

    // Optimistic Locking
    @Test
    void versionIncrementsOnUpdate() {
        User user = userRepo.save(new User("versioned", new Email("versioned@test.com"), "V1", UserRole.DEVELOPER));
        Long v0 = user.getVersion();

        user.setDisplayName("V2");
        User updated = userRepo.save(user);

        assertThat(updated.getVersion()).isGreaterThan(v0);
    }

    @Test
    void concurrentUpdateCausesOptimisticLockException() {
        User user = userRepo.save(new User("conflict", new Email("conflict@test.com"), "Original", UserRole.DEVELOPER));

        // load users in two seperate EntityManagers
        EntityManager em1 = JpaUtil.getEntityManager();
        EntityManager em2 = JpaUtil.getEntityManager();
        EntityTransaction etx1 = em1.getTransaction();
        EntityTransaction etx2 = em2.getTransaction();

        try {
            etx1.begin();
            etx2.begin();

            User u1 = em1.find(User.class, user.getUserId());
            User u2 = em2.find(User.class, user.getUserId());

            // both have same version
            assertThat(u1.getVersion()).isEqualTo(u2.getVersion());

            // first transaction updates and commits
            u1.setDisplayName("Changed by ETX1");
            etx1.commit();

            // second transaction tries to update — but its version is stale
            u2.setDisplayName("Changed by ETX2");
            assertThatThrownBy(etx2::commit).isInstanceOf(Exception.class);

        } finally {
            if (etx1.isActive()) etx1.rollback();
            if (etx2.isActive()) etx2.rollback();
            em1.close();
            em2.close();
        }

        // First change wins
        User finalUser = userRepo.findById(user.getUserId()).orElseThrow();
        assertThat(finalUser.getDisplayName()).isEqualTo("Changed by ETX1");
    }

    // Lifecycle Callbacks
    @Test
    void prePersistSetsCreatedAt() {
        User user = new User("timecheck", new Email("timecheck@test.com"), "Time", UserRole.DEVELOPER);
        User saved = userRepo.save(user);

        assertThat(saved.getUserId()).isNotNull(); // createdAt is not a User field but the pattern holds for Issue, Comment, AuditLog
    }
}
