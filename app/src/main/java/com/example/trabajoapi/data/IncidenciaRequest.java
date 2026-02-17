package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;

public class IncidenciaRequest {
    @SerializedName("tipo")
    private String tipo;

    @SerializedName("fecha_inicio")
    private String fechaInicio;

    @SerializedName("fecha_fin")
    private String fechaFin;

    @SerializedName("comentario_trabajador")
    private String comentario;

    // Construye el payload que consume el backend para registrar una solicitud/incidencia.
    public IncidenciaRequest(String tipo, String fechaInicio, String fechaFin, String comentario) {
        this.tipo = tipo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.comentario = comentario;
    }
}
