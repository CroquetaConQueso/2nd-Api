package com.example.trabajoapi.ui.admin.empleados;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.trabajoapi.data.repository.AdminRepository;

public class AdminEmpleadosViewModelFactory implements ViewModelProvider.Factory {

    private final AdminRepository repo;

    // Inyecta el repositorio para construir el VM de empleados con sus dependencias.
    public AdminEmpleadosViewModelFactory(AdminRepository repo) {
        this.repo = repo;
    }

    // Devuelve el VM correcto cuando la pantalla lo solicita.
    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AdminEmpleadosViewModel.class)) {
            return (T) new AdminEmpleadosViewModel(repo);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
