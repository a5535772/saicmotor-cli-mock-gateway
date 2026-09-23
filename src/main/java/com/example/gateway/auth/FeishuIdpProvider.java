package com.example.gateway.auth;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class FeishuIdpProvider implements IdpProvider {

    private static final Logger log = LoggerFactory.getLogger(FeishuIdpProvider.class);

    private final IdpProperties props;
    private final RestClient client;

    public FeishuIdpProvider(IdpProperties props, RestClient.Builder clientBuilder) {
        this.props = props;
        this.client = clientBuilder.baseUrl(props.getFeishu().getBaseUrl()).build();
    }

    @Override
    public String buildAuthorizeUrl(String state, String redirectUri) {
        String base = props.getFeishu().getBaseUrl().replace("open.feishu.cn", "accounts.feishu.cn");
        return base + "/open-apis/authen/v1/authorize?app_id=" + enc(props.getFeishu().getAppId())
            + "&redirect_uri=" + enc(redirectUri) + "&state=" + enc(state);
    }

    @Override
    public IdpUser exchangeCode(String code, String redirectUri) {
        log.info("调用飞书换取用户身份 — redirectUri={}", redirectUri);
        String appToken = fetchAppAccessToken();

        long t1 = System.currentTimeMillis();
        JsonNode tokenResp = client.post()
            .uri("/open-apis/authen/v1/oidc/access_token")
            .header("Authorization", "Bearer " + appToken)
            .body(Map.of("grant_type", "authorization_code", "code", code))
            .retrieve().body(JsonNode.class);
        ensureFeishuCode(tokenResp, "oidc/access_token");
        String userAccessToken = tokenResp.path("data").path("access_token").asText();
        log.info("飞书 oidc/access_token 成功 — duration={}ms", System.currentTimeMillis() - t1);

        long t2 = System.currentTimeMillis();
        JsonNode info = client.get()
            .uri("/open-apis/authen/v1/user_info")
            .header("Authorization", "Bearer " + userAccessToken)
            .retrieve().body(JsonNode.class);
        ensureFeishuCode(info, "user_info");
        JsonNode data = info.path("data");
        log.info("飞书 user_info 成功 — duration={}ms name={} email={}",
                System.currentTimeMillis() - t2,
                data.path("name").asText(null),
                firstNonEmpty(data, "email", "enterprise_email"));
        log.debug("user_info response: {}", data);

        String name = data.path("name").asText(null);
        String email = firstNonEmpty(data, "email", "enterprise_email");
        return new IdpUser(email, name);
    }

    private static String firstNonEmpty(JsonNode node, String... fields) {
        for (String f : fields) {
            String v = node.path(f).asText(null);
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }

    private String fetchAppAccessToken() {
        long start = System.currentTimeMillis();
        JsonNode resp = client.post()
            .uri("/open-apis/auth/v3/app_access_token/internal")
            .body(Map.of("app_id", props.getFeishu().getAppId(),
                         "app_secret", props.getFeishu().getAppSecret()))
            .retrieve().body(JsonNode.class);
        ensureFeishuCode(resp, "app_access_token");
        log.info("飞书 app_access_token 获取成功 — duration={}ms appId={}",
                System.currentTimeMillis() - start, props.getFeishu().getAppId());
        return resp.path("app_access_token").asText();
    }

    private void ensureFeishuCode(JsonNode node, String api) {
        if (node == null || node.path("code").asInt(-1) != 0) {
            int feishuCode = node == null ? -1 : node.path("code").asInt(-1);
            String msg = node == null ? "无响应" : node.path("msg").asText("飞书返回错误");
            log.warn("飞书接口返回错误 — api={} code={} msg={}", api, feishuCode, msg);
            throw new RuntimeException("飞书接口错误(" + api + " code=" + feishuCode + "): " + msg);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
