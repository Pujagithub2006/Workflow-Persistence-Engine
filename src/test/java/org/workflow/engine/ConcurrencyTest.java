package org.workflow.engine;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.workflow.engine.domain.enums.UserRole;
import org.workflow.engine.domain.model.User;
import org.workflow.engine.domain.valueobject.Email;
import org.workflow.engine.persistence.JpaUtil;
import org.workflow.engine.persistence.repositories.UserRepository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

public class ConcurrencyTest {
    private final UserRepository userRepo = new UserRepository();

    @BeforeEach
    void clean() {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction etx = em.getTransaction();
        try {
            etx.begin();
            em.createQuery("DELETE FROM User").executeUpdate();
            etx.commit();
        } catch (Exception e) {
            if (etx.isActive()) etx.rollback();
        } finally {
            em.close();
        }
    }

    @AfterAll
    static void shutdown() {
        JpaUtil.shutdown();
    }

    // ---------- Concurrent inserts: all succeed ----------
    @Test
    void concurrentInsertsWithDistinctUsernamesAllSucceed() throws InterruptedException {
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();

        for(int i=0; i<threads; i++) {
            final int id = i;

            pool.submit(()->{
                try {
                    userRepo.save(new User("user" + id, new Email("user" + id + "@test.com"), "User " + id, UserRole.DEVELOPER));
                    successes.incrementAndGet();
                } catch(Exception e) {
                    // shouldn't happen with distinct usernames
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(successes.get()).isEqualTo(threads);
        assertThat(userRepo.findAll()).hasSize(threads);
    }


    // ---------- Concurrent updates: only one wins (optimistic lock) ----------
    @Test
    void optimisticLockDetectsConcurrentUpdate() {
        // 1. create a user
        User user = userRepo.save(new User("conflict",
                new Email("conflict@test.com"), "Original", UserRole.DEVELOPER));

        // 2. load the same row in two separate EntityManagers
        EntityManager em1 = JpaUtil.getEntityManager();
        EntityManager em2 = JpaUtil.getEntityManager();
        EntityTransaction etx1 = em1.getTransaction();
        EntityTransaction etx2 = em2.getTransaction();

        try {
            etx1.begin();
            etx2.begin();

            User u1 = em1.find(User.class, user.getUserId());
            User u2 = em2.find(User.class, user.getUserId());

            // both see same version
            assertThat(u1.getVersion()).isEqualTo(u2.getVersion());

            // transaction 1 wins
            u1.setDisplayName("From ETX1");
            etx1.commit();

            // transaction 2 tries to commit stale data — fails
            u2.setDisplayName("From ETX2");
            assertThatThrownBy(etx2::commit).isInstanceOf(Exception.class);

        } finally {
            if (etx1.isActive()) etx1.rollback();
            if (etx2.isActive()) etx2.rollback();
            em1.close();
            em2.close();
        }

        // verify: ETX1's write is what persisted
        User finalUser = userRepo.findById(user.getUserId()).orElseThrow();
        assertThat(finalUser.getDisplayName()).isEqualTo("From ETX1");
    }

    // ---------- Retry pattern for optimistic conflicts ----------
    @Test
    void retryAfterOptimisticConflictEventuallySucceeds() throws InterruptedException {
        User user = userRepo.save(new User("retry", new Email("retry@test.com"), "Original", UserRole.DEVELOPER));

        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();

        for(int i=0; i<threads; i++) {
            final int id = i;

            pool.submit(()->{
                int attempts = 0;

                while(attempts<3) {
                    try {
                        User fresh = userRepo.findById(user.getUserId()).orElseThrow();
                        fresh.setDisplayName("From thread " + id);
                        userRepo.save(fresh);
                        successes.incrementAndGet();
                        break; // now next thread

                    } catch (Exception e) {
                        attempts++;

                        try {
                            Thread.sleep(20);

                        } catch (InterruptedException ignored) {

                        }
                    }
                }

                latch.countDown();
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        // all threads eventually succeed thanks to retry
        assertThat(successes.get()).isEqualTo(threads);

        User finalUser = userRepo.findById(user.getUserId()).orElseThrow();
        assertThat(finalUser.getDisplayName()).startsWith("From thread ");
    }
}
