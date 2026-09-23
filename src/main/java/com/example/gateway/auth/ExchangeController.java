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
        log.info("发起 SSO 登录 — clientPort={}", port);
        if (port < 1024 || port > 65535) {
            log.warn("SSO 发起失败 — clientPort={} 原因=非法端口", port);
            return error(4000, "非法端口");
        }
        String redirectUri = "http://localhost:" + port + "/callback";
        String state = stateStore.create(redirectUri);
        String authUrl = idpProvider.buildAuthorizeUrl(state, redirectUri);
        log.info("SSO 授权链接已生成 — clientPort={} state={} redirectUri={}", port, state, redirectUri);
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
        log.info("SSO code 换 token — state={}", state);

        if (!stateStore.validate(state, redirectUri)) {
            log.warn("SSO 换 token 失败 — state={} 原因=state 无效或已过期", state);
            return error(4002, "state 无效或已过期");
        }
        IdpUser idpUser;
        try {
            idpUser = idpProvider.exchangeCode(code, redirectUri);
        } catch (Exception e) {
            log.warn("SSO 换 token 失败 — state={} 原因=code 换取失败: {}", state, e.getMessage());
            return error(4004, "SSO code 无效或 IdP 不可达: " + e.getMessage());
        }
        UserDirectory.User user = userDirectory.findByUsername(idpUser.name());
        if (user == null) {
            log.warn("SSO 换 token 失败 — state={} 原因=IdP 用户未匹配到员工 name={} email={}",
                    state, idpUser.name(), idpUser.email());
            return error(4003, "未找到对应员工: " + idpUser.name());
        }
        String token = tokenService.issue(user.getUsername(), user.getUserId());
        log.info("SSO 登录成功 — name={} email={} → username={} userId={} token={}…",
                idpUser.name(), idpUser.email(), user.getUsername(), user.getUserId(),
                TokenService.fingerprint(token));
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
