package com.agileguard.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request to create or update a project team. */
@Data
public class ProjectTeamRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private String jiraProjectKey;
    private String githubOrg;
}
