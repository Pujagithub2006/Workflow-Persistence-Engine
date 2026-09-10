package org.workflow.engine.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Comment {

    private Long id;
    private String content;
    private User author;
    private Issue issue;
    private LocalDateTime createdAt;

    public Comment(String commentContent, User author, Issue issue) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Comment content cannot be null or empty");
        }
        if (author == null) {
            throw new IllegalArgumentException("Author cannot be null");
        }
        if (issue == null) {
            throw new IllegalArgumentException("Issue cannot be null");
        }
        this.content = content;
        this.author = author;
        this.issue = issue;
        this.createdAt = LocalDateTime.now();
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
