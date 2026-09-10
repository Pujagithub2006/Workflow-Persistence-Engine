DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS issues CASCADE;
DROP TABLE IF EXISTS transitions CASCADE;
DROP TABLE IF EXISTS states CASCADE;
DROP TABLE IF EXISTS workflows CASCADE;
DROP TABLE IF EXISTS project_team CASCADE;
DROP TABLE IF EXISTS projects CASCADE;
DROP TABLE IF EXISTS workspace_members CASCADE;
DROP TABLE IF EXISTS workspaces CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       display_name VARCHAR(100),
                       role VARCHAR(20) NOT NULL,
                       active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE workspaces (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(100) UNIQUE NOT NULL,
                            description TEXT,
                            owner_id INTEGER NOT NULL REFERENCES users(id)
);

CREATE TABLE workspace_members (
                                   workspace_id INTEGER REFERENCES workspaces(id) ON DELETE CASCADE,
                                   user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
                                   PRIMARY KEY (workspace_id, user_id)
);

CREATE TABLE projects (
                          id SERIAL PRIMARY KEY,
                          key VARCHAR(10) UNIQUE NOT NULL,
                          name VARCHAR(100) NOT NULL,
                          description TEXT,
                          workspace_id INTEGER REFERENCES workspaces(id),
                          project_lead_id INTEGER REFERENCES users(id),
                          next_issue_number BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE project_team (
                              project_id INTEGER REFERENCES projects(id) ON DELETE CASCADE,
                              user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
                              PRIMARY KEY (project_id, user_id)
);

CREATE TABLE workflows (
                           id SERIAL PRIMARY KEY,
                           name VARCHAR(100) UNIQUE NOT NULL,
                           description TEXT
);

CREATE TABLE states (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(50) NOT NULL,
                        description TEXT,
                        status VARCHAR(20) NOT NULL,
                        workflow_id INTEGER NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
                        is_initial BOOLEAN NOT NULL DEFAULT FALSE,
                        UNIQUE (workflow_id, name)
);

CREATE TABLE transitions (
                             id SERIAL PRIMARY KEY,
                             name VARCHAR(50) NOT NULL,
                             description TEXT,
                             from_state_id INTEGER NOT NULL REFERENCES states(id) ON DELETE CASCADE,
                             to_state_id INTEGER NOT NULL REFERENCES states(id) ON DELETE CASCADE,
                             UNIQUE (from_state_id, to_state_id)
);

CREATE TABLE issues (
                        id SERIAL PRIMARY KEY,
                        issue_key VARCHAR(20) UNIQUE NOT NULL,
                        summary VARCHAR(500) NOT NULL,
                        description TEXT,
                        reporter_id INTEGER NOT NULL REFERENCES users(id),
                        assignee_id INTEGER REFERENCES users(id),
                        issue_type VARCHAR(20) NOT NULL,
                        priority VARCHAR(20) NOT NULL,
                        state_id INTEGER NOT NULL REFERENCES states(id),
                        project_id INTEGER NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        resolved_at TIMESTAMP
);

CREATE TABLE comments (
                          id SERIAL PRIMARY KEY,
                          content TEXT NOT NULL,
                          issue_id INTEGER NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
                          author_id INTEGER NOT NULL REFERENCES users(id),
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
                            id SERIAL PRIMARY KEY,
                            issue_id INTEGER NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
                            action VARCHAR(255) NOT NULL,
                            performed_by_id INTEGER REFERENCES users(id),
                            field VARCHAR(50),
                            timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);