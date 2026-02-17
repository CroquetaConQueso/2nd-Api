package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequest {
    // Usamos @SerializedName para que Retrofit envíe "email" en el JSON
    // independientemente del nombre de la variable en Java.
    @SerializedName("email")
    private String email;

    public ResetPasswordRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
