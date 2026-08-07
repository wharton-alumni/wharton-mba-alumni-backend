package edu.wharton.alumni.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CohortCampus {
    Philadelphia("Philadelphia"),
    San_Francisco("San Francisco"),
    Global("Global");

    private final String label;

    CohortCampus(String label) {
        this.label = label;
    }

    @JsonValue
    public String label() {
        return label;
    }

    @JsonCreator
    public static CohortCampus fromLabel(String value) {
        for (CohortCampus campus : values()) {
            if (campus.label.equalsIgnoreCase(value) || campus.name().equalsIgnoreCase(value)) {
                return campus;
            }
        }
        throw new IllegalArgumentException("Unsupported cohort campus: " + value);
    }
}
