package org.workflow.engine.testutil;

import org.workflow.engine.persistence.TransactionManager;

public class TestCleanup {

    public static void all() {
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
}