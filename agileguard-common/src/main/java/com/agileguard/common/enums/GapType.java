package com.agileguard.common.enums;

/** Types of JIRA story quality gaps that AgileGuard can detect. */
public enum GapType {
    MISSING_DESCRIPTION,
    MISSING_ACCEPTANCE_CRITERIA,
    MISSING_STORY_POINTS,
    MISSING_DEV_SUBTASK,
    MISSING_QA_SUBTASK,
    NO_EFFORT_LOGGED,
    AGING_STORY,
    INVALID_AC_FORMAT,
    COMPONENT_VERSION_CONFLICT,
    TRANSITION_VIOLATION,
    MISSING_PR_LINK
}
