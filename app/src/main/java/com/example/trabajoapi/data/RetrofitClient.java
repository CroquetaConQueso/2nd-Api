package com.example.trabajoapi.data;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static RetrofitClient instance = null;
    private ApiService myApi;

    private static final String BASE_URL = "https://SQulito.eu.pythonanywhere.com/";

    private RetrofitClient() {
        // Construye el cliente HTTP con timeouts “realistas” para red móvil y servidor remoto.
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // Levanta Retrofit con la URL base y el convertidor JSON.
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        myApi = retrofit.create(ApiService.class);
    }

    // Mantiene una única instancia compartida para reutilizar conexiones y configuración.
    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    // Expone la interfaz de endpoints ya lista para usar desde repositorios y VMs.
    public ApiService getMyApi() {
        return myApi;
    }
}
