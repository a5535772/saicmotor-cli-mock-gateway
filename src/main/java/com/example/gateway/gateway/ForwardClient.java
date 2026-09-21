package com.example.gateway.gateway;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ForwardClient {

    private final RestClient client = RestClient.create();

    public record ForwardResponse(int status, String contentType, byte[] body) {}

    public ForwardResponse forward(String method, String url, String contentType, byte[] body, String userId) {
        RestClient.RequestBodySpec spec = client.method(HttpMethod.valueOf(method))
            .uri(url)
            .header("X-User-Id", userId);
        if (contentType != null) spec.header("Content-Type", contentType);
        if (body != null && body.length > 0) spec.body(body);
        ResponseEntity<byte[]> e = spec.retrieve().toEntity(byte[].class);
        return new ForwardResponse(e.getStatusCode().value(), e.getHeaders().getFirst("Content-Type"), e.getBody());
    }
}
