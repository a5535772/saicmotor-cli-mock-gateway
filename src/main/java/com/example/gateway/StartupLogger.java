package com.example.gateway;

import com.example.gateway.auth.IdpProperties;
import com.example.gateway.auth.UserDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    private final int port;
    private final String apiBackend;
    private final UserDirectory userDirectory;
    private final IdpProperties idpProperties;

    public StartupLogger(@Value("${server.port:8081}") int port,
                         @Value("${gateway.api-backend}") String apiBackend,
                         UserDirectory userDirectory, IdpProperties idpProperties) {
        this.port = port;
        this.apiBackend = apiBackend;
        this.userDirectory = userDirectory;
        this.idpProperties = idpProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        int userCount = userDirectory.getUsers() == null ? 0 : userDirectory.getUsers().size();
        String appId = idpProperties.getFeishu().getAppId();
        String appSecret = idpProperties.getFeishu().getAppSecret();
        log.info("mock-gateway 启动完成 — port={} apiBackend={} 用户数={} IdP={} stateTtl={}s 飞书凭据={}",
                port, apiBackend, userCount, idpProperties.getProvider(),
                idpProperties.getStateTtlSeconds(),
                (!appId.isBlank() && !appSecret.isBlank()) ? "已配置(appId=" + appId + ")" : "未配置");
    }
}
