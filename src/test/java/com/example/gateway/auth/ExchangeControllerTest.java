package com.example.gateway.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExchangeControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private IdpProvider idpProvider;

    @Test
    void startReturnsAuthUrlAndState() throws Exception {
        when(idpProvider.buildAuthorizeUrl(any(), any()))
            .thenReturn("https://accounts.feishu.cn/open-apis/authen/v1/authorize?x=1");

        mvc.perform(get("/auth/exchange/start").param("port", "3000"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.code").value(0))
           .andExpect(jsonPath("$.data.authUrl").isNotEmpty())
           .andExpect(jsonPath("$.data.state").isNotEmpty());
    }

    @Test
    void exchangeSuccessIssuesToken() throws Exception {
        when(idpProvider.buildAuthorizeUrl(any(), any())).thenReturn("https://x");
        when(idpProvider.exchangeCode(eq("C1"), any()))
            .thenReturn(new IdpUser("zhangsan@saicmotor.com", "张三"));

        String state = fetchState();
        mvc.perform(post("/auth/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"C1\",\"state\":\"" + state + "\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.code").value(0))
           .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void exchangeBadStateReturnsError() throws Exception {
        mvc.perform(post("/auth/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"C1\",\"state\":\"nope\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.code").value(4002));
    }

    @Test
    void exchangeUnmappedUserReturnsError() throws Exception {
        when(idpProvider.buildAuthorizeUrl(any(), any())).thenReturn("https://x");
        when(idpProvider.exchangeCode(any(), any()))
            .thenReturn(new IdpUser("ghost@saicmotor.com", "幽灵"));
        String state = fetchState();

        mvc.perform(post("/auth/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"C1\",\"state\":\"" + state + "\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.code").value(4003));
    }

    private String fetchState() throws Exception {
        String json = mvc.perform(get("/auth/exchange/start").param("port", "3000"))
            .andReturn().getResponse().getContentAsString();
        return com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(json).path("data").path("state").asText();
    }
}