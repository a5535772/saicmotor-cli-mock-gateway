package com.example.gateway.auth;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private final TokenService service = new TokenService();

    @Test
    void issueThenParseRoundTrips() {
        String token = service.issue("zhangsan", "zhangsan");
        assertEquals("zhangsan", service.parseUserId(token));
    }

    @Test
    void parseGarbageReturnsNull() {
        assertNull(service.parseUserId("not-a-valid-token"));
    }

    @Test
    void parseExpiredReturnsNull() {
        String payload = "{\"sub\":\"zhangsan\",\"exp\":1}";
        String expired = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        assertNull(service.parseUserId(expired));
    }
}