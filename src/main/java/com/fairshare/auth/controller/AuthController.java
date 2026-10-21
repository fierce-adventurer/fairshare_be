package com.fairshare.auth.controller;

import com.fairshare.auth.dto.*;
import com.fairshare.auth.oauth.AppleOAuth2Handler;
import com.fairshare.auth.oauth.GitHubOAuth2Handler;
import com.fairshare.auth.oauth.GoogleOAuth2Handler;
import com.fairshare.auth.service.AuthService;
import com.fairshare.auth.service.EmailVerificationService;
import com.fairshare.auth.service.OAuthStateService;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Phone, OAuth, email verification and token management")
public class AuthController {
    private final AuthService authService;
    private final GoogleOAuth2Handler googleOAuth2Handler;
    private final GitHubOAuth2Handler gitHubOAuth2Handler;
    private final AppleOAuth2Handler appleOAuth2Handler;
    private final OtpService otpService;
    private final OAuthStateService stateService;
    private final EmailVerificationService emailVerificationService;
    private final String frontendUrl;
    private final String mobileRedirectUri;

    public AuthController(
            AuthService authService,
            GoogleOAuth2Handler googleOAuth2Handler,
            GitHubOAuth2Handler gitHubOAuth2Handler,
            AppleOAuth2Handler appleOAuth2Handler,
            OtpService otpService,
            OAuthStateService stateService,
            EmailVerificationService emailVerificationService,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
            @Value("${app.mobile-redirect-uri:${app.native-auth-redirect-url:com.fairshare.app://auth/callback}}") String mobileRedirectUri
    ) {
        this.authService = authService;
        this.googleOAuth2Handler = googleOAuth2Handler;
        this.gitHubOAuth2Handler = gitHubOAuth2Handler;
        this.appleOAuth2Handler = appleOAuth2Handler;
        this.otpService = otpService;
        this.stateService = stateService;
        this.emailVerificationService = emailVerificationService;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.mobileRedirectUri = mobileRedirectUri;
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Send a purpose-bound OTP to a mobile number")
    public ResponseEntity<ApiResponse<OtpService.OtpDispatch>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        OtpService.OtpDispatch dispatch = otpService.sendOtp(request.phone(), OtpService.OtpPurpose.from(request.purpose()));
        return ResponseEntity.ok(ApiResponse.ok(dispatch, "Verification code sent"));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify a phone OTP and either sign in or authorize registration")
    public ResponseEntity<ApiResponse<VerifyPhoneOtpResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        OtpService.OtpPurpose purpose = OtpService.OtpPurpose.from(request.purpose());
        OtpService.OtpVerification verification = otpService.verifyOtp(request.phone(), request.code(), purpose);
        if (purpose == OtpService.OtpPurpose.SIGN_IN) {
            Optional<AuthResponse> auth = authService.loginWithVerifiedPhone(verification.phone());
            VerifyPhoneOtpResponse result = auth
                    .map(value -> new VerifyPhoneOtpResponse(true, false, verification.phone(), null, value))
                    .orElseGet(() -> new VerifyPhoneOtpResponse(true, true, verification.phone(), null, null));
            return ResponseEntity.ok(ApiResponse.ok(result, auth.isPresent() ? "Login successful" : "No account found for this mobile number"));
        }
        if (authService.loginWithVerifiedPhone(verification.phone()).isPresent()) {
            throw new com.fairshare.shared.exception.BadRequestException("That mobile number already has an account. Sign in instead.");
        }
        return ResponseEntity.ok(ApiResponse.ok(
                new VerifyPhoneOtpResponse(true, true, verification.phone(), verification.verificationToken(), null),
                "Phone verified"
        ));
    }

    @PostMapping("/register")
    @Operation(summary = "Create an account after phone verification")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authService.register(request), "Account created. Verify your email and review your profile."));
    }

    @PostMapping("/login")
    @Operation(summary = "Legacy email/password login used for account recovery")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request), "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refreshToken(request.refreshToken()), "Token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody RefreshRequest request) {
        if (request != null && request.refreshToken() != null) authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }

    @GetMapping("/oauth/google")
    public RedirectView initiateGoogleOAuth(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String client
    ) {
        String state = stateService.issue(platform != null ? platform : client);
        return new RedirectView(googleOAuth2Handler.buildAuthorizeUrl(state));
    }

    @GetMapping("/oauth/github")
    public RedirectView initiateGithubOAuth(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String client
    ) {
        String state = stateService.issue(platform != null ? platform : client);
        return new RedirectView(gitHubOAuth2Handler.buildAuthorizeUrl(state));
    }

    @GetMapping("/oauth/apple")
    public RedirectView initiateAppleOAuth(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String client
    ) {
        String state = stateService.issue(platform != null ? platform : client);
        return new RedirectView(appleOAuth2Handler.buildAuthorizeUrl(state));
    }

    @GetMapping("/oauth/google/callback")
    public RedirectView handleGoogleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        return completeOAuth(state, code, error, errorDescription, () -> authService.handleGoogleCallback(code));
    }

    @GetMapping("/oauth/github/callback")
    public RedirectView handleGithubCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        return completeOAuth(state, code, error, errorDescription, () -> authService.handleGithubCallback(code));
    }

    @PostMapping("/oauth/apple/callback")
    public RedirectView handleAppleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            @RequestParam(required = false) String user
    ) {
        return completeOAuth(state, code, error, errorDescription, () -> authService.handleAppleCallback(code, user));
    }

    @GetMapping("/email/verify")
    public RedirectView verifyEmail(@RequestParam String token) {
        try {
            emailVerificationService.verify(token);
            return new RedirectView(frontendUrl + "/?email_verified=true");
        } catch (Exception failure) {
            return new RedirectView(frontendUrl + "/?email_verification_error=" + encode(message(failure)));
        }
    }

    private RedirectView completeOAuth(
            String state,
            String code,
            String error,
            String errorDescription,
            AuthExchange exchange
    ) {
        OAuthStateService.OAuthClient inferred = stateService.infer(state);
        try {
            OAuthStateService.OAuthClient client = stateService.consume(state);
            if (error != null) return new RedirectView(errorRedirect(client, errorDescription == null || errorDescription.isBlank() ? error : errorDescription));
            if (code == null || code.isBlank()) return new RedirectView(errorRedirect(client, "Missing authorization code"));
            return new RedirectView(successRedirect(client, exchange.exchange()));
        } catch (Exception failure) {
            return new RedirectView(errorRedirect(inferred, message(failure)));
        }
    }

    private String successRedirect(OAuthStateService.OAuthClient client, AuthResponse response) {
        return callbackBase(client) + "?access_token=" + encode(response.accessToken()) + "&refresh_token=" + encode(response.refreshToken());
    }

    private String errorRedirect(OAuthStateService.OAuthClient client, String message) {
        return callbackBase(client) + "?error=" + encode(message == null ? "Authentication failed" : message);
    }

    private String callbackBase(OAuthStateService.OAuthClient client) {
        return client == OAuthStateService.OAuthClient.MOBILE ? mobileRedirectUri : frontendUrl + "/auth/callback";
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String message(Exception error) { return error.getMessage() == null ? "Authentication failed" : error.getMessage(); }

    @FunctionalInterface
    private interface AuthExchange { AuthResponse exchange(); }
}
