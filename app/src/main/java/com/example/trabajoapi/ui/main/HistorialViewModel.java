package com.example.trabajoapi.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.common.Event;
import com.example.trabajoapi.data.repository.MainRepository;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistorialViewModel extends ViewModel {

    private final MainRepository repo;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<FichajeResponse>> fichajes = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Event<String>> toastEvent = new MutableLiveData<>();

    public HistorialViewModel() {
        this.repo = new MainRepository();
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<List<FichajeResponse>> getFichajes() { return fichajes; }
    public LiveData<Event<String>> getToastEvent() { return toastEvent; }

    // Carga el historial del usuario y expone progreso y resultado para la UI.
    public void cargarMisFichajes(String token) {
        loading.setValue(true);

        repo.obtenerHistorial(token, new Callback<List<FichajeResponse>>() {
            @Override
            public void onResponse(Call<List<FichajeResponse>> call, Response<List<FichajeResponse>> response) {
                loading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    fichajes.postValue(response.body());
                } else {
                    toastEvent.postValue(new Event<>("Error al cargar historial: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<FichajeResponse>> call, Throwable t) {
                loading.postValue(false);
                toastEvent.postValue(new Event<>("Error de red: " + t.getMessage()));
            }
        });
    }
}
