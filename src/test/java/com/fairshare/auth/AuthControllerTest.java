package com.fairshare.auth;

import com.fairshare.auth.dto.AuthResponse;
import com.fairshare.auth.dto.LoginRequest;
import com.fairshare.auth.dto.RegisterRequest;
import com.fairshare.auth.oauth.GitHubOAuth2Handler;
import com.fairshare.auth.oauth.GoogleOAuth2Handler;
import com.fairshare.auth.service.AuthService;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private GoogleOAuth2Handler googleOAuth2Handler;

    @MockBean
    private GitHubOAuth2Handler gitHubOAuth2Handler;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void testRegisterSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        User dummyUser = new User(userId, "test@example.com", "hash", "Test User");
        UserProfileResponse profile = UserProfileResponse.from(dummyUser);
        AuthResponse mockResponse = new AuthResponse("access.token.jwt", "refresh-uuid", profile);

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        String json = """
                {
                    "name": "Test User",
                    "email": "test@example.com",
                    "password": "Password123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access.token.jwt"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
    }

    @Test
    void testLoginInvalidBodyValidation() throws Exception {
        String invalidJson = """
                {
                    "email": "not-an-email",
                    "password": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nativeGoogleLoginCarriesNativeDestinationInState() throws Exception {
        when(googleOAuth2Handler.buildAuthorizeUrl(any(String.class)))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth");

        mockMvc.perform(get("/api/v1/auth/oauth/google").param("client", "native"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://accounts.google.com/o/oauth2/v2/auth"));

        verify(googleOAuth2Handler).buildAuthorizeUrl(argThat(state -> state.startsWith("native.")));
    }

    @Test
    void nativeGithubLoginCarriesNativeDestinationInState() throws Exception {
        when(gitHubOAuth2Handler.buildAuthorizeUrl(any(String.class)))
                .thenReturn("https://github.com/login/oauth/authorize");

        mockMvc.perform(get("/api/v1/auth/oauth/github").param("client", "native"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://github.com/login/oauth/authorize"));

        verify(gitHubOAuth2Handler).buildAuthorizeUrl(argThat(state -> state.startsWith("native.")));
    }

    @Test
    void nativeGoogleCallbackReturnsToTheAppWithTokensInFragment() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse profile = UserProfileResponse.from(new User(userId, "native@example.com", "hash", "Native User"));
        when(authService.handleGoogleCallback("provider-code"))
                .thenReturn(new AuthResponse("access.token.jwt", "refresh-uuid", profile));

        mockMvc.perform(get("/api/v1/auth/oauth/google/callback")
                        .param("code", "provider-code")
                        .param("state", "native.test-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("com.fairshare.app://auth/callback#access_token=access.token.jwt&refresh_token=refresh-uuid"));
    }

    @Test
    void webGoogleCallbackKeepsTheBrowserCallback() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse profile = UserProfileResponse.from(new User(userId, "web@example.com", "hash", "Web User"));
        when(authService.handleGoogleCallback("provider-code"))
                .thenReturn(new AuthResponse("access.token.jwt", "refresh-uuid", profile));

        mockMvc.perform(get("/api/v1/auth/oauth/google/callback")
                        .param("code", "provider-code")
                        .param("state", "web.test-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:5173/auth/callback?access_token=access.token.jwt&refresh_token=refresh-uuid"));
    }

    @Test
    void authenticatedUserCanRecordRumButCannotReadAdminStats() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(UUID.randomUUID(), "rum-user@example.com", "USER");

        mockMvc.perform(post("/api/v1/admin/rum")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventName": "contacts_loaded",
                                  "path": "/people",
                                  "durationMs": 12,
                                  "metadata": {"source": "security-test"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
