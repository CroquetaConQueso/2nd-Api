package com.example.trabajoapi.ui.main;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.trabajoapi.data.repository.MainRepository;

public class MainViewModelFactory implements ViewModelProvider.Factory {

    private final MainRepository repo;

    // Inyecta el repositorio para construir el VM principal con sus dependencias.
    public MainViewModelFactory(MainRepository repo) {
        this.repo = repo;
    }

    // Devuelve la instancia correcta del VM según la clase solicitada.
    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(repo);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
