package com.example.trabajoapi.data.repository;

import com.example.trabajoapi.data.ApiService;
import com.example.trabajoapi.data.IncidenciaRequest;
import com.example.trabajoapi.data.IncidenciaResponse;
import com.example.trabajoapi.data.RetrofitClient;

import java.util.List;

import retrofit2.Call;

public class IncidenciaRepository {

    private final ApiService api;

    public IncidenciaRepository() {
        // Deja lista la API para reutilizarla en todas las llamadas de incidencias.
        this.api = RetrofitClient.getInstance().getMyApi();
    }

    // Prepara la petición de alta de incidencia y devuelve el Call para encolar/cancelar.
    public Call<Void> crearIncidencia(String bearer, IncidenciaRequest request) {
        return api.crearIncidencia(bearer, request);
    }

    // Prepara la petición del historial del usuario y devuelve el Call para encolar/cancelar.
    public Call<List<IncidenciaResponse>> getMisIncidencias(String bearer) {
        return api.getMisIncidencias(bearer);
    }
}
