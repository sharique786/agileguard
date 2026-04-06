package com.agileguard.jira.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A resolved JIRA Epic used for the Epic Link field. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JiraEpic {
    private String issueKey;   // "COMMSSURV-10"
    private String name;       // Epic name / summary
    private String color;      // colour label from JIRA (optional)
    private String status;
}
