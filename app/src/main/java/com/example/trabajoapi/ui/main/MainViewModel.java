package com.example.trabajoapi.ui.main;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.trabajoapi.data.ChangePasswordRequest;
import com.example.trabajoapi.data.FichajeRequest;
import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.RecordatorioResponse;
import com.example.trabajoapi.data.ResumenResponse;
import com.example.trabajoapi.data.common.Event;
import com.example.trabajoapi.data.repository.MainRepository;

import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel principal de la pantalla Main.
 *
 * Responsabilidad:
 * - Exponer estado observable para la UI (dentro/fuera, resumen mensual, historial).
 * - Coordinar llamadas al repositorio para fichar, consultar historial/resumen, recordatorios y cambio de contraseña.
 * - Traducir respuestas del backend a mensajes de UI (toast/diálogo), sin tocar la vista directamente.
 *
 * Mantiene MVVM:
 * - La Activity observa LiveData<Event<...>> y decide cómo mostrarlo.
 * - El ViewModel no conoce Views ni Context.
 */
public class MainViewModel extends ViewModel {

    private final MainRepository repo;

    // Estado de presencia: true = el último fichaje es ENTRADA (está "dentro"), false = SALIDA o vacío.
    private final MutableLiveData<Boolean> dentro = new MutableLiveData<>(false);

    // Resumen mensual (horas teóricas, trabajadas, saldo, etc).
    private final MutableLiveData<ResumenResponse> resumen = new MutableLiveData<>();

    // Historial (normalmente últimos 50 fichajes).
    private final MutableLiveData<List<FichajeResponse>> historial = new MutableLiveData<>();

    // Eventos "one-shot" para UI (no deben repetirse en rotaciones).
    private final MutableLiveData<Event<String>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<RecordatorioResponse>> recordatorioEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> logoutEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<List<FichajeResponse>>> historialDialogEvent = new MutableLiveData<>();

    public MainViewModel(MainRepository repo) {
        this.repo = repo;
    }

    // --- GETTERS para observación en la Activity ---

    public LiveData<Boolean> getDentro() { return dentro; }
    public LiveData<ResumenResponse> getResumen() { return resumen; }
    public LiveData<List<FichajeResponse>> getHistorial() { return historial; }
    public LiveData<Event<String>> getToastEvent() { return toastEvent; }
    public LiveData<Event<RecordatorioResponse>> getRecordatorioEvent() { return recordatorioEvent; }
    public LiveData<Event<Boolean>> getLogoutEvent() { return logoutEvent; }
    public LiveData<Event<List<FichajeResponse>>> getHistorialDialogEvent() { return historialDialogEvent; }

    /**
     * Refresca lo necesario para pintar el dashboard:
     * - Estado de fichaje (dentro/fuera).
     * - Resumen de horas extra.
     */
    public void cargarDashboard(@NonNull String bearer) {
        consultarEstadoFichaje(bearer);
        obtenerHorasExtra(bearer);
    }

    /**
     * Consulta historial y deduce el estado:
     * - Si el último fichaje (lista[0]) es ENTRADA => dentro=true
     * - Si es SALIDA o no hay fichajes => dentro=false
     *
     * Nota: depende de que el backend devuelva el historial ordenado DESC en /mis-fichajes.
     */
    public void consultarEstadoFichaje(@NonNull String bearer) {
        repo.obtenerHistorial(bearer, new Callback<List<FichajeResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<FichajeResponse>> call, @NonNull Response<List<FichajeResponse>> response) {
                if (response.code() == 401) {
                    logoutEvent.postValue(new Event<>(true));
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<FichajeResponse> lista = response.body();
                    historial.postValue(lista);

                    boolean dentroNow = !lista.isEmpty()
                            && "ENTRADA".equalsIgnoreCase(lista.get(0).getTipo());

                    dentro.postValue(dentroNow);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<FichajeResponse>> call, @NonNull Throwable t) {
                toastEvent.postValue(new Event<>("Sin conexión al servidor"));
            }
        });
    }

    /**
     * Recupera el resumen mensual (por defecto mes/año actuales) y lo expone a la UI.
     */
    public void obtenerHorasExtra(@NonNull String bearer) {
        repo.getResumen(bearer, null, null, new Callback<ResumenResponse>() {
            @Override
            public void onResponse(@NonNull Call<ResumenResponse> call, @NonNull Response<ResumenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resumen.postValue(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResumenResponse> call, @NonNull Throwable t) { }
        });
    }

    /**
     * Fichaje manual:
     * - Enviamos lat/lon, nfc_data = null.
     * - El backend decide si corresponde ENTRADA o SALIDA.
     */
    public void fichar(@NonNull String bearer, double lat, double lon, String ignorarNfc) {
        FichajeRequest req = new FichajeRequest(lat, lon, null);
        repo.fichar(bearer, req, new Callback<FichajeResponse>() {
            @Override
            public void onResponse(@NonNull Call<FichajeResponse> call, @NonNull Response<FichajeResponse> response) {
                manejarRespuestaFichaje(response, bearer, "Manual");
            }

            @Override
            public void onFailure(@NonNull Call<FichajeResponse> call, @NonNull Throwable t) {
                toastEvent.postValue(new Event<>("Error de red: revisa tu conexión"));
            }
        });
    }

    /**
     * Fichaje por NFC:
     * - Enviamos lat/lon + nfcId leído del tag.
     * - El backend valida si el NFC es el de oficina o el personal (según configuración).
     */
    public void realizarFichajeNfc(@NonNull String bearer, double lat, double lon, String nfcId) {
        repo.ficharPorNfc(bearer, lat, lon, nfcId, new Callback<FichajeResponse>() {
            @Override
            public void onResponse(@NonNull Call<FichajeResponse> call, @NonNull Response<FichajeResponse> response) {
                manejarRespuestaFichaje(response, bearer, "NFC");
            }

            @Override
            public void onFailure(@NonNull Call<FichajeResponse> call, @NonNull Throwable t) {
                toastEvent.postValue(new Event<>("Error de conexión al fichar por NFC"));
            }
        });
    }

    /**
     * Interpreta una respuesta de /fichar o /fichar-nfc:
     * - Si éxito: publica mensaje, actualiza "dentro" y refresca datos (resumen + estado).
     * - Si error: traduce el error del backend a un texto entendible.
     */
    private void manejarRespuestaFichaje(Response<FichajeResponse> response, String bearer, String origen) {
        if (response.code() == 401) {
            toastEvent.postValue(new Event<>("Sesión caducada. Entra de nuevo."));
            logoutEvent.postValue(new Event<>(true));
            return;
        }

        if (response.isSuccessful() && response.body() != null) {
            String tipo = response.body().getTipo();

            String mensajeExito = "ENTRADA".equalsIgnoreCase(tipo)
                    ? "Bienvenido. Has registrado tu entrada."
                    : "Hasta luego. Has registrado tu salida.";

            toastEvent.postValue(new Event<>(mensajeExito));

            boolean dentroNow = "ENTRADA".equalsIgnoreCase(tipo);
            dentro.postValue(dentroNow);

            // Refresca datos visibles después del fichaje (evita UI desincronizada).
            obtenerHorasExtra(bearer);
            consultarEstadoFichaje(bearer);

        } else {
            String mensajeAmigable = analizarErrorServer(response);
            toastEvent.postValue(new Event<>(mensajeAmigable));
        }
    }

    /**
     * Traduce el error del backend a mensajes cortos para el usuario.
     * Se intenta leer JSON {"message": "..."} si existe.
     */
    private String analizarErrorServer(Response<?> response) {
        try {
            String errorJson = response.errorBody() != null ? response.errorBody().string() : "";

            String mensajeOriginal = "";
            try {
                JSONObject json = new JSONObject(errorJson);
                if (json.has("message")) mensajeOriginal = json.getString("message");
                else if (json.has("status")) mensajeOriginal = json.getString("status");
            } catch (Exception e) {
                mensajeOriginal = errorJson;
            }

            String m = mensajeOriginal.toLowerCase();

            // Error típico del backend cuando valida distancia GPS.
            if (m.contains("lejos")) {
                String extra = "";
                int idx = mensajeOriginal.indexOf("(");
                if (idx >= 0) extra = " " + mensajeOriginal.substring(idx);
                return "Estás demasiado lejos de la oficina." + extra;
            }

            // Empresa exige NFC de oficina y no se está usando.
            if (m.contains("restringido") || m.contains("escanear el nfc")) {
                return "Debes fichar en el punto NFC de la entrada.";
            }

            // NFC no coincide (oficial/personal según reglas del backend).
            if (m.contains("nfc incorrecto") || m.contains("no válido") || m.contains("no valido")) {
                return "NFC no reconocido. Usa el punto NFC oficial.";
            }

            // Mensajes de GPS inactivo.
            if (m.contains("gps")) {
                return "Activa el GPS de alta precisión para fichar.";
            }

            if (response.code() == 403) return "Acceso denegado.";
            if (response.code() == 404) return "Recurso no encontrado.";
            if (response.code() >= 500) return "Error del servidor. Inténtalo más tarde.";

            return !mensajeOriginal.isEmpty() ? mensajeOriginal : "Error desconocido al fichar.";

        } catch (Exception e) {
            return "Error procesando la respuesta del servidor.";
        }
    }

    /**
     * Consulta al backend si hay que avisar al usuario por falta de fichaje.
     *
     * Comportamiento actual:
     * - 401 => forzar logout
     * - 204 => no hay recordatorio (silencioso)
     * - 200 con body => si avisar=true o hay texto, emite recordatorioEvent
     *
     * Esto permite que el backend decida si es falta de ENTRADA o SALIDA.
     */
    public void comprobarRecordatorio(@NonNull String bearer) {
        repo.getRecordatorio(bearer, new Callback<RecordatorioResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecordatorioResponse> call, @NonNull Response<RecordatorioResponse> response) {

                if (response.code() == 401) {
                    logoutEvent.postValue(new Event<>(true));
                    return;
                }

                // Si el backend devuelve 204, significa "no hay nada que avisar".
                if (response.code() == 204) {
                    return;
                }

                if (!response.isSuccessful()) {
                    return;
                }

                RecordatorioResponse r = response.body();
                if (r == null) return;

                boolean debeMostrar = false;

                // Caso ideal: backend envía avisar=true.
                try {
                    if (r.isAvisar()) {
                        debeMostrar = true;
                    }
                } catch (Exception ignored) { }

                // Fallback: si no existe avisar pero hay título/mensaje, también lo mostramos.
                if (!debeMostrar) {
                    String t = null;
                    String m = null;
                    try { t = r.getTitulo(); } catch (Exception ignored) { }
                    try { m = r.getMensaje(); } catch (Exception ignored) { }

                    boolean hayTexto =
                            (t != null && !t.trim().isEmpty()) ||
                                    (m != null && !m.trim().isEmpty());

                    if (hayTexto) debeMostrar = true;
                }

                if (debeMostrar) {
                    recordatorioEvent.postValue(new Event<>(r));
                }
            }

            @Override
            public void onFailure(@NonNull Call<RecordatorioResponse> call, @NonNull Throwable t) { }
        });
    }

    /**
     * Recupera historial y lo publica como Event para abrir un diálogo en la UI.
     */
    public void pedirHistorialParaDialogo(@NonNull String bearer) {
        repo.obtenerHistorial(bearer, new Callback<List<FichajeResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<FichajeResponse>> call, @NonNull Response<List<FichajeResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    historialDialogEvent.postValue(new Event<>(response.body()));
                } else {
                    toastEvent.postValue(new Event<>("No se pudo cargar el historial"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<FichajeResponse>> call, @NonNull Throwable t) {
                toastEvent.postValue(new Event<>("Error de red"));
            }
        });
    }

    /**
     * Solicita cambio de contraseña.
     */
    public void cambiarPassword(@NonNull String bearer, @NonNull String actual, @NonNull String nueva) {
        ChangePasswordRequest req = new ChangePasswordRequest(actual, nueva);
        repo.changePassword(bearer, req, new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) toastEvent.postValue(new Event<>("Contraseña actualizada"));
                else toastEvent.postValue(new Event<>("Error al cambiar contraseña"));
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                toastEvent.postValue(new Event<>("Error de red"));
            }
        });
    }
}
