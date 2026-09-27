package com.fairshare.auth.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class GitHubOAuth2Handler {

    private static final Logger log = LoggerFactory.getLogger(GitHubOAuth2Handler.class);

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final RestTemplate restTemplate;

    public GitHubOAuth2Handler(
            @Value("${app.oauth2.github.client-id:}") String clientId,
            @Value("${app.oauth2.github.client-secret:}") String clientSecret,
            @Value("${app.oauth2.github.redirect-uri:http://localhost:8080/api/v1/auth/oauth/github/callback}") String redirectUri
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.restTemplate = new RestTemplate();
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    public String buildAuthorizeUrl(String state) {
        if (!isConfigured()) {
            throw new IllegalStateException("GitHub OAuth is not configured. Please set GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET.");
        }
        return UriComponentsBuilder.fromHttpUrl("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "read:user,user:email")
                .queryParam("state", state)
                .build().toUriString();
    }

    public GitHubUserInfo exchangeCodeForUserInfo(String code) {
        if (!isConfigured()) {
            throw new IllegalStateException("GitHub OAuth is not configured. Please set GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET.");
        }

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(body, tokenHeaders);

        try {
            ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.exchange(
                    "https://github.com/login/oauth/access_token",
                    HttpMethod.POST,
                    tokenRequest,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> tokenBody = tokenResponse.getBody();
            if (tokenBody == null || !tokenBody.containsKey("access_token")) {
                String errorDescription = tokenBody != null && tokenBody.containsKey("error_description")
                        ? (String) tokenBody.get("error_description")
                        : "Failed to obtain access token from GitHub";
                throw new RuntimeException(errorDescription);
            }

            String accessToken = (String) tokenBody.get("access_token");

            // Fetch GitHub User details
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            userHeaders.set("User-Agent", "Fairshare-App");
            userHeaders.setAccept(Collections.singletonList(MediaType.parseMediaType("application/vnd.github+json")));
            HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

            ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    userRequest,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> userInfo = userResponse.getBody();
            if (userInfo == null) {
                throw new RuntimeException("Failed to obtain user info from GitHub");
            }

            String githubId = String.valueOf(userInfo.get("id"));
            String login = (String) userInfo.get("login");
            String name = (String) userInfo.get("name");
            if (name == null || name.isBlank()) {
                name = login;
            }
            String avatarUrl = (String) userInfo.get("avatar_url");
            String email = (String) userInfo.get("email");

            // If email is not in the public profile, fetch user's emails
            if (email == null || email.isBlank()) {
                email = fetchPrimaryEmail(accessToken);
            }

            // Fallback if still no email
            if (email == null || email.isBlank()) {
                email = login + "@users.noreply.github.com";
            }

            return new GitHubUserInfo(githubId, email, name, avatarUrl);
        } catch (Exception e) {
            log.error("GitHub OAuth token exchange failed", e);
            throw new RuntimeException("GitHub OAuth token exchange failed: " + e.getMessage());
        }
    }

    private String fetchPrimaryEmail(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("User-Agent", "Fairshare-App");
            headers.setAccept(Collections.singletonList(MediaType.parseMediaType("application/vnd.github+json")));
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<>() {}
            );

            List<Map<String, Object>> emails = response.getBody();
            if (emails != null && !emails.isEmpty()) {
                // Find primary email
                for (Map<String, Object> emailObj : emails) {
                    Boolean primary = (Boolean) emailObj.get("primary");
                    if (Boolean.TRUE.equals(primary)) {
                        return (String) emailObj.get("email");
                    }
                }
                // Fall back to first email in the list
                return (String) emails.get(0).get("email");
            }
        } catch (Exception e) {
            log.warn("Could not fetch user emails from GitHub: {}", e.getMessage());
        }
        return null;
    }

    public record GitHubUserInfo(String id, String email, String name, String avatarUrl) {}
}
