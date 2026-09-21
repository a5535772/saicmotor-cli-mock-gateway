package com.example.gateway.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserDirectory userDirectory;
    private final TokenService tokenService;

    public AuthController(UserDirectory userDirectory, TokenService tokenService) {
        this.userDirectory = userDirectory;
        this.tokenService = tokenService;
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        UserDirectory.User user = userDirectory.findByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            log.warn("登录失败 — username={}", username);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("code", 4001);
            resp.put("msg", "账号或密码错误");
            resp.put("data", null);
            return resp;
        }
        String token = tokenService.issue(username, user.getUserId());
        log.info("登录成功 — username={}, userId={}", username, user.getUserId());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", 0);
        resp.put("msg", "ok");
        resp.put("data", Map.of("token", token));
        return resp;
    }
}
