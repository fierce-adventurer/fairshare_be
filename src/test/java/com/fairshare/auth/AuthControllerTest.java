package com.fairshare.auth;

import com.fairshare.auth.dto.AuthResponse;
import com.fairshare.auth.oauth.AppleOAuth2Handler;
import com.fairshare.auth.oauth.GitHubOAuth2Handler;
import com.fairshare.auth.oauth.GoogleOAuth2Handler;
import com.fairshare.auth.service.AuthService;
import com.fairshare.auth.service.EmailVerificationService;
import com.fairshare.auth.service.OAuthStateService;
import com.fairshare.auth.service.OtpService;
import com.fairshare.shared.security.JwtTokenProvider;
import com.fairshare.user.dto.UserProfileResponse;
import com.fairshare.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @MockBean AuthService authService;
    @MockBean GoogleOAuth2Handler googleOAuth2Handler;
    @MockBean GitHubOAuth2Handler gitHubOAuth2Handler;
    @MockBean AppleOAuth2Handler appleOAuth2Handler;
    @MockBean OtpService otpService;
    @MockBean OAuthStateService stateService;
    @MockBean EmailVerificationService emailVerificationService;

    @Test
    void phoneOtpCanCreateAnAuthenticatedReturningSession() throws Exception {
        UserProfileResponse profile = profile("phone@example.com", "Phone User");
        AuthResponse auth = new AuthResponse("access.jwt", "refresh-id", profile);
        when(otpService.verifyOtp("+919876543210", "482193", OtpService.OtpPurpose.SIGN_IN))
                .thenReturn(new OtpService.OtpVerification("+919876543210", "proof"));
        when(authService.loginWithVerifiedPhone("+919876543210")).thenReturn(Optional.of(auth));

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+919876543210","code":"482193","purpose":"SIGN_IN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registrationRequired").value(false))
                .andExpect(jsonPath("$.data.auth.accessToken").value("access.jwt"));
    }

    @Test
    void signUpOtpReturnsASingleUseRegistrationProof() throws Exception {
        when(otpService.verifyOtp("+919876543210", "482193", OtpService.OtpPurpose.SIGN_UP))
                .thenReturn(new OtpService.OtpVerification("+919876543210", "registration-proof"));
        when(authService.loginWithVerifiedPhone("+919876543210")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+919876543210","code":"482193","purpose":"SIGN_UP"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registrationRequired").value(true))
                .andExpect(jsonPath("$.data.verificationToken").value("registration-proof"));
    }

    @Test
    void registrationAcceptsTheReviewedDetailsContract() throws Exception {
        AuthResponse response = new AuthResponse("access.jwt", "refresh-id", profile("new@example.com", "New Member"));
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"New",
                                  "lastName":"Member",
                                  "email":"new@example.com",
                                  "dateOfBirth":"1998-04-12",
                                  "password":"Password123",
                                  "phone":"+919876543210",
                                  "phoneVerificationToken":"registration-proof"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.email").value("new@example.com"));
    }

    @Test
    void nativeGoogleFlowUsesValidatedMobileStateAndReturnsToTheApp() throws Exception {
        when(stateService.issue("mobile")).thenReturn("mobile:state");
        when(googleOAuth2Handler.buildAuthorizeUrl("mobile:state")).thenReturn("https://accounts.google.com/o/oauth2/v2/auth");
        mockMvc.perform(get("/api/v1/auth/oauth/google").param("platform", "mobile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://accounts.google.com/o/oauth2/v2/auth"));

        when(stateService.consume("mobile:state")).thenReturn(OAuthStateService.OAuthClient.MOBILE);
        when(stateService.infer("mobile:state")).thenReturn(OAuthStateService.OAuthClient.MOBILE);
        when(authService.handleGoogleCallback("provider-code"))
                .thenReturn(new AuthResponse("access.jwt", "refresh-id", profile("mobile@example.com", "Mobile User")));
        mockMvc.perform(get("/api/v1/auth/oauth/google/callback")
                        .param("code", "provider-code")
                        .param("state", "mobile:state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("com.fairshare.app://auth/callback?access_token=access.jwt&refresh_token=refresh-id"));
    }

    @Test
    void appleCallbackUsesTheSameValidatedSessionContract() throws Exception {
        when(stateService.consume("web:state")).thenReturn(OAuthStateService.OAuthClient.WEB);
        when(stateService.infer("web:state")).thenReturn(OAuthStateService.OAuthClient.WEB);
        when(authService.handleAppleCallback(eq("apple-code"), any()))
                .thenReturn(new AuthResponse("apple.jwt", "apple-refresh", profile("relay@privaterelay.appleid.com", "Apple User")));

        mockMvc.perform(post("/api/v1/auth/oauth/apple/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("code", "apple-code")
                        .param("state", "web:state")
                        .param("user", "{\"name\":{\"firstName\":\"Apple\",\"lastName\":\"User\"}}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:5173/auth/callback?access_token=apple.jwt&refresh_token=apple-refresh"));
    }

    @Test
    void invalidEmailLoginBodyIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticatedUserCanRecordRumButCannotReadAdminStats() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(UUID.randomUUID(), "rum-user@example.com", "USER");
        mockMvc.perform(post("/api/v1/admin/rum")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventName\":\"contacts_loaded\",\"durationMs\":12}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private UserProfileResponse profile(String email, String name) {
        return UserProfileResponse.from(new User(UUID.randomUUID(), email, "hash", name));
    }
}
