package com.example.trabajoapi.data.repository;

import androidx.annotation.NonNull;

import com.example.trabajoapi.data.ApiService;
import com.example.trabajoapi.data.ChangePasswordRequest;
import com.example.trabajoapi.data.FichajeRequest;
import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.NfcFichajeRequest;
import com.example.trabajoapi.data.RecordatorioResponse;
import com.example.trabajoapi.data.ResumenResponse;
import com.example.trabajoapi.data.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

public class MainRepository {

    private final ApiService api;

    public MainRepository() {
        // Centraliza el acceso a la API para no repetir inicialización en cada pantalla/VM.
        this.api = RetrofitClient.getInstance().getMyApi();
    }

    // Pide el historial de fichajes del usuario.
    public void obtenerHistorial(String token, Callback<List<FichajeResponse>> callback) {
        api.obtenerHistorial(token).enqueue(callback);
    }

    // Pide el resumen de horas, opcionalmente filtrado por mes y año.
    public void getResumen(String token, Integer mes, Integer anio, Callback<ResumenResponse> callback) {
        api.getResumen(token, mes, anio).enqueue(callback);
    }

    // Consulta si hay un recordatorio pendiente para mostrar al usuario.
    public void getRecordatorio(String token, Callback<RecordatorioResponse> callback) {
        api.getRecordatorioFichaje(token).enqueue(callback);
    }

    // Lanza un fichaje estándar con GPS.
    public void fichar(String token, FichajeRequest request, Callback<FichajeResponse> callback) {
        api.fichar(token, request).enqueue(callback);
    }

    // Lanza un fichaje validando NFC + ubicación con el endpoint dedicado.
    public void ficharPorNfc(String token, double lat, double lon, String nfcData, Callback<FichajeResponse> callback) {
        NfcFichajeRequest request = new NfcFichajeRequest(nfcData, lat, lon);
        api.ficharNfc(token, request).enqueue(callback);
    }

    // Envía el cambio de contraseña para el usuario autenticado.
    public void changePassword(String token, ChangePasswordRequest request, Callback<Void> callback) {
        api.changePassword(token, request).enqueue(callback);
    }
}
