package com.example.trabajoapi.ui.incidencia;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.trabajoapi.data.IncidenciaRequest;
import com.example.trabajoapi.data.IncidenciaResponse;
import com.example.trabajoapi.data.common.Event;
import com.example.trabajoapi.data.repository.IncidenciaRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncidenciaViewModel extends ViewModel {

    private final IncidenciaRepository repo;

    private final MutableLiveData<Event<String>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<List<IncidenciaResponse>>> historialEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> logoutEvent = new MutableLiveData<>();

    private Call<Void> crearCall;
    private Call<List<IncidenciaResponse>> histCall;

    public IncidenciaViewModel(IncidenciaRepository repo) {
        this.repo = repo;
    }

    public LiveData<Event<String>> getToastEvent() { return toastEvent; }
    public LiveData<Event<List<IncidenciaResponse>>> getHistorialEvent() { return historialEvent; }
    public LiveData<Event<Boolean>> getLogoutEvent() { return logoutEvent; }

    // Envía una incidencia y devuelve un mensaje de confirmación o un error entendible.
    public void crearIncidencia(String bearer, String tipo, String inicio, String fin, String comentario) {
        if (bearer == null || bearer.trim().isEmpty()) return;

        if (crearCall != null) crearCall.cancel();
        crearCall = repo.crearIncidencia(bearer, new IncidenciaRequest(tipo, inicio, fin, comentario));

        crearCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    toastEvent.postValue(new Event<>("¡Solicitud Enviada!"));
                } else {
                    handleError(response);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (call.isCanceled()) return;
                toastEvent.postValue(new Event<>("Error Red: " + (t.getMessage() != null ? t.getMessage() : "")));
            }
        });
    }

    // Pide el historial de incidencias del usuario y lo publica para el diálogo/lista.
    public void cargarHistorial(String bearer) {
        if (bearer == null || bearer.trim().isEmpty()) return;

        if (histCall != null) histCall.cancel();
        histCall = repo.getMisIncidencias(bearer);

        histCall.enqueue(new Callback<List<IncidenciaResponse>>() {
            @Override
            public void onResponse(Call<List<IncidenciaResponse>> call, Response<List<IncidenciaResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    historialEvent.postValue(new Event<>(response.body()));
                } else {
                    handleError(response);
                }
            }

            @Override
            public void onFailure(Call<List<IncidenciaResponse>> call, Throwable t) {
                if (call.isCanceled()) return;
                toastEvent.postValue(new Event<>("Error Red: " + (t.getMessage() != null ? t.getMessage() : "")));
            }
        });
    }

    // Convierte códigos HTTP y cuerpos típicos en mensajes cortos y útiles para el usuario.
    private void handleError(Response<?> response) {
        if (response.code() == 401) {
            toastEvent.postValue(new Event<>("Tu sesión ha caducado"));
            logoutEvent.postValue(new Event<>(true));
            return;
        }

        String errorMsg;
        if (response.code() == 422) {
            try {
                String raw = response.errorBody() != null ? response.errorBody().string() : "";
                if (raw.contains("fecha")) errorMsg = "Revisa el formato de las fechas";
                else if (raw.contains("password")) errorMsg = "La contraseña no es válida";
                else errorMsg = "Datos incorrectos. Revisa el formulario.";
            } catch (Exception e) {
                errorMsg = "Datos no válidos";
            }
        } else if (response.code() == 403) {
            errorMsg = "No tienes permiso para hacer esto";
        } else if (response.code() >= 500) {
            errorMsg = "Error del servidor. Inténtalo luego.";
        } else {
            errorMsg = "Ocurrió un error (" + response.code() + ")";
        }

        toastEvent.postValue(new Event<>(errorMsg));
    }

    // Cancela llamadas pendientes para no dejar trabajo colgando al destruir el VM.
    @Override
    protected void onCleared() {
        super.onCleared();
        if (crearCall != null) crearCall.cancel();
        if (histCall != null) histCall.cancel();
    }
}
