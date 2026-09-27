package com.fairshare.auth.controller;

import com.fairshare.auth.dto.AuthResponse;
import com.fairshare.auth.dto.LoginRequest;
import com.fairshare.auth.dto.RefreshRequest;
import com.fairshare.auth.dto.RegisterRequest;
import com.fairshare.auth.dto.SendOtpRequest;
import com.fairshare.auth.dto.VerifyOtpRequest;
import com.fairshare.auth.oauth.GitHubOAuth2Handler;
import com.fairshare.auth.oauth.GoogleOAuth2Handler;
import com.fairshare.auth.service.AuthService;
import com.fairshare.auth.service.OtpService;
import com.fairshare.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, login, OAuth and token management")
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuth2Handler googleOAuth2Handler;
    private final GitHubOAuth2Handler gitHubOAuth2Handler;
    private final OtpService otpService;
    private final String frontendUrl;

    public AuthController(
            AuthService authService,
            GoogleOAuth2Handler googleOAuth2Handler,
            GitHubOAuth2Handler gitHubOAuth2Handler,
            OtpService otpService,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl
    ) {
        this.authService = authService;
        this.googleOAuth2Handler = googleOAuth2Handler;
        this.gitHubOAuth2Handler = gitHubOAuth2Handler;
        this.otpService = otpService;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Send OTP code to phone number")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        String code = otpService.sendOtp(request.phone());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("phone", request.phone(), "message", "OTP sent successfully")));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP code")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean valid = otpService.verifyOtp(request.phone(), request.code());
        if (!valid) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired OTP code"));
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of("verified", true), "OTP verified successfully"));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user with email & password")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email & password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh expired access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(response, "Token refreshed"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody RefreshRequest request) {
        if (request != null && request.refreshToken() != null) {
            authService.logout(request.refreshToken());
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }

    @GetMapping("/oauth/google")
    @Operation(summary = "Initiate Google OAuth2 flow (redirects to Google)")
    public RedirectView initiateGoogleOAuth() {
        String state = UUID.randomUUID().toString();
        String authorizeUrl = googleOAuth2Handler.buildAuthorizeUrl(state);
        return new RedirectView(authorizeUrl);
    }

    @GetMapping("/oauth/google/callback")
    @Operation(summary = "Google OAuth2 callback (redirects to frontend with tokens)")
    public RedirectView handleGoogleCallback(@RequestParam String code) {
        try {
            AuthResponse response = authService.handleGoogleCallback(code);
            String redirectTarget = String.format(
                    "%s/auth/callback?access_token=%s&refresh_token=%s",
                    frontendUrl,
                    URLEncoder.encode(response.accessToken(), StandardCharsets.UTF_8),
                    URLEncoder.encode(response.refreshToken(), StandardCharsets.UTF_8)
            );
            return new RedirectView(redirectTarget);
        } catch (Exception e) {
            String errorRedirect = String.format(
                    "%s/auth/callback?error=%s",
                    frontendUrl,
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)
            );
            return new RedirectView(errorRedirect);
        }
    }

    @GetMapping("/oauth/github")
    @Operation(summary = "Initiate GitHub OAuth2 flow (redirects to GitHub)")
    public RedirectView initiateGitHubOAuth() {
        String state = UUID.randomUUID().toString();
        String authorizeUrl = gitHubOAuth2Handler.buildAuthorizeUrl(state);
        return new RedirectView(authorizeUrl);
    }

    @GetMapping("/oauth/github/callback")
    @Operation(summary = "GitHub OAuth2 callback (redirects to frontend with tokens)")
    public RedirectView handleGitHubCallback(@RequestParam String code) {
        try {
            AuthResponse response = authService.handleGithubCallback(code);
            String redirectTarget = String.format(
                    "%s/auth/callback?access_token=%s&refresh_token=%s",
                    frontendUrl,
                    URLEncoder.encode(response.accessToken(), StandardCharsets.UTF_8),
                    URLEncoder.encode(response.refreshToken(), StandardCharsets.UTF_8)
            );
            return new RedirectView(redirectTarget);
        } catch (Exception e) {
            String errorRedirect = String.format(
                    "%s/auth/callback?error=%s",
                    frontendUrl,
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)
            );
            return new RedirectView(errorRedirect);
        }
    }
}
