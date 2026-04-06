package com.agileguard.jira.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Represents an Atlassian JIRA sprint. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JiraSprint {
    private Long   id;
    private String name;           // "Sprint 42 — Platform Q2"
    private String state;          // active | closed | future
    private String boardId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String goal;
}
