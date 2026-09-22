package com.example.gateway.auth;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StateStore {

    private record Entry(String redirectUri, long expiresAt) {}

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final long ttlSeconds;

    public StateStore(IdpProperties props) {
        this.ttlSeconds = props.getStateTtlSeconds();
    }

    // 供测试直接指定 TTL
    StateStore(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public String create(String redirectUri) {
        long now = System.currentTimeMillis();
        entries.entrySet().removeIf(en -> now > en.getValue().expiresAt());
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        entries.put(state, new Entry(redirectUri, now + ttlSeconds * 1000));
        return state;
    }

    public boolean validate(String state, String redirectUri) {
        if (state == null) return false;
        Entry e = entries.remove(state); // 单次使用
        if (e == null) return false;
        if (System.currentTimeMillis() > e.expiresAt()) return false;
        return redirectUri != null && e.redirectUri().equals(redirectUri);
    }
}
