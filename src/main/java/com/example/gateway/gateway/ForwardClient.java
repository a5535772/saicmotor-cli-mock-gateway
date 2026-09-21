package com.example.gateway.gateway;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ForwardClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public record ForwardResponse(int status, String contentType, byte[] body) {}

    public ForwardResponse forward(String method, String url, String contentType, byte[] body, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        if (contentType != null) headers.set("Content-Type", contentType);

        HttpEntity<byte[]> entity = new HttpEntity<>(body != null && body.length > 0 ? body : null, headers);
        ResponseEntity<byte[]> e = restTemplate.exchange(url, HttpMethod.valueOf(method), entity, byte[].class);
        return new ForwardResponse(e.getStatusCode().value(), e.getHeaders().getFirst("Content-Type"), e.getBody());
    }
}