package edu.wharton.alumni.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EventRsvpStatus {
    JOINED("JOINED"),
    INTERESTED("INTERESTED"),
    CANCELLED("CANCELLED");

    private final String label;

    EventRsvpStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String label() {
        return label;
    }

    @JsonCreator
    public static EventRsvpStatus fromLabel(String value) {
        for (EventRsvpStatus status : values()) {
            if (status.label.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unsupported RSVP status: " + value);
    }
}
