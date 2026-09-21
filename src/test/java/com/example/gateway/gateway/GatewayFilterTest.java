package com.example.gateway.gateway;

import com.example.gateway.auth.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GatewayFilterTest {

    @Test
    void forwardsLeaveWithInjectedUserId() throws Exception {
        TokenService tokens = mock(TokenService.class);
        when(tokens.parseUserId("tok")).thenReturn("zhangsan");
        ForwardClient client = mock(ForwardClient.class);
        when(client.forward(eq("GET"), eq("http://localhost:8080/leave/balance"), isNull(), any(), eq("zhangsan")))
            .thenReturn(new ForwardClient.ForwardResponse(200, "application/json", "{\"code\":0}".getBytes()));

        GatewayFilter filter = new GatewayFilter(tokens, client, "http://localhost:8080");
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/leave/balance");
        req.addHeader("Authorization", "Bearer tok");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (rq, rs) -> {});

        assertEquals(200, res.getStatus());
        verify(client).forward(eq("GET"), eq("http://localhost:8080/leave/balance"), isNull(), any(), eq("zhangsan"));
    }

    @Test
    void forwardsAttendanceWithInjectedUserId() throws Exception {
        TokenService tokens = mock(TokenService.class);
        when(tokens.parseUserId("tok")).thenReturn("zhangsan");
        ForwardClient client = mock(ForwardClient.class);
        when(client.forward(eq("GET"), eq("http://localhost:8080/attendance/records"), isNull(), any(), eq("zhangsan")))
            .thenReturn(new ForwardClient.ForwardResponse(200, "application/json", "{\"code\":0}".getBytes()));

        GatewayFilter filter = new GatewayFilter(tokens, client, "http://localhost:8080");
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/attendance/records");
        req.addHeader("Authorization", "Bearer tok");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (rq, rs) -> {});

        assertEquals(200, res.getStatus());
        verify(client).forward(eq("GET"), eq("http://localhost:8080/attendance/records"), isNull(), any(), eq("zhangsan"));
    }

    @Test
    void missingTokenReturns401WithoutForwarding() throws Exception {
        TokenService tokens = mock(TokenService.class);
        ForwardClient client = mock(ForwardClient.class);
        GatewayFilter filter = new GatewayFilter(tokens, client, "http://localhost:8080");

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/leave/balance");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, (rq, rs) -> {});

        assertEquals(401, res.getStatus());
        assertEquals("{\"code\":401,\"msg\":\"未登录或 token 缺失\",\"data\":null}", res.getContentAsString());
        verify(client, never()).forward(anyString(), anyString(), any(), any(), any());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        TokenService tokens = mock(TokenService.class);
        when(tokens.parseUserId("bad")).thenReturn(null);
        ForwardClient client = mock(ForwardClient.class);
        GatewayFilter filter = new GatewayFilter(tokens, client, "http://localhost:8080");

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/leave/balance");
        req.addHeader("Authorization", "Bearer bad");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, (rq, rs) -> {});

        assertEquals(401, res.getStatus());
        assertEquals("{\"code\":401,\"msg\":\"未登录或会话已过期\",\"data\":null}", res.getContentAsString());
        verify(client, never()).forward(anyString(), anyString(), any(), any(), any());
    }
}