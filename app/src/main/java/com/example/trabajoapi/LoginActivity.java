package com.example.trabajoapi;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.lifecycle.ViewModelProvider;

import com.example.trabajoapi.data.LoginResponse;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;
import com.example.trabajoapi.data.repository.AuthRepository;
import com.example.trabajoapi.ui.login.LoginViewModel;
import com.example.trabajoapi.ui.login.LoginViewModelFactory;

public class LoginActivity extends AppCompatActivity {

    private EditText etNif, etPass;
    private AppCompatButton btnLogin;
    private TextView tvForgot;

    private SessionManager sessionManager;
    private LoginViewModel vm;

    // Prepara la pantalla de acceso, enlaza la UI y conecta el flujo de autenticación.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        // Vincula los campos del layout con las referencias de la Activity.
        etNif = findViewById(R.id.etNifLogin);
        etPass = findViewById(R.id.etPassLogin);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgot = findViewById(R.id.tvForgot);

        // Monta el VM con su repositorio para resolver las llamadas de login.
        AuthRepository repo = new AuthRepository(RetrofitClient.getInstance().getMyApi());
        vm = new ViewModelProvider(this, new LoginViewModelFactory(repo)).get(LoginViewModel.class);

        // Engancha el estado del VM para reflejar carga, mensajes y éxito de login.
        observarVM();

        // Lee campos, valida, y dispara el login bloqueando el botón durante la petición.
        btnLogin.setOnClickListener(v -> {
            String nif = etNif.getText() != null ? etNif.getText().toString().trim() : "";
            String pass = etPass.getText() != null ? etPass.getText().toString().trim() : "";

            if (nif.isEmpty() || pass.isEmpty()) {
                mostrarToastPop("Por favor, introduce usuario y contraseña", false);
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("...");
            vm.login(nif, pass);
        });

        // Abre el diálogo para solicitar el email y lanzar el reset.
        if (tvForgot != null) {
            tvForgot.setOnClickListener(v -> mostrarDialogoResetPassword());
        }
    }

    // Refresca UI según eventos del VM: carga, mensajes y acceso correcto.
    private void observarVM() {
        vm.getLoading().observe(this, isLoading -> {
            // Reactiva el botón cuando termina la petición y restaura el texto.
            boolean loading = isLoading != null && isLoading;
            btnLogin.setEnabled(!loading);
            if (!loading) btnLogin.setText("ENTRAR");
        });

        vm.getToastEvent().observe(this, e -> {
            if (e == null) return;
            String raw = e.getContentIfNotHandled();
            if (raw == null) return;

            boolean ok = raw.startsWith("OK:");
            String msg = raw;

            // Limpia el prefijo para mostrar un mensaje directo al usuario.
            if (raw.startsWith("OK:") || raw.startsWith("ERROR:")) {
                msg = raw.substring(raw.indexOf(":") + 1).trim();
            }

            mostrarToastPop(msg, ok);
        });

        vm.getLoginOkEvent().observe(this, e -> {
            if (e == null) return;
            LoginResponse r = e.getContentIfNotHandled();
            if (r == null) return;

            // Guarda sesión y salta al flujo principal limpiando el backstack.
            sessionManager.saveSession(r);

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // Si viene un aviso con el login, lo envía para que se muestre al entrar.
            if (r.getRecordatorio() != null && r.getRecordatorio().isAvisar()) {
                intent.putExtra("AVISO_TITULO", r.getRecordatorio().getTitulo());
                intent.putExtra("AVISO_MENSAJE", r.getRecordatorio().getMensaje());
            }

            startActivity(intent);
            finish();
        });
    }

    // Pide un email y lanza la petición de recuperación a través del VM.
    private void mostrarDialogoResetPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("RECUPERAR CONTRASEÑA");

        EditText input = new EditText(this);
        input.setHint("Email");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setPadding(60, 40, 60, 40);
        builder.setView(input);

        builder.setPositiveButton("ENVIAR", (dialog, which) -> {
            String email = input.getText() != null ? input.getText().toString() : "";
            vm.resetPassword(email);
        });

        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }

    // Muestra el toast personalizado y, si algo falla, cae al toast estándar.
    private void mostrarToastPop(String mensaje, boolean esExito) {
        try {
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.layout_toast_pop, null);

            TextView text = layout.findViewById(R.id.toastText);
            text.setText(mensaje);

            ImageView icon = layout.findViewById(R.id.toastIcon);
            icon.setImageResource(esExito ? R.drawable.ic_pop_success : R.drawable.ic_pop_error);

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();
        } catch (Exception e) {
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
        }
    }
}
