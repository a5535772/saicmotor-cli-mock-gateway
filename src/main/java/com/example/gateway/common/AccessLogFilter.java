package com.example.gateway.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * 全量访问日志：为每个请求生成 requestId 放入 MDC，并记录入口/出口骨架日志。
 * 业务日志通过 logback pattern 中的 %X{requestId} 与本次请求串联。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = newRequestId();
        MDC.put("requestId", requestId);
        long start = System.currentTimeMillis();

        String query = request.getQueryString();
        String uri = request.getRequestURI() + (query != null ? "?" + query : "");
        log.info("收到请求 — {} {} from={}", request.getMethod(), uri, clientIp(request));

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("请求完成 — {} {} status={} duration={}ms",
                    request.getMethod(), uri, response.getStatus(), duration);
            MDC.remove("requestId");
        }
    }

    private static String clientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return req.getRemoteAddr();
    }

    private static String newRequestId() {
        byte[] bytes = new byte[4];
        RANDOM.nextBytes(bytes);
        char[] out = new char[8];
        for (int i = 0; i < 4; i++) {
            out[i * 2] = HEX[(bytes[i] >> 4) & 0xF];
            out[i * 2 + 1] = HEX[bytes[i] & 0xF];
        }
        return new String(out);
    }
}
