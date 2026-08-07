package edu.wharton.alumni.controller;

import edu.wharton.alumni.dto.ForgotPasswordRequest;
import edu.wharton.alumni.dto.LoginRequest;
import edu.wharton.alumni.dto.LoginResponse;
import edu.wharton.alumni.dto.MessageResponse;
import edu.wharton.alumni.dto.RegisterRequest;
import edu.wharton.alumni.dto.ResetPasswordRequest;
import edu.wharton.alumni.model.AlumniProfile;
import edu.wharton.alumni.security.JwtUser;
import edu.wharton.alumni.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public MessageResponse logout() {
        return new MessageResponse("Logged out.");
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return new MessageResponse("If an account exists, password reset instructions have been sent.");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new MessageResponse("Password reset complete.");
    }

    @GetMapping("/me")
    public AlumniProfile me(@AuthenticationPrincipal JwtUser user) {
        return authService.me(user.id());
    }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
}
