package com.example.trabajoapi.ui.admin.empleados;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.common.Event;
import com.example.trabajoapi.data.repository.AdminRepository;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminFichajesViewModel extends ViewModel {

    private final AdminRepository repo;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<FichajeResponse>> fichajes = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Event<String>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> goLoginEvent = new MutableLiveData<>();

    private Call<List<FichajeResponse>> call;

    public AdminFichajesViewModel(AdminRepository repo) {
        this.repo = repo;
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<List<FichajeResponse>> getFichajes() { return fichajes; }
    public LiveData<Event<String>> getToastEvent() { return toastEvent; }
    public LiveData<Event<Boolean>> getGoLoginEvent() { return goLoginEvent; }

    // Pide el historial de un empleado y publica lista, mensajes y estado de sesión.
    public void cargarHistorial(String token, int idEmpleado) {
        if (token == null) return;

        loading.setValue(true);

        if (call != null) call.cancel();
        call = repo.getFichajesDeEmpleado(token, idEmpleado);

        call.enqueue(new Callback<List<FichajeResponse>>() {
            @Override
            public void onResponse(Call<List<FichajeResponse>> call, Response<List<FichajeResponse>> response) {
                loading.postValue(false);

                if (response.code() == 401) {
                    toastEvent.postValue(new Event<>("Sesión caducada"));
                    goLoginEvent.postValue(new Event<>(true));
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    fichajes.postValue(response.body());
                    if (response.body().isEmpty()) {
                        toastEvent.postValue(new Event<>("Este empleado no tiene fichajes."));
                    }
                } else {
                    toastEvent.postValue(new Event<>("Error servidor: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<FichajeResponse>> call, Throwable t) {
                if (call.isCanceled()) return;
                loading.postValue(false);
                toastEvent.postValue(new Event<>("Error de red: " + t.getMessage()));
            }
        });
    }

    // Limpia llamadas activas cuando el VM se destruye.
    @Override
    protected void onCleared() {
        super.onCleared();
        if (call != null) call.cancel();
    }
}
