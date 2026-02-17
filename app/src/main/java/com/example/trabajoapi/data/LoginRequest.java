package com.example.trabajoapi.data;

public class LoginRequest {
    private String nif;
    private String password;

    // Empaqueta credenciales para el endpoint de login.
    public LoginRequest(String nif, String password) {
        this.nif = nif;
        this.password = password;
    }
}
