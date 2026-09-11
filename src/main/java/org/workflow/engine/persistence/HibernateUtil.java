package org.workflow.engine.persistence;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.workflow.engine.domain.model.*;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static final Logger logger = LoggerFactory.getLogger(HibernateUtil.class);
    private static final SessionFactory sessionFactory = build();

    private static SessionFactory build() {
        try {
            return new Configuration()
                    .configure()
                    .addAnnotatedClass(User.class)
                    .addAnnotatedClass(Workspace.class)
                    .addAnnotatedClass(Project.class)
                    .addAnnotatedClass(Workflow.class)
                    .addAnnotatedClass(State.class)
                    .addAnnotatedClass(Transition.class)
                    .addAnnotatedClass(Issue.class)
                    .addAnnotatedClass(Comment.class)
                    .addAnnotatedClass(AuditLog.class)
                    .buildSessionFactory();
        } catch (Exception e) {
            logger.error("Failed to build Session Factory", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        if(sessionFactory != null) {
            sessionFactory.close();
        }
    }
}
