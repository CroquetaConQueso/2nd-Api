package com.example.trabajoapi.ui.admin.mapa;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.trabajoapi.data.EmpresaConfigResponse;
import com.example.trabajoapi.data.common.Event;
import com.example.trabajoapi.data.repository.AdminRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminMapaViewModel extends ViewModel {

    private final AdminRepository repo;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<EmpresaConfigResponse> config = new MutableLiveData<>();

    private final MutableLiveData<Event<String>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> goLoginEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> savedEvent = new MutableLiveData<>();

    private Call<EmpresaConfigResponse> getConfigCall;
    private Call<Void> updateConfigCall;

    public AdminMapaViewModel(AdminRepository repo) {
        this.repo = repo;
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<EmpresaConfigResponse> getConfig() { return config; }
    public LiveData<Event<String>> getToastEvent() { return toastEvent; }
    public LiveData<Event<Boolean>> getGoLoginEvent() { return goLoginEvent; }
    public LiveData<Event<Boolean>> getSavedEvent() { return savedEvent; }

    // Pide al backend la configuración de empresa y la publica para que la UI la pinte.
    public void cargarConfiguracion(String bearer) {
        if (bearer == null || bearer.trim().isEmpty()) return;

        loading.setValue(true);

        if (getConfigCall != null) getConfigCall.cancel();
        getConfigCall = repo.getEmpresaConfig(bearer);

        getConfigCall.enqueue(new Callback<EmpresaConfigResponse>() {
            @Override
            public void onResponse(Call<EmpresaConfigResponse> call, Response<EmpresaConfigResponse> response) {
                loading.postValue(false);

                if (response.code() == 401) {
                    toastEvent.postValue(new Event<>("Sesión caducada"));
                    goLoginEvent.postValue(new Event<>(true));
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    config.postValue(response.body());
                } else {
                    toastEvent.postValue(new Event<>("Error del servidor: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<EmpresaConfigResponse> call, Throwable t) {
                if (call.isCanceled()) return;
                loading.postValue(false);
                toastEvent.postValue(new Event<>("Error de red"));
            }
        });
    }

    // Envía una nueva ubicación/radio y notifica a la UI si se guardó correctamente.
    public void guardarConfiguracion(String bearer, double lat, double lon, int radio) {
        if (bearer == null || bearer.trim().isEmpty()) return;

        loading.setValue(true);

        EmpresaConfigResponse body = new EmpresaConfigResponse();
        body.setLatitud(lat);
        body.setLongitud(lon);
        body.setRadio(radio);

        if (updateConfigCall != null) updateConfigCall.cancel();
        updateConfigCall = repo.updateEmpresaConfig(bearer, body);

        updateConfigCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                loading.postValue(false);

                if (response.code() == 401) {
                    toastEvent.postValue(new Event<>("Sesión caducada"));
                    goLoginEvent.postValue(new Event<>(true));
                    return;
                }

                if (response.isSuccessful()) {
                    toastEvent.postValue(new Event<>("¡Configuración Guardada!"));
                    savedEvent.postValue(new Event<>(true));
                } else {
                    toastEvent.postValue(new Event<>("Error al guardar (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (call.isCanceled()) return;
                loading.postValue(false);
                toastEvent.postValue(new Event<>("Error de red"));
            }
        });
    }

    // Cancela llamadas pendientes para no dejar peticiones activas al cerrar el VM.
    @Override
    protected void onCleared() {
        super.onCleared();
        if (getConfigCall != null) getConfigCall.cancel();
        if (updateConfigCall != null) updateConfigCall.cancel();
    }
}
