package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;

public class IncidenciaResponse {
    @SerializedName("tipo")
    private String tipo;

    @SerializedName("estado")
    private String estado;

    @SerializedName("fecha_inicio")
    private String fechaInicio;

    @SerializedName("fecha_fin")
    private String fechaFin;

    @SerializedName("comentario_admin")
    private String comentarioAdmin;

    // Devuelve los campos listos para pintar en historial y detalle.
    public String getTipo() { return tipo; }
    public String getEstado() { return estado; }
    public String getFechaInicio() { return fechaInicio; }
    public String getFechaFin() { return fechaFin; }
    public String getComentarioAdmin() { return comentarioAdmin; }
}
