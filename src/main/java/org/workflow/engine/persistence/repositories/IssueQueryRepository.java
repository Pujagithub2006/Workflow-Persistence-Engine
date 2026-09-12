package org.workflow.engine.persistence.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.workflow.engine.domain.dto.IssueSummary;
import org.workflow.engine.domain.enums.IssuePriority;
import org.workflow.engine.domain.enums.IssueStatus;
import org.workflow.engine.domain.model.Issue;
import org.workflow.engine.persistence.JpaUtil;

import java.util.ArrayList;
import java.util.List;

public class IssueQueryRepository {

    // JPQL — basic filters
    public List<Issue> findByPriority(IssuePriority priority) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT i FROM Issue i WHERE i.priority = :p ORDER BY i.id DESC",
                            Issue.class)
                    .setParameter("p", priority)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Issue> findByProjectAndStatus(Long projectId, IssueStatus status) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT i FROM Issue i " +
                                    "WHERE i.project.id = :pid AND i.currentState.status = :s " +
                                    "ORDER BY i.priority DESC, i.id",
                            Issue.class)
                    .setParameter("pid", projectId)
                    .setParameter("s", status)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long countByProjectAndPriority(Long projectId, IssuePriority priority) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT COUNT(i) FROM Issue i " +
                                    "WHERE i.project.id = :pid AND i.priority = :p",
                            Long.class)
                    .setParameter("pid", projectId)
                    .setParameter("p", priority)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    // JPQL — aggregate
    // returns [status, count] rows.
    public List<Object[]> countByStatusInProject(Long projectId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT i.currentState.status, COUNT(i) " +
                                    "FROM Issue i " +
                                    "WHERE i.project.id = :pid " +
                                    "GROUP BY i.currentState.status",
                            Object[].class)
                    .setParameter("pid", projectId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // JPQL — constructor projection
    public List<IssueSummary> findSummariesByProject(Long projectId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT new com.workflow.persistence.domain.dto.IssueSummary(" +
                                    "   i.issueKey, i.summary, i.priority, s.status, " +
                                    "   COALESCE(a.username, 'Unassigned')) " +
                                    "FROM Issue i " +
                                    "JOIN i.currentState s " +
                                    "LEFT JOIN i.assignee a " +
                                    "WHERE i.project.id = :pid " +
                                    "ORDER BY i.priority DESC, i.id",
                            IssueSummary.class)
                    .setParameter("pid", projectId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // pagination with whitelisted sort
    public List<Issue> findByProjectPaginated(Long projectId, int page, int size, String sortField, boolean ascending) {
        EntityManager em = JpaUtil.getEntityManager();

        try {
            String safeField = whiteListSortField(sortField);
            String direction = ascending ? "ASC" : "DESC";

            return em.createQuery(
                            "SELECT i FROM Issue i " +
                                    "WHERE i.project.id = :pid " +
                                    "ORDER BY i." + safeField + " " + direction,
                            Issue.class)
                    .setParameter("pid", projectId)
                    .setFirstResult(page*size)
                    .setMaxResults(size)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long countByProject(Long projectId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT COUNT(i) FROM Issue i WHERE i.project.id = :pid",
                            Long.class)
                    .setParameter("pid", projectId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    /*
     Whitelist of columns that can be used in ORDER BY.
     NEVER concatenate user input into JPQL — that's SQL injection.
    */
    private String whiteListSortField(String field) {
        return switch(field) {
            case "issueKey", "summary", "priority", "createdAt" -> field;
            default -> "id";
        };
    }

    // Criteria API — dynamic filtering
    /*
     Build a query from optional filters.
     Only use Criteria when filters are truly optional/dynamic.
    */
    public List<Issue> search(Long projectId, IssuePriority priority, IssueStatus status, Long assigneeId) {
        EntityManager em = JpaUtil.getEntityManager();

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Issue> cq = cb.createQuery(Issue.class);
            Root<Issue> root = cq.from(Issue.class);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("project").get("id"), projectId));

            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("currentState").get("status"), status));
            }
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
            }

            cq.where(predicates.toArray(new Predicate[0]));
            cq.orderBy(cb.desc(root.get("id")));

            return em.createQuery(cq).getResultList();

        } finally {
            em.close();
        }
    }
}
