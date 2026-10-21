package com.fairshare.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class SmsDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(SmsDeliveryService.class);

    private final String provider;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final RestTemplate restTemplate = new RestTemplate();

    public SmsDeliveryService(
            @Value("${app.sms.provider:console}") String provider,
            @Value("${app.sms.twilio.account-sid:}") String accountSid,
            @Value("${app.sms.twilio.auth-token:}") String authToken,
            @Value("${app.sms.twilio.from-number:}") String fromNumber
    ) {
        this.provider = provider;
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    public void sendOtp(String phone, String code) {
        if ("console".equalsIgnoreCase(provider)) {
            log.info("Development OTP prepared for {}", mask(phone));
            return;
        }
        if (!"twilio".equalsIgnoreCase(provider)) {
            throw new IllegalStateException("Unsupported SMS provider: " + provider);
        }
        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            throw new IllegalStateException("Twilio SMS is selected but its credentials are incomplete.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(accountSid, authToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", phone);
        body.add("From", fromNumber);
        body.add("Body", "Your Fairshare verification code is " + code + ". It expires in 5 minutes.");
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
        restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private String mask(String phone) {
        if (phone.length() <= 4) return "****";
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
