package com.example.trabajoapi;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trabajoapi.data.SessionManager;
import com.example.trabajoapi.ui.main.HistorialViewModel;

public class HistorialActivity extends AppCompatActivity {

    private HistorialViewModel viewModel;
    private SessionManager sessionManager;
    private FichajeAdapter adapter;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;

    // Monta la lista de fichajes, conecta el adapter y dispara la carga inicial.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_propio);

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(HistorialViewModel.class);

        // Enlaza la UI y deja listo el botón de vuelta.
        recyclerView = findViewById(R.id.recyclerMisFichajes);
        progressBar = findViewById(R.id.progressHistorial);
        findViewById(R.id.btnVolverHistorial).setOnClickListener(v -> finish());

        // Configura RecyclerView con su layout y adapter.
        adapter = new FichajeAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Refleja en pantalla carga, lista y mensajes del VM.
        observarViewModel();

        // Lanza la petición con el token actual.
        viewModel.cargarMisFichajes("Bearer " + sessionManager.getAuthToken());
    }

    // Escucha cambios del VM para actualizar progreso, lista y mensajes.
    private void observarViewModel() {
        viewModel.getLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getFichajes().observe(this, lista -> {
            if (lista != null) adapter.setLista(lista);
        });

        viewModel.getToastEvent().observe(this, event -> {
            String msg = event.getContentIfNotHandled();
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }
}
