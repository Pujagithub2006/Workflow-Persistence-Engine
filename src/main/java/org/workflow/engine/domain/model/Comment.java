package org.workflow.engine.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Comment() {

    }

    public Comment(String commentContent, User author, Issue issue) {
        if (commentContent == null || commentContent.isBlank()) {
            throw new IllegalArgumentException("Comment content cannot be null or empty");
        }
        if (author == null) {
            throw new IllegalArgumentException("Author cannot be null");
        }
        if (issue == null) {
            throw new IllegalArgumentException("Issue cannot be null");
        }
        this.content = commentContent;
        this.author = author;
        this.issue = issue;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onPersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public void updateContent(String newContent) {
        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        this.content = newContent;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public User getAuthor() {
        return author;
    }

    public Issue getIssue() {
        return issue;
    }
    public void setIssue(Issue issue) {
        this.issue =  issue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        return Objects.equals(id, comment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Comment{author=" + author.getUsername() + ", contents='" + content.substring(0, Math.min(30, content.length())) + "...'}";
    }

}
