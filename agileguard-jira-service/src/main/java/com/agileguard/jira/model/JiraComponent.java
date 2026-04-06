package com.agileguard.jira.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents a JIRA project component. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JiraComponent {
    private String id;
    private String name;
    private String description;
    private String leadName;
}
