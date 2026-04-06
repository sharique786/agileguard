package com.agileguard.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request to create or update a feature team. */
@Data
public class FeatureTeamRequest {

    @NotBlank(message = "Feature team name is required")
    private String name;

    private String jiraComponent;
    private String githubRepos;
}
