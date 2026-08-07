package edu.wharton.alumni.dto;

import edu.wharton.alumni.model.EventStatus;
import jakarta.validation.constraints.NotNull;

public record EventStatusRequest(
        @NotNull EventStatus status
) {
}
