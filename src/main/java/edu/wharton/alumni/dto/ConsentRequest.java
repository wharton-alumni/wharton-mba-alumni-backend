package edu.wharton.alumni.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsentRequest(@NotBlank String consentText) {
}
