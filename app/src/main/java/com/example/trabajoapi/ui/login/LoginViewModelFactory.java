package com.example.trabajoapi.ui.login;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.trabajoapi.data.repository.AuthRepository;

public class LoginViewModelFactory implements ViewModelProvider.Factory {

    private final AuthRepository repo;

    // Inyecta el repositorio para construir el VM de login con sus dependencias.
    public LoginViewModelFactory(AuthRepository repo) {
        this.repo = repo;
    }

    // Devuelve el VM correcto en función de la clase solicitada.
    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(repo);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
