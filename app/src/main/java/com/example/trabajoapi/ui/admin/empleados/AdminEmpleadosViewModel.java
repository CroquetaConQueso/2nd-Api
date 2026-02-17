package com.example.trabajoapi.ui.admin.empleados;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.TrabajadorResponse;
import com.example.trabajoapi.data.common.Event;
import com.example.trabajoapi.data.repository.AdminRepository;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminEmpleadosViewModel extends ViewModel {

    public static class EmpleadoFichajesUI {
        private final String empleadoNombre;
        private final List<FichajeResponse> fichajes;

        public EmpleadoFichajesUI(String empleadoNombre, List<FichajeResponse> fichajes) {
            this.empleadoNombre = empleadoNombre;
            this.fichajes = fichajes;
        }

        public String getEmpleadoNombre() { return empleadoNombre; }
        public List<FichajeResponse> getFichajes() { return fichajes; }
    }

    private final AdminRepository repo;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<TrabajadorResponse>> empleados = new MutableLiveData<>(Collections.emptyList());

    private final MutableLiveData<Event<String>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> goLoginEvent = new MutableLiveData<>();

    private final MutableLiveData<Event<EmpleadoFichajesUI>> fichajesEmpleadoEvent = new MutableLiveData<>();

    private Call<List<TrabajadorResponse>> empleadosCall;
    private Call<List<FichajeResponse>> fichajesCall;

    public AdminEmpleadosViewModel(AdminRepository repo) {
        this.repo = repo;
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<List<TrabajadorResponse>> getEmpleados() { return empleados; }
    public LiveData<Event<String>> getToastEvent() { return toastEvent; }
    public LiveData<Event<Boolean>> getGoLoginEvent() { return goLoginEvent; }
    public LiveData<Event<EmpleadoFichajesUI>> getFichajesEmpleadoEvent() { return fichajesEmpleadoEvent; }

    // Carga la lista de empleados y la publica para que la pantalla la pinte.
    public void cargarEmpleados(String bearer) {
        if (bearer == null || bearer.trim().isEmpty()) return;

        loading.setValue(true);

        if (empleadosCall != null) empleadosCall.cancel();
        empleadosCall = repo.getEmpleados(bearer);

        empleadosCall.enqueue(new Callback<List<TrabajadorResponse>>() {
            @Override
            public void onResponse(Call<List<TrabajadorResponse>> call, Response<List<TrabajadorResponse>> response) {
                loading.postValue(false);

                if (response.code() == 401) {
                    toastEvent.postValue(new Event<>("Sesión caducada"));
                    goLoginEvent.postValue(new Event<>(true));
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<TrabajadorResponse> data = response.body();
                    empleados.postValue(data);

                    if (data.isEmpty()) {
                        toastEvent.postValue(new Event<>("La lista está vacía"));
                    }
                } else {
                    toastEvent.postValue(new Event<>("Error del servidor: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<TrabajadorResponse>> call, Throwable t) {
                if (call.isCanceled()) return;
                loading.postValue(false);
                toastEvent.postValue(new Event<>("Error de red: " + (t.getMessage() != null ? t.getMessage() : "")));
            }
        });
    }

    // Pide los fichajes de un empleado y emite un evento preparado para mostrar en UI.
    public void cargarFichajesEmpleado(String bearer, int idEmpleado, String nombreEmpleado) {
        if (bearer == null || bearer.trim().isEmpty()) return;

        loading.setValue(true);

        if (fichajesCall != null) fichajesCall.cancel();
        fichajesCall = repo.getFichajesEmpleado(bearer, idEmpleado);

        fichajesCall.enqueue(new Callback<List<FichajeResponse>>() {
            @Override
            public void onResponse(Call<List<FichajeResponse>> call, Response<List<FichajeResponse>> response) {
                loading.postValue(false);

                if (response.code() == 401) {
                    toastEvent.postValue(new Event<>("Sesión caducada"));
                    goLoginEvent.postValue(new Event<>(true));
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<FichajeResponse> data = response.body();
                    fichajesEmpleadoEvent.postValue(new Event<>(new EmpleadoFichajesUI(nombreEmpleado, data)));
                } else {
                    toastEvent.postValue(new Event<>("Error del servidor: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<FichajeResponse>> call, Throwable t) {
                if (call.isCanceled()) return;
                loading.postValue(false);
                toastEvent.postValue(new Event<>("Error de red: " + (t.getMessage() != null ? t.getMessage() : "")));
            }
        });
    }

    // Cancela llamadas pendientes para no dejar peticiones activas al cerrar el VM.
    @Override
    protected void onCleared() {
        super.onCleared();
        if (empleadosCall != null) empleadosCall.cancel();
        if (fichajesCall != null) fichajesCall.cancel();
    }
}
