package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;

public class FichajeRequest {
    private Double latitud;
    private Double longitud;

    @SerializedName("nfc_data")
    private String nfcData;

    // Construye el fichaje con ubicación y deja NFC vacío para el modo manual.
    public FichajeRequest(Double latitud, Double longitud) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.nfcData = null;
    }

    // Construye el fichaje con ubicación y UID NFC para validar el torno/tarjeta.
    public FichajeRequest(Double latitud, Double longitud, String nfcData) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.nfcData = nfcData;
    }
}
