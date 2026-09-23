package com.example.gateway.auth;

import jakarta.servlet.http.HttpServletRequest;
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
    public Map<String, Object> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String remote = request.getRemoteAddr();
        log.info("账号密码登录尝试 — username={} from={}", username, remote);

        UserDirectory.User user = userDirectory.findByUsername(username);
        if (user == null) {
            log.warn("登录失败 — username={} from={} 原因=用户不存在", username, remote);
            return error(4001, "账号或密码错误");
        }
        if (!user.getPassword().equals(body.get("password"))) {
            log.warn("登录失败 — username={} userId={} from={} 原因=密码错误",
                    username, user.getUserId(), remote);
            return error(4001, "账号或密码错误");
        }
        String token = tokenService.issue(username, user.getUserId());
        log.info("登录成功 — username={} userId={} from={} token={}…",
                username, user.getUserId(), remote, TokenService.fingerprint(token));
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
