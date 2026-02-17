package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class EmpresaConfigResponse implements Serializable {

    @SerializedName("latitud")
    private Double latitud;

    @SerializedName("longitud")
    private Double longitud;

    @SerializedName("radio")
    private Integer radio;

    public EmpresaConfigResponse() {
    }

    public EmpresaConfigResponse(Double latitud, Double longitud, Integer radio) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.radio = radio;
    }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public Integer getRadio() { return radio; }
    public void setRadio(Integer radio) { this.radio = radio; }
}