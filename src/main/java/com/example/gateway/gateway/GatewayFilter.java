package com.example.gateway.gateway;

import com.example.gateway.auth.TokenService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class GatewayFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(GatewayFilter.class);

    private final TokenService tokenService;
    private final ForwardClient forwardClient;
    private final String apiBackend;

    public GatewayFilter(TokenService tokenService, ForwardClient forwardClient,
                         @Value("${gateway.api-backend}") String apiBackend) {
        this.tokenService = tokenService;
        this.forwardClient = forwardClient;
        this.apiBackend = apiBackend;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String remote = req.getRemoteAddr();
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            log.warn("{} {} from={} — 拒绝访问 原因=缺少 Authorization header",
                    req.getMethod(), req.getRequestURI(), remote);
            writeJson(res, 401, "{\"code\":401,\"msg\":\"未登录或 token 缺失\",\"data\":null}");
            return;
        }
        String userId = tokenService.parseUserId(auth.substring(7));
        if (userId == null) {
            log.warn("{} {} from={} — 拒绝访问 原因=token 无效或已过期",
                    req.getMethod(), req.getRequestURI(), remote);
            writeJson(res, 401, "{\"code\":401,\"msg\":\"未登录或会话已过期\",\"data\":null}");
            return;
        }

        String query = req.getQueryString();
        String target = apiBackend + req.getRequestURI() + (query != null ? "?" + query : "");
        byte[] body = req.getInputStream().readAllBytes();

        long start = System.currentTimeMillis();
        log.info("→ 网关放行 {} {} from={} user={} bodyBytes={}",
                req.getMethod(), req.getRequestURI(), remote, userId, body.length);
        ForwardClient.ForwardResponse upstream = forwardClient.forward(
            req.getMethod(), target, req.getContentType(), body, userId);
        log.info("← 网关返回 {} {} user={} status={} duration={}ms",
                req.getMethod(), req.getRequestURI(), userId, upstream.status(),
                System.currentTimeMillis() - start);

        res.setStatus(upstream.status());
        if (upstream.contentType() != null) res.setContentType(upstream.contentType());
        res.getOutputStream().write(upstream.body() == null ? new byte[0] : upstream.body());
    }

    private void writeJson(HttpServletResponse res, int status, String json) throws IOException {
        res.setStatus(status);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setContentType("application/json");
        res.getWriter().write(json);
    }
}
