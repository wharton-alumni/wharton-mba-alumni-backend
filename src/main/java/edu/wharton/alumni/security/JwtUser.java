package edu.wharton.alumni.security;

import edu.wharton.alumni.model.Role;

import java.util.UUID;

public record JwtUser(UUID id, Role role) {
}
