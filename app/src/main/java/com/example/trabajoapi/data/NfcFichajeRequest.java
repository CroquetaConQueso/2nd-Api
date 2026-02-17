package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;

public class NfcFichajeRequest {

    @SerializedName("nfc_data")
    private final String nfcData;

    @SerializedName("latitud")
    private final Double latitud;

    @SerializedName("longitud")
    private final Double longitud;

    // Empaqueta el UID NFC y la ubicación para validación y registro en servidor.
    public NfcFichajeRequest(String nfcData, Double latitud, Double longitud) {
        this.nfcData = nfcData;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getNfcData() { return nfcData; }
    public Double getLatitud() { return latitud; }
    public Double getLongitud() { return longitud; }
}
