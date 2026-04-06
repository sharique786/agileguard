package com.agileguard.jira.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A JIRA user returned from the user-search endpoint. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JiraUser {
    private String accountId;
    private String displayName;
    private String emailAddress;
    private String avatarUrl;
}
