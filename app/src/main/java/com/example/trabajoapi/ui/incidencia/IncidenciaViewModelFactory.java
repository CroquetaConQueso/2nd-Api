package com.example.trabajoapi.ui.incidencia;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.trabajoapi.data.repository.IncidenciaRepository;

public class IncidenciaViewModelFactory implements ViewModelProvider.Factory {

    private final IncidenciaRepository repo;

    // Inyecta el repositorio para construir el VM de incidencias con sus dependencias.
    public IncidenciaViewModelFactory(IncidenciaRepository repo) {
        this.repo = repo;
    }

    // Devuelve el VM correcto cuando se solicita desde la UI.
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(IncidenciaViewModel.class)) {
            return (T) new IncidenciaViewModel(repo);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
