package com.example.gateway.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "saicmotor.idp")
public class IdpProperties {

    private String provider = "feishu";
    private Feishu feishu = new Feishu();
    private long stateTtlSeconds = 300;

    public static class Feishu {
        private String baseUrl = "https://open.feishu.cn";
        private String appId = "";
        private String appSecret = "";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
        public String getAppId() { return appId; }
        public void setAppId(String v) { this.appId = v; }
        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String v) { this.appSecret = v; }
    }

    public String getProvider() { return provider; }
    public void setProvider(String v) { this.provider = v; }
    public Feishu getFeishu() { return feishu; }
    public void setFeishu(Feishu v) { this.feishu = v; }
    public long getStateTtlSeconds() { return stateTtlSeconds; }
    public void setStateTtlSeconds(long v) { this.stateTtlSeconds = v; }
}
