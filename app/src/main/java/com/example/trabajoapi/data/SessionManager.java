package com.example.trabajoapi.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private final SharedPreferences prefs;

    private static final String PREF_NAME = "user_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_ROL = "rol";

    private static final String KEY_ID_TRABAJADOR = "id_trabajador";
    private static final String KEY_NOMBRE = "nombre";
    private static final String KEY_ID_EMPRESA = "id_empresa";

    // Centraliza lecturas y escrituras de sesión en preferencias.
    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Guarda el token de acceso para llamadas al backend.
    public void saveAuthToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    // Guarda el rol para habilitar o bloquear secciones de la app.
    public void saveRol(String rol) {
        prefs.edit().putString(KEY_ROL, rol).apply();
    }

    // Guarda el id del trabajador para peticiones que lo requieran.
    public void saveIdTrabajador(int id) {
        prefs.edit().putInt(KEY_ID_TRABAJADOR, id).apply();
    }

    // Guarda el nombre para mostrarlo o reutilizarlo sin pedirlo de nuevo.
    public void saveNombre(String nombre) {
        prefs.edit().putString(KEY_NOMBRE, nombre).apply();
    }

    // Guarda el id de empresa asociado a la sesión.
    public void saveIdEmpresa(int idEmpresa) {
        prefs.edit().putInt(KEY_ID_EMPRESA, idEmpresa).apply();
    }

    // Devuelve el token si existe, o null si la sesión no está inicializada.
    public String getAuthToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    // Mantiene compatibilidad con código antiguo que esperaba este getter.
    public String getToken() {
        return getAuthToken();
    }

    // Devuelve el token ya preparado para cabecera Authorization.
    public String getBearerToken() {
        String t = getAuthToken();
        return t == null ? null : "Bearer " + t;
    }

    // Devuelve el rol actual o un valor por defecto.
    public String getRol() {
        return prefs.getString(KEY_ROL, "Trabajador");
    }

    // Devuelve el id del trabajador o -1 si no hay sesión completa.
    public int getIdTrabajador() {
        return prefs.getInt(KEY_ID_TRABAJADOR, -1);
    }

    public String getNombre() {
        return prefs.getString(KEY_NOMBRE, null);
    }

    public int getIdEmpresa() {
        return prefs.getInt(KEY_ID_EMPRESA, -1);
    }

    // Guarda sesión usando el formato antiguo (token + rol).
    public void saveSession(String token, String rol) {
        saveAuthToken(token);
        saveRol(rol);
    }

    // Guarda sesión completa con los datos que devuelve el login.
    public void saveSession(LoginResponse r) {
        if (r == null) return;
        if (r.getAccessToken() != null) saveAuthToken(r.getAccessToken());
        if (r.getRol() != null) saveRol(r.getRol());
        saveIdTrabajador(r.getIdTrabajador());
        saveNombre(r.getNombre());
        saveIdEmpresa(r.getIdEmpresa());
    }

    // Decide si el usuario debe ver panel admin en base al rol recibido.
    public boolean isAdmin() {
        String rol = getRol();
        if (rol == null) return false;
        String r = rol.toUpperCase();
        return r.contains("ADMIN") || r.contains("GERENTE") || r.contains("JEFE");
    }

    // Limpia toda la sesión para forzar re-login sin residuos.
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
