package com.fairshare.auth.oauth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class AppleOAuth2Handler {
    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private final String clientId;
    private final String teamId;
    private final String keyId;
    private final String privateKeyPem;
    private final String configuredClientSecret;
    private final String redirectUri;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final NimbusJwtDecoder decoder;

    public AppleOAuth2Handler(
            @Value("${app.oauth2.apple.client-id:}") String clientId,
            @Value("${app.oauth2.apple.team-id:}") String teamId,
            @Value("${app.oauth2.apple.key-id:}") String keyId,
            @Value("${app.oauth2.apple.private-key:}") String privateKeyPem,
            @Value("${app.oauth2.apple.client-secret:}") String configuredClientSecret,
            @Value("${app.oauth2.apple.redirect-uri:https://example.com/api/v1/auth/oauth/apple/callback}") String redirectUri,
            ObjectMapper objectMapper
    ) {
        this.clientId = clientId;
        this.teamId = teamId;
        this.keyId = keyId;
        this.privateKeyPem = privateKeyPem;
        this.configuredClientSecret = configuredClientSecret;
        this.redirectUri = redirectUri;
        this.objectMapper = objectMapper;
        this.decoder = NimbusJwtDecoder.withJwkSetUri(APPLE_ISSUER + "/auth/keys").build();
        this.decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(APPLE_ISSUER));
    }

    public boolean isConfigured() {
        boolean canCreateSecret = !teamId.isBlank() && !keyId.isBlank() && !privateKeyPem.isBlank();
        return !clientId.isBlank() && !redirectUri.isBlank() && (!configuredClientSecret.isBlank() || canCreateSecret);
    }

    public String buildAuthorizeUrl(String state) {
        requireConfiguration();
        return UriComponentsBuilder.fromHttpUrl(APPLE_ISSUER + "/auth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("response_mode", "form_post")
                .queryParam("scope", "name email")
                .queryParam("state", state)
                .build().encode().toUriString();
    }

    public AppleUserInfo exchangeCodeForUserInfo(String code, String userJson) {
        requireConfiguration();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret());
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", redirectUri);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                APPLE_ISSUER + "/auth/token",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<>() {}
        );
        Map<String, Object> tokenBody = response.getBody();
        if (tokenBody == null || tokenBody.get("id_token") == null) throw new IllegalStateException("Apple did not return an identity token.");
        Jwt jwt = decoder.decode(String.valueOf(tokenBody.get("id_token")));
        List<String> audiences = jwt.getAudience();
        if (audiences == null || !audiences.contains(clientId)) throw new IllegalStateException("Apple identity token audience does not match Fairshare.");

        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        if (sub == null || sub.isBlank() || email == null || email.isBlank()) throw new IllegalStateException("Apple did not return the account identity required by Fairshare.");
        Object rawEmailVerified = jwt.getClaims().get("email_verified");
        boolean emailVerified = Boolean.TRUE.equals(rawEmailVerified) || "true".equalsIgnoreCase(String.valueOf(rawEmailVerified));
        if (!emailVerified) throw new IllegalStateException("Apple did not return a verified email address.");
        return new AppleUserInfo(sub, email, extractName(userJson), true);
    }

    private String extractName(String userJson) {
        if (userJson == null || userJson.isBlank()) return null;
        try {
            Map<String, Object> user = objectMapper.readValue(userJson, new TypeReference<>() {});
            Object rawName = user.get("name");
            if (!(rawName instanceof Map<?, ?> name)) return null;
            String first = name.get("firstName") == null ? "" : String.valueOf(name.get("firstName")).trim();
            String last = name.get("lastName") == null ? "" : String.valueOf(name.get("lastName")).trim();
            String full = (first + " " + last).trim();
            return full.isBlank() ? null : full;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String clientSecret() {
        if (!configuredClientSecret.isBlank()) return configuredClientSecret;
        try {
            String normalized = privateKeyPem.replace("\\n", "\n")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            PrivateKey privateKey = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(normalized)));
            Instant now = Instant.now();
            return Jwts.builder()
                    .header().keyId(keyId).and()
                    .issuer(teamId)
                    .subject(clientId)
                    .audience().add(APPLE_ISSUER).and()
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plus(180, ChronoUnit.DAYS)))
                    .signWith(privateKey, Jwts.SIG.ES256)
                    .compact();
        } catch (Exception invalidKey) {
            throw new IllegalStateException("The configured Apple private key is invalid.", invalidKey);
        }
    }

    private void requireConfiguration() {
        if (!isConfigured()) throw new IllegalStateException("Sign in with Apple is not configured on the backend.");
    }

    public record AppleUserInfo(String id, String email, String name, boolean emailVerified) {}
}
