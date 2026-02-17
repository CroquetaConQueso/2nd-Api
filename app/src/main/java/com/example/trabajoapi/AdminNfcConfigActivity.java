package com.example.trabajoapi;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trabajoapi.data.SessionManager;
import com.example.trabajoapi.data.repository.AdminRepository;
import com.example.trabajoapi.nfc.NfcFichajeController;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminNfcConfigActivity extends AppCompatActivity implements NfcFichajeController.Listener {

    private NfcFichajeController nfcController;
    private SessionManager sessionManager;
    private AdminRepository repository;

    private TextView tvEstado;
    private TextView tvCodigoLeido;
    private MaterialButton btnGuardar;
    private ImageView imgNfcIcon;

    private String codigoLeidoActual = null;

    // Deja lista la pantalla para leer una etiqueta y guardar su código como NFC principal.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_nfc_config);

        sessionManager = new SessionManager(this);
        repository = new AdminRepository();
        nfcController = new NfcFichajeController(this);

        tvEstado = findViewById(R.id.tvNfcConfigEstado);
        tvCodigoLeido = findViewById(R.id.tvNfcConfigCodigo);
        btnGuardar = findViewById(R.id.btnNfcConfigGuardar);
        imgNfcIcon = findViewById(R.id.imgNfcConfigIcon);

        // Espera a tener un código leído antes de permitir el guardado.
        btnGuardar.setEnabled(false);
        btnGuardar.setOnClickListener(v -> guardarNfc());

        // Cierra la pantalla sin aplicar cambios.
        findViewById(R.id.btnBackNfcConfig).setOnClickListener(v -> finish());
    }

    // Activa la lectura NFC cuando la pantalla vuelve al foco.
    @Override
    protected void onResume() {
        super.onResume();
        nfcController.onResume(this);
    }

    // Detiene la lectura NFC cuando la pantalla pierde el foco.
    @Override
    protected void onPause() {
        super.onPause();
        nfcController.onPause(this);
    }

    // --- NFC LISTENERS ---

    // Informa si el NFC está listo para leer o si falta activarlo en el sistema.
    @Override
    public void onNfcReady(boolean enabled) {
        if (enabled) {
            tvEstado.setText("Acerca la etiqueta que será el 'NFC PRINCIPAL'...");
        } else {
            tvEstado.setText("NFC Desactivado. Actívalo en ajustes.");
        }
    }

    // Al leer una etiqueta válida, guarda el código y habilita el botón de guardado.
    @Override
    public void onTagValida(String nfcId) {
        runOnUiThread(() -> {
            codigoLeidoActual = nfcId;
            tvCodigoLeido.setText("CÓDIGO: " + nfcId);
            tvEstado.setText("¡Etiqueta leída! Pulsa guardar.");
            btnGuardar.setEnabled(true);
            imgNfcIcon.setImageResource(R.drawable.ic_pop_check);
        });
    }

    // Si la lectura no cuadra con el formato esperado, muestra el motivo.
    @Override
    public void onTagInvalida(String motivo, String payload) {
        runOnUiThread(() -> {
            tvEstado.setText("Error leyendo etiqueta: " + motivo);
            imgNfcIcon.setImageResource(R.drawable.ic_pop_error);
        });
    }

    // Muestra un fallo general de NFC sin cambiar el flujo principal.
    @Override
    public void onNfcError(String motivo) {
        runOnUiThread(() -> {
            tvEstado.setText("Error NFC: " + motivo);
        });
    }

    // Envía el código leído al backend y deja el estado del botón coherente con el resultado.
    private void guardarNfc() {
        if (codigoLeidoActual == null) return;

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        String token = sessionManager.getAuthToken();
        repository.updateEmpresaNfc("Bearer " + token, codigoLeidoActual, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminNfcConfigActivity.this, "¡NFC Principal Configurado!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("GUARDAR CONFIGURACIÓN");
                    Toast.makeText(AdminNfcConfigActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnGuardar.setEnabled(true);
                btnGuardar.setText("GUARDAR CONFIGURACIÓN");
                Toast.makeText(AdminNfcConfigActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
