package com.example.gateway.auth;

public interface IdpProvider {
    String buildAuthorizeUrl(String state, String redirectUri);
    IdpUser exchangeCode(String code, String redirectUri);
}