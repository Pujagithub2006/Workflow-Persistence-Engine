package org.workflow.engine.repository;

import org.workflow.engine.domain.enums.UserRole;
import org.workflow.engine.domain.model.User;
import org.workflow.engine.domain.valueobject.Email;
import org.workflow.engine.persistence.JpaUtil;
import org.workflow.engine.persistence.repositories.UserRepository;
import org.workflow.engine.testutil.TestCleanup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class UserRepositoryTest {

    private final UserRepository userRepo = new UserRepository();

    @BeforeEach
    void clean() {
        TestCleanup.all();
    }

    @AfterAll
    static void shutdown() {
        JpaUtil.shutdown();
    }

    @Test
    void saveInsertsNewUserAndGeneratesId() {
        User saved = userRepo.save(new User("alice",
                new Email("alice@test.com"), "Alice", UserRole.DEVELOPER));

        assertThat(saved.getUserId()).isNotNull();
        assertThat(saved.getVersion()).isZero();
    }

    @Test
    void saveUpdatesExistingUser() {
        User user = userRepo.save(new User("bob",
                new Email("bob@test.com"), "Bob", UserRole.DEVELOPER));

        user.setDisplayName("Robert");
        User updated = userRepo.save(user);

        User loaded = userRepo.findById(user.getUserId()).orElseThrow();
        assertThat(loaded.getDisplayName()).isEqualTo("Robert");
        assertThat(loaded.getVersion()).isGreaterThan(updated.getVersion() - 1);
    }

    @Test
    void findByIdReturnsUser() {
        User saved = userRepo.save(new User("carol",
                new Email("carol@test.com"), "Carol", UserRole.TESTER));

        assertThat(userRepo.findById(saved.getUserId())).isPresent();
    }

    @Test
    void findByIdReturnsEmptyForMissing() {
        assertThat(userRepo.findById(99999L)).isEmpty();
    }

    @Test
    void findByUsernameReturnsUser() {
        userRepo.save(new User("dave",
                new Email("dave@test.com"), "Dave", UserRole.DEVELOPER));

        assertThat(userRepo.findByUsername("dave")).isPresent();
        assertThat(userRepo.findByUsername("missing")).isEmpty();
    }

    @Test
    void findAllReturnsOrderedByUsername() {
        userRepo.save(new User("zara", new Email("z@test.com"), "Zara", UserRole.DEVELOPER));
        userRepo.save(new User("adam", new Email("a@test.com"), "Adam", UserRole.DEVELOPER));

        List<User> all = userRepo.findAll();
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getUsername()).isEqualTo("adam");
    }

    @Test
    void deleteRemovesUser() {
        User saved = userRepo.save(new User("ghost",
                new Email("ghost@test.com"), "Ghost", UserRole.VIEWER));

        userRepo.delete(saved.getUserId());

        assertThat(userRepo.findById(saved.getUserId())).isEmpty();
    }

    @Test
    void duplicateUsernameFails() {
        userRepo.save(new User("unique",
                new Email("unique1@test.com"), "Unique", UserRole.DEVELOPER));

        assertThatThrownBy(() -> userRepo.save(new User("unique",
                new Email("unique2@test.com"), "Duplicate", UserRole.DEVELOPER)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void duplicateEmailFails() {
        userRepo.save(new User("userA",
                new Email("shared@test.com"), "A", UserRole.DEVELOPER));

        assertThatThrownBy(() -> userRepo.save(new User("userB",
                new Email("shared@test.com"), "B", UserRole.DEVELOPER)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void invalidEmailFails() {
        assertThatThrownBy(() -> new Email("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void versionIncrementsOnUpdate() {
        User user = userRepo.save(new User("versioned",
                new Email("v@test.com"), "Versioned", UserRole.DEVELOPER));
        Long v0 = user.getVersion();

        user.setDisplayName("Updated");
        User updated = userRepo.save(user);

        assertThat(updated.getVersion()).isGreaterThan(v0);
    }
}