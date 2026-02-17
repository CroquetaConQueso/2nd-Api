package com.example.trabajoapi.data;

public class FcmTokenRequest {
    private String token;

    public FcmTokenRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}