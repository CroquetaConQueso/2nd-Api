package com.example.trabajoapi.ui.admin.mapa;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.trabajoapi.data.repository.AdminRepository;

public class AdminMapaViewModelFactory implements ViewModelProvider.Factory {

    private final AdminRepository repo;

    // Inyecta el repositorio para construir el VM del mapa con sus dependencias.
    public AdminMapaViewModelFactory(AdminRepository repo) {
        this.repo = repo;
    }

    // Devuelve el VM correcto cuando la pantalla lo solicita.
    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AdminMapaViewModel.class)) {
            return (T) new AdminMapaViewModel(repo);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
