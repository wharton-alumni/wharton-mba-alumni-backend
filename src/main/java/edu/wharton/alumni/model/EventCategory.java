package edu.wharton.alumni.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EventCategory {
    Networking("Networking"),
    Industry_Insights("Industry Insights"),
    Reunion("Reunion"),
    Career_Opportunity("Career Opportunity"),
    Community_Event("Community Event");

    private final String label;

    EventCategory(String label) {
        this.label = label;
    }

    @JsonValue
    public String label() {
        return label;
    }

    @JsonCreator
    public static EventCategory fromLabel(String value) {
        for (EventCategory category : values()) {
            if (category.label.equalsIgnoreCase(value) || category.name().equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unsupported event category: " + value);
    }
}
