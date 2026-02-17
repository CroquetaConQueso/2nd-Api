package com.example.trabajoapi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.trabajoapi.data.SessionManager;

public class AdminActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    // Centraliza accesos rápidos a herramientas de administración.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        sessionManager = new SessionManager(this);

        // Vuelve a la pantalla anterior sin modificar el estado.
        ImageView btnVolver = findViewById(R.id.btnVolverAdmin);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> finish());
        }

        // Abre la lista de empleados para consultar sus historiales.
        View btnEmpleados = findViewById(R.id.cardEmpleados);
        if (btnEmpleados != null) {
            btnEmpleados.setOnClickListener(v ->
                    startActivity(new Intent(AdminActivity.this, AdminEmpleadosActivity.class))
            );
        }

        // Abre el mapa para configurar ubicación y radio de fichaje.
        View btnMapa = findViewById(R.id.cardMapa);
        if (btnMapa != null) {
            btnMapa.setOnClickListener(v ->
                    startActivity(new Intent(AdminActivity.this, AdminMapaActivity.class))
            );
        }

        // Abre la pantalla para fijar la etiqueta NFC principal de la empresa.
        View btnConfigNfc = findViewById(R.id.btnConfigNfc);
        if (btnConfigNfc != null) {
            btnConfigNfc.setOnClickListener(v ->
                    startActivity(new Intent(AdminActivity.this, AdminNfcConfigActivity.class))
            );
        }
    }
}
