package com.example.gateway.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private static final long TTL_MS = 3_600_000L;
    private final ObjectMapper mapper = new ObjectMapper();

    public String issue(String username, String userId) {
        String payload = "{\"sub\":\"" + userId + "\",\"username\":\"" + username
            + "\",\"exp\":" + (System.currentTimeMillis() + TTL_MS) + "}";
        String token = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        log.debug("签发 token — username={} userId={} ttlMs={} fingerprint={}",
                username, userId, TTL_MS, fingerprint(token));
        return token;
    }

    public String parseUserId(String token) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);
            JsonNode node = mapper.readTree(new String(decoded, StandardCharsets.UTF_8));
            if (node.path("exp").asLong() < System.currentTimeMillis()) {
                log.debug("token 已过期 — userId={}", node.path("sub").asText());
                return null;
            }
            String sub = node.path("sub").asText();
            if (sub == null || sub.isEmpty()) {
                log.debug("token 缺少 sub 字段");
                return null;
            }
            return sub;
        } catch (Exception e) {
            log.debug("token 解析失败 — 原因={}", e.getMessage());
            return null;
        }
    }

    /** token 前 8 位指纹，用于日志中辨识同一 token，避免泄露完整凭据 */
    public static String fingerprint(String token) {
        if (token == null) return "null";
        return token.length() <= 8 ? token : token.substring(0, 8);
    }
}
