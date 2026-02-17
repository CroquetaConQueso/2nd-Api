package com.example.trabajoapi.data.repository;

import com.example.trabajoapi.data.ApiService;
import com.example.trabajoapi.data.EmpresaConfigResponse;
import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.NfcFichajeRequest;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.TrabajadorResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

public class AdminRepository {

    private final ApiService api;

    public AdminRepository() {
        // Centraliza las llamadas del panel admin con el mismo cliente Retrofit.
        this.api = RetrofitClient.getInstance().getMyApi();
    }

    // Pide la lista de empleados disponible para el rol administrador.
    public Call<List<TrabajadorResponse>> getEmpleados(String bearerToken) {
        return api.getEmpleados(bearerToken);
    }

    // Devuelve el Call del historial del empleado para poder controlarlo desde el VM.
    public Call<List<FichajeResponse>> getFichajesDeEmpleado(String token, int idEmpleado) {
        return RetrofitClient.getInstance().getMyApi().getFichajesEmpleado(token, idEmpleado);
    }

    // Variante equivalente usando la instancia ya guardada en el repositorio.
    public Call<List<FichajeResponse>> getFichajesEmpleado(String bearerToken, int idEmpleado) {
        return api.getFichajesEmpleado(bearerToken, idEmpleado);
    }

    // Carga la configuración actual de ubicación/radio de la empresa.
    public Call<EmpresaConfigResponse> getEmpresaConfig(String bearerToken) {
        return api.getEmpresaConfig(bearerToken);
    }

    // Envía la configuración nueva de ubicación/radio al backend.
    public Call<Void> updateEmpresaConfig(String bearerToken, EmpresaConfigResponse config) {
        return api.updateEmpresaConfig(bearerToken, config);
    }

    // Guarda el NFC principal de la empresa para que el fichaje NFC se pueda validar.
    public void updateEmpresaNfc(String token, String nfcData, Callback<Void> callback) {
        NfcFichajeRequest request = new NfcFichajeRequest(nfcData, 0.0, 0.0);
        api.updateEmpresaNfc(token, request).enqueue(callback);
    }
}
