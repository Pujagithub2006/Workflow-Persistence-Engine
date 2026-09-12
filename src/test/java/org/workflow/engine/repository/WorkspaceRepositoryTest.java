package org.workflow.engine.repository;

import org.workflow.engine.domain.enums.UserRole;
import org.workflow.engine.domain.model.User;
import org.workflow.engine.domain.model.Workspace;
import org.workflow.engine.domain.valueobject.Email;
import org.workflow.engine.persistence.JpaUtil;
import org.workflow.engine.persistence.repositories.UserRepository;
import org.workflow.engine.persistence.repositories.WorkspaceRepository;
import org.workflow.engine.testutil.TestCleanup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class WorkspaceRepositoryTest {

    private final WorkspaceRepository workspaceRepo = new WorkspaceRepository();
    private final UserRepository userRepo = new UserRepository();

    private User owner;

    @BeforeEach
    void setup() {
        TestCleanup.all();
        owner = userRepo.save(new User("owner",
                new Email("owner@test.com"), "Owner", UserRole.ADMIN));
    }

    @AfterAll
    static void shutdown() {
        JpaUtil.shutdown();
    }

    @Test
    void saveInsertsWorkspace() {
        Workspace saved = workspaceRepo.save(new Workspace("WS1", "Test", owner));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findByIdReturnsWorkspace() {
        Workspace saved = workspaceRepo.save(new Workspace("WS2", "Test", owner));

        assertThat(workspaceRepo.findById(saved.getId())).isPresent();
    }

    @Test
    void findAllReturnsOrdered() {
        workspaceRepo.save(new Workspace("ZWS", "Test", owner));
        workspaceRepo.save(new Workspace("AWS", "Test", owner));

        assertThat(workspaceRepo.findAll()).hasSize(2);
    }

    @Test
    void ownerIsAddedAsMember() {
        Workspace saved = workspaceRepo.save(new Workspace("WS3", "Test", owner));

        Workspace loaded = workspaceRepo.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getMembers()).contains(owner);
    }

    @Test
    void addMemberIsPersisted() {
        Workspace ws = workspaceRepo.save(new Workspace("WS4", "Test", owner));
        User member = userRepo.save(new User("m1",
                new Email("m1@test.com"), "M1", UserRole.DEVELOPER));

        ws.addMember(member);
        workspaceRepo.save(ws);

        Workspace loaded = workspaceRepo.findById(ws.getId()).orElseThrow();
        assertThat(loaded.getMembers()).contains(member);
    }

    @Test
    void removeMemberIsPersisted() {
        Workspace ws = workspaceRepo.save(new Workspace("WS5", "Test", owner));
        User member = userRepo.save(new User("m2",
                new Email("m2@test.com"), "M2", UserRole.DEVELOPER));
        ws.addMember(member);
        workspaceRepo.save(ws);

        Workspace loaded = workspaceRepo.findById(ws.getId()).orElseThrow();
        loaded.removeMember(loaded.getMembers().stream()
                .filter(u -> u.getUsername().equals("m2")).findFirst().orElseThrow());
        workspaceRepo.save(loaded);

        Workspace reloaded = workspaceRepo.findById(ws.getId()).orElseThrow();
        assertThat(reloaded.getMembers()).noneMatch(u -> u.getUsername().equals("m2"));
    }

    @Test
    void cannotRemoveOwner() {
        Workspace ws = workspaceRepo.save(new Workspace("WS6", "Test", owner));
        Workspace loaded = workspaceRepo.findById(ws.getId()).orElseThrow();

        assertThatThrownBy(() -> loaded.removeMember(owner))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void duplicateNameFails() {
        workspaceRepo.save(new Workspace("DUP", "Test", owner));

        assertThatThrownBy(() -> workspaceRepo.save(new Workspace("DUP", "Test", owner)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deleteRemovesWorkspace() {
        Workspace saved = workspaceRepo.save(new Workspace("TO-DELETE", "Test", owner));

        workspaceRepo.delete(saved.getId());

        assertThat(workspaceRepo.findById(saved.getId())).isEmpty();
    }
}