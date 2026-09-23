package com.example.gateway.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ForwardClient {

    private static final Logger log = LoggerFactory.getLogger(ForwardClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    public record ForwardResponse(int status, String contentType, byte[] body) {}

    public ForwardResponse forward(String method, String url, String contentType, byte[] body, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        if (contentType != null) headers.set("Content-Type", contentType);

        int bodySize = body == null ? 0 : body.length;
        HttpEntity<byte[]> entity = new HttpEntity<>(bodySize > 0 ? body : null, headers);
        long start = System.currentTimeMillis();
        log.debug("转发上游请求 — {} {} user={} bodyBytes={}", method, url, userId, bodySize);
        try {
            ResponseEntity<byte[]> e = restTemplate.exchange(
                    url, HttpMethod.valueOf(method), entity, byte[].class);
            log.debug("上游响应 — {} {} status={} duration={}ms",
                    method, url, e.getStatusCode().value(), System.currentTimeMillis() - start);
            return new ForwardResponse(e.getStatusCode().value(),
                    e.getHeaders().getFirst("Content-Type"), e.getBody());
        } catch (Exception ex) {
            log.error("转发上游失败 — {} {} user={} duration={}ms 原因={}",
                    method, url, userId, System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }
}
