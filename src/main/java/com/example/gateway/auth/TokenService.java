package com.example.gateway.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TokenService {

    private static final long TTL_MS = 3_600_000L;
    private final ObjectMapper mapper = new ObjectMapper();

    public String issue(String username, String userId) {
        String payload = "{\"sub\":\"" + userId + "\",\"username\":\"" + username
            + "\",\"exp\":" + (System.currentTimeMillis() + TTL_MS) + "}";
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public String parseUserId(String token) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);
            JsonNode node = mapper.readTree(new String(decoded, StandardCharsets.UTF_8));
            if (node.path("exp").asLong() < System.currentTimeMillis()) return null;
            String sub = node.path("sub").asText();
            return sub == null || sub.isEmpty() ? null : sub;
        } catch (Exception e) {
            return null;
        }
    }
}