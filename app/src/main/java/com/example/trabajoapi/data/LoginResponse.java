package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("id_trabajador")
    private int idTrabajador;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("rol")
    private String rol;

    @SerializedName("id_empresa")
    private int idEmpresa;

    @SerializedName("recordatorio")
    private RecordatorioResponse recordatorio;

    // Expone los datos de sesión que devuelve el backend tras autenticar.
    public String getAccessToken() { return accessToken; }
    public int getIdTrabajador() { return idTrabajador; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
    public int getIdEmpresa() { return idEmpresa; }

    // Entrega el recordatorio opcional para mostrar avisos al entrar.
    public RecordatorioResponse getRecordatorio() { return recordatorio; }
}
