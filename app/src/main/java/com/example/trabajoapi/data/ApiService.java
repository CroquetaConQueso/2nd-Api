package com.example.trabajoapi.data;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Autenticación y gestión de sesión.
    @POST("api/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    // Dispara el flujo de recuperación de contraseña desde el identificador (email).
    @POST("api/reset-password")
    Call<Void> resetPassword(@Body ResetPasswordRequest request);

    // Cambia la contraseña del usuario autenticado.
    @POST("api/change-password")
    Call<Void> changePassword(
            @Header("Authorization") String token,
            @Body ChangePasswordRequest request
    );

    // Registra un fichaje usando la ubicación actual.
    @POST("api/fichar")
    Call<FichajeResponse> fichar(
            @Header("Authorization") String token,
            @Body FichajeRequest request
    );

    // Registra un fichaje validando la tarjeta NFC junto con la ubicación.
    @POST("api/fichar-nfc")
    Call<FichajeResponse> ficharNfc(
            @Header("Authorization") String token,
            @Body NfcFichajeRequest request
    );

    // Devuelve los últimos fichajes del usuario.
    @GET("api/mis-fichajes")
    Call<List<FichajeResponse>> obtenerHistorial(@Header("Authorization") String token);

    // Devuelve el resumen mensual (permite filtrar por mes/año si se envía).
    @GET("api/resumen")
    Call<ResumenResponse> getResumen(
            @Header("Authorization") String token,
            @Query("mes") Integer mes,
            @Query("anio") Integer anio
    );

    // Envía una solicitud/incidencia con fechas y motivo.
    @POST("api/incidencias")
    Call<Void> crearIncidencia(
            @Header("Authorization") String token,
            @Body IncidenciaRequest request
    );

    // Lista las incidencias del usuario autenticado.
    @GET("api/incidencias")
    Call<List<IncidenciaResponse>> getMisIncidencias(@Header("Authorization") String token);

    // Asocia el token FCM del dispositivo a la cuenta para recibir avisos.
    @POST("api/save-fcm-token")
    Call<Void> saveFcmToken(
            @Header("Authorization") String token,
            @Body FcmTokenRequest request
    );

    // Devuelve el listado de empleados (zona admin).
    @GET("api/empleados")
    Call<List<TrabajadorResponse>> getEmpleados(@Header("Authorization") String token);

    // Devuelve el historial de fichajes de un empleado concreto (zona admin).
    @GET("api/fichajes-empleado/{id}")
    Call<List<FichajeResponse>> getFichajesEmpleado(
            @Header("Authorization") String token,
            @Path("id") int idEmpleado
    );

    // Carga la configuración de empresa (ubicación/radio).
    @GET("api/empresa/config")
    Call<EmpresaConfigResponse> getEmpresaConfig(@Header("Authorization") String token);

    // Guarda la configuración de empresa (ubicación/radio).
    @POST("api/empresa/config")
    Call<Void> updateEmpresaConfig(
            @Header("Authorization") String token,
            @Body EmpresaConfigResponse config
    );

    // Pide al servidor un recordatorio de fichaje si procede.
    @GET("api/recordatorio-fichaje")
    Call<RecordatorioResponse> getRecordatorioFichaje(@Header("Authorization") String token);

    // Actualiza el NFC principal de la empresa para validar fichajes.
    @POST("api/empresa/config-nfc")
    Call<Void> updateEmpresaNfc(
            @Header("Authorization") String token,
            @Body NfcFichajeRequest request
    );
}
