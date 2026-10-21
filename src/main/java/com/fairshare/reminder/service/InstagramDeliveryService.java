package com.fairshare.reminder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class InstagramDeliveryService {
    private final ObjectMapper mapper;
    private final String accessToken;
    private final String accountId;
    private final String graphVersion;

    public record DeliveryResult(String status, String providerReference, String errorMessage) {}

    public InstagramDeliveryService(
            ObjectMapper mapper,
            @Value("${app.instagram.access-token:}") String accessToken,
            @Value("${app.instagram.account-id:}") String accountId,
            @Value("${app.instagram.graph-version:v25.0}") String graphVersion
    ) {
        this.mapper = mapper;
        this.accessToken = accessToken;
        this.accountId = accountId;
        this.graphVersion = graphVersion;
    }

    public DeliveryResult send(String instagramScopedRecipientId, String message) {
        if (instagramScopedRecipientId == null || instagramScopedRecipientId.isBlank()) {
            return new DeliveryResult("recipient_link_required", null,
                    "The payer must message Fairshare's connected Instagram professional account before reminders can be delivered.");
        }
        if (accessToken.isBlank() || accountId.isBlank()) {
            return new DeliveryResult("queued_configuration_required", null,
                    "Instagram professional messaging credentials are not configured.");
        }
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "recipient", Map.of("id", instagramScopedRecipientId),
                    "message", Map.of("text", message)
            ));
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("https://graph.instagram.com/" + graphVersion + "/" + accountId + "/messages"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode json = mapper.readTree(response.body());
                return new DeliveryResult("sent", json.path("message_id").asText(null), null);
            }
            return new DeliveryResult("failed", null, "Instagram provider returned HTTP " + response.statusCode());
        } catch (Exception error) {
            return new DeliveryResult("failed", null, error.getMessage());
        }
    }
}
