package org.workflow.engine.persistence.repositories;

import org.workflow.engine.domain.model.Issue;
import org.workflow.engine.domain.valueobject.IssueKey;
import org.workflow.engine.persistence.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class IssueRepository {

    public Issue save(Issue issue) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Issue merged = session.merge(issue);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw new RuntimeException("Failed to save issue", e);
        }
    }

    public Optional<Issue> findByKey(String key) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Issue WHERE issueKey = :k", Issue.class)
                    .setParameter("k", new IssueKey(key))
                    .uniqueResultOptional();
        }
    }

    public List<Issue> findByProject(Long projectId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Issue WHERE project.id = :pid ORDER BY id DESC", Issue.class)
                    .setParameter("pid", projectId)
                    .list();
        }
    }

    public void delete(Long id) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Issue i = session.get(Issue.class, id);
            if (i != null) session.remove(i);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw new RuntimeException("Failed to delete issue", e);
        }
    }
}