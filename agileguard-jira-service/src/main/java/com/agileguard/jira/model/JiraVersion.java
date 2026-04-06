package com.agileguard.jira.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Represents a JIRA fix version / release. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JiraVersion {
    private String    id;
    private String    name;           // "v2.4.0"
    private String    description;
    private boolean   released;
    private boolean   archived;
    private LocalDate releaseDate;
}
