package org.workflow.engine.domain.dto;

import org.workflow.engine.domain.enums.IssuePriority;

/* Lightweight view of an Issue for list screens.
   Avoids loading the full entity and its relationships.
   Hence, use records.
*/
public record IssueSummary(
        String issueKey,
        String summary,
        IssuePriority priority,
        String status,
        String assigneeName
) {}
