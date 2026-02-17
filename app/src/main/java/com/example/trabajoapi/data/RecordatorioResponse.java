package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;

public class RecordatorioResponse {

    @SerializedName("avisar")
    private Boolean avisar;

    @SerializedName("titulo")
    private String titulo;

    @SerializedName("mensaje")
    private String mensaje;

    // Permite distinguir entre “no viene el campo” y “viene false”.
    public boolean hasAvisarFlag() {
        return avisar != null;
    }

    // Indica si el backend pide mostrar el recordatorio.
    public boolean isAvisar() {
        return avisar != null && avisar;
    }

    public String getTitulo() { return titulo; }
    public String getMensaje() { return mensaje; }
}
