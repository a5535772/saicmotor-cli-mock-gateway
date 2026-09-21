package com.example.gateway.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "gateway")
public class UserDirectory {

    private List<User> users;

    public static class User {
        private String username;
        private String password;
        private String userId;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    public User findByUsername(String username) {
        if (users == null) return null;
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
    }
}
