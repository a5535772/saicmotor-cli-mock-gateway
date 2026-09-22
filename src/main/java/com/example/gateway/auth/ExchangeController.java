package com.example.gateway.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ExchangeController {

    private static final Logger log = LoggerFactory.getLogger(ExchangeController.class);

    private final StateStore stateStore;
    private final IdpProvider idpProvider;
    private final UserDirectory userDirectory;
    private final TokenService tokenService;

    public ExchangeController(StateStore stateStore, IdpProvider idpProvider,
                              UserDirectory userDirectory, TokenService tokenService) {
        this.stateStore = stateStore;
        this.idpProvider = idpProvider;
        this.userDirectory = userDirectory;
        this.tokenService = tokenService;
    }

    @GetMapping("/auth/exchange/start")
    public Map<String, Object> start(@RequestParam int port) {
        if (port < 1024 || port > 65535) {
            return error(4000, "非法端口");
        }
        String redirectUri = "http://localhost:" + port + "/callback";
        String state = stateStore.create(redirectUri);
        String authUrl = idpProvider.buildAuthorizeUrl(state, redirectUri);
        log.debug("生成 state={}, authUrl={}", state, authUrl);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", 0);
        resp.put("msg", "ok");
        resp.put("data", Map.of("authUrl", authUrl, "state", state));
        return resp;
    }

    @PostMapping("/auth/exchange")
    public Map<String, Object> exchange(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String state = body.get("state");
        String redirectUri = "http://localhost:3000/callback";
        if (!stateStore.validate(state, redirectUri)) {
            return error(4002, "state 无效或已过期");
        }
        IdpUser idpUser;
        try {
            idpUser = idpProvider.exchangeCode(code, redirectUri);
        } catch (Exception e) {
            log.warn("code 换取失败", e);
            return error(4004, "SSO code 无效或 IdP 不可达: " + e.getMessage());
        }
        UserDirectory.User user = userDirectory.findByEmail(idpUser.email());
        if (user == null) {
            return error(4003, "未找到对应员工: " + idpUser.email());
        }
        String token = tokenService.issue(user.getUsername(), user.getUserId());
        log.info("SSO 登录成功 — email={}, username={}", idpUser.email(), user.getUsername());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", 0);
        resp.put("msg", "ok");
        resp.put("data", Map.of("token", token));
        return resp;
    }

    private Map<String, Object> error(int code, String msg) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", code);
        resp.put("msg", msg);
        resp.put("data", null);
        return resp;
    }
}