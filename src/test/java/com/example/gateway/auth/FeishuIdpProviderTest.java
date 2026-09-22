package com.example.gateway.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.jupiter.api.Assertions.*;

class FeishuIdpProviderTest {

    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://open.feishu.cn");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private FeishuIdpProvider provider() {
        IdpProperties props = new IdpProperties();
        props.getFeishu().setAppId("app1");
        props.getFeishu().setAppSecret("sec1");
        return new FeishuIdpProvider(props, builder);
    }

    @Test
    void exchangesCodeForUserEmail() {
        server.expect(requestTo("https://open.feishu.cn/open-apis/auth/v3/app_access_token/internal"))
              .andRespond(withSuccess("{\"code\":0,\"app_access_token\":\"app-tok\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://open.feishu.cn/open-apis/authen/v1/oidc/access_token"))
              .andRespond(withSuccess("{\"code\":0,\"data\":{\"access_token\":\"u-tok\"}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://open.feishu.cn/open-apis/authen/v1/user_info"))
              .andRespond(withSuccess("{\"code\":0,\"data\":{\"email\":\"zhangsan@saicmotor.com\",\"name\":\"张三\"}}", MediaType.APPLICATION_JSON));

        IdpUser user = provider().exchangeCode("auth-code", "http://localhost:3000/callback");
        assertEquals("zhangsan@saicmotor.com", user.email());
        assertEquals("张三", user.name());
        server.verify();
    }

    @Test
    void throwsWhenFeishuCodeInvalid() {
        server.expect(requestTo("https://open.feishu.cn/open-apis/auth/v3/app_access_token/internal"))
              .andRespond(withSuccess("{\"code\":0,\"app_access_token\":\"app-tok\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://open.feishu.cn/open-apis/authen/v1/oidc/access_token"))
              .andRespond(withSuccess("{\"code\":20036,\"msg\":\"invalid code\"}", MediaType.APPLICATION_JSON));

        assertThrows(RuntimeException.class,
            () -> provider().exchangeCode("bad", "http://localhost:3000/callback"));
    }
}