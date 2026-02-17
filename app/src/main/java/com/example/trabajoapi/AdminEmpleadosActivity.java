package com.example.trabajoapi;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.SessionManager;
import com.example.trabajoapi.data.TrabajadorResponse;
import com.example.trabajoapi.data.repository.AdminRepository;
import com.example.trabajoapi.ui.admin.empleados.AdminEmpleadosViewModel;
import com.example.trabajoapi.ui.admin.empleados.AdminEmpleadosViewModelFactory;

import java.util.List;

public class AdminEmpleadosActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SessionManager sessionManager;

    private AdminEmpleadosViewModel vm;

    // Muestra la lista de empleados y permite entrar a su historial.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_empleados);

        sessionManager = new SessionManager(this);

        recyclerView = findViewById(R.id.recyclerEmpleados);
        progressBar = findViewById(R.id.progressEmpleados);

        // Vuelve a la pantalla anterior sin tocar estado.
        ImageView btnVolver = findViewById(R.id.btnVolverLista);
        if (btnVolver != null) btnVolver.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Monta el VM que se encarga de pedir empleados y emitir eventos.
        vm = new ViewModelProvider(
                this,
                new AdminEmpleadosViewModelFactory(new AdminRepository())
        ).get(AdminEmpleadosViewModel.class);

        observarVM();

        String token = sessionManager.getAuthToken();
        if (token == null) {
            irALogin();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        vm.cargarEmpleados("Bearer " + token);
    }

    // Vincula carga, lista y mensajes del VM con la UI.
    private void observarVM() {
        vm.getLoading().observe(this, isLoading -> {
            if (isLoading == null) return;
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        vm.getEmpleados().observe(this, empleados -> {
            if (empleados == null) return;
            pintarLista(empleados);
        });

        vm.getToastEvent().observe(this, e -> {
            if (e == null) return;
            String msg = e.getContentIfNotHandled();
            if (msg != null) mostrarToastPop(msg, !msg.toUpperCase().contains("ERROR"));
        });

        vm.getGoLoginEvent().observe(this, e -> {
            if (e == null) return;
            Boolean go = e.getContentIfNotHandled();
            if (go != null && go) irALogin();
        });

        // Se mantiene para compatibilidad con el flujo anterior sin romper el VM.
        vm.getFichajesEmpleadoEvent().observe(this, e -> {
            if (e == null) return;
            AdminEmpleadosViewModel.EmpleadoFichajesUI ui = e.getContentIfNotHandled();
            if (ui == null) return;
            mostrarDialogoFichajesEmpleado(ui.getEmpleadoNombre(), ui.getFichajes());
        });
    }

    // Pinta la lista y delega el click para abrir el historial del empleado.
    private void pintarLista(List<TrabajadorResponse> empleados) {
        EmpleadoAdapter adapter = new EmpleadoAdapter(empleados, empleado -> {

            Intent intent = new Intent(AdminEmpleadosActivity.this, AdminVerFichajesActivity.class);

            intent.putExtra("ID_EMPLEADO", empleado.getIdTrabajador());
            intent.putExtra("NOMBRE_EMPLEADO", empleado.getNombreCompleto());

            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    // Mantiene el diálogo por si el VM aún emite este flujo en alguna ruta.
    private void mostrarDialogoFichajesEmpleado(String nombreEmpleado, List<FichajeResponse> lista) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("FICHAJES — " + (nombreEmpleado != null ? nombreEmpleado : "Empleado"));

        if (lista == null || lista.isEmpty()) {
            builder.setMessage("Este empleado no tiene fichajes.");
        } else {
            String[] items = new String[lista.size()];
            for (int i = 0; i < lista.size(); i++) {
                FichajeResponse f = lista.get(i);

                String rawFecha = f.getFechaHora();
                String fechaLimpia = "Sin fecha";
                if (rawFecha != null) {
                    fechaLimpia = rawFecha.replace("T", " ");
                    if (fechaLimpia.length() > 16) fechaLimpia = fechaLimpia.substring(0, 16);
                }

                String tipo = (f.getTipo() != null ? f.getTipo() : "REGISTRO");
                items[i] = tipo + "\n" + fechaLimpia;
            }
            builder.setItems(items, null);
        }

        builder.setPositiveButton("CERRAR", null);
        builder.show();
    }

    // Muestra el toast personalizado y, si falla el layout, cae al toast estándar.
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

    // Cierra sesión y devuelve al login cuando el token ya no es válido.
    private void irALogin() {
        sessionManager.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
