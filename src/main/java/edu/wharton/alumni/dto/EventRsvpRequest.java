package edu.wharton.alumni.dto;

import edu.wharton.alumni.model.EventRsvpStatus;
import jakarta.validation.constraints.NotNull;

public record EventRsvpRequest(@NotNull EventRsvpStatus status) {
}
