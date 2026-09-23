package com.example.gateway.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StateStore {

    private static final Logger log = LoggerFactory.getLogger(StateStore.class);

    private record Entry(String redirectUri, long expiresAt) {}

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final long ttlSeconds;

    @Autowired
    public StateStore(IdpProperties props) {
        this.ttlSeconds = props.getStateTtlSeconds();
    }

    // 供测试直接指定 TTL
    StateStore(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public String create(String redirectUri) {
        long now = System.currentTimeMillis();
        int before = entries.size();
        entries.entrySet().removeIf(en -> now > en.getValue().expiresAt());
        int removed = before - entries.size();
        if (removed > 0) log.debug("清理过期 state — count={}", removed);

        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        entries.put(state, new Entry(redirectUri, now + ttlSeconds * 1000));
        log.debug("创建 state — state={} redirectUri={} ttl={}s", state, redirectUri, ttlSeconds);
        return state;
    }

    public boolean validate(String state, String redirectUri) {
        if (state == null) {
            log.debug("state 校验失败 — 原因=state 为空");
            return false;
        }
        Entry e = entries.remove(state); // 单次使用
        if (e == null) {
            log.debug("state 校验失败 — state={} 原因=不存在或已被使用", state);
            return false;
        }
        if (System.currentTimeMillis() > e.expiresAt()) {
            log.debug("state 校验失败 — state={} 原因=已过期", state);
            return false;
        }
        if (redirectUri == null || !e.redirectUri().equals(redirectUri)) {
            log.debug("state 校验失败 — state={} 原因=redirectUri 不匹配 expected={} actual={}",
                    state, e.redirectUri(), redirectUri);
            return false;
        }
        log.debug("state 校验通过 — state={}", state);
        return true;
    }
}
