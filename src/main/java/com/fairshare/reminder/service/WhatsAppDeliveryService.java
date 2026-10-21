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
public class WhatsAppDeliveryService {
    private final ObjectMapper mapper;
    private final String accessToken;
    private final String phoneNumberId;
    private final String graphVersion;
    public record DeliveryResult(String status,String providerReference,String errorMessage){}
    public WhatsAppDeliveryService(ObjectMapper mapper,
        @Value("${app.whatsapp.access-token:}") String accessToken,
        @Value("${app.whatsapp.phone-number-id:}") String phoneNumberId,
        @Value("${app.whatsapp.graph-version:v21.0}") String graphVersion){this.mapper=mapper;this.accessToken=accessToken;this.phoneNumberId=phoneNumberId;this.graphVersion=graphVersion;}

    public DeliveryResult send(String destination,String message){
        if(accessToken.isBlank()||phoneNumberId.isBlank()) return new DeliveryResult("queued_configuration_required",null,"WhatsApp Business credentials are not configured.");
        if(destination==null||destination.isBlank()) return new DeliveryResult("failed",null,"Payer has no phone number.");
        try{
            String to=destination.replaceAll("\\D","");
            String body=mapper.writeValueAsString(Map.of("messaging_product","whatsapp","recipient_type","individual","to",to,"type","text","text",Map.of("preview_url",false,"body",message)));
            HttpRequest request=HttpRequest.newBuilder(URI.create("https://graph.facebook.com/"+graphVersion+"/"+phoneNumberId+"/messages"))
                    .header("Authorization","Bearer "+accessToken).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()>=200&&response.statusCode()<300){JsonNode json=mapper.readTree(response.body());String ref=json.path("messages").path(0).path("id").asText(null);return new DeliveryResult("sent",ref,null);}
            return new DeliveryResult("failed",null,"WhatsApp provider returned HTTP "+response.statusCode());
        }catch(Exception error){return new DeliveryResult("failed",null,error.getMessage());}
    }
}
