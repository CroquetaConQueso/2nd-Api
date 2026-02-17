package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class TrabajadorResponse implements Serializable {

    // Claves alineadas con el esquema del backend.
    @SerializedName("id_trabajador")
    private int idTrabajador;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("apellidos")
    private String apellidos;

    @SerializedName("nif")
    private String nif;

    @SerializedName("email")
    private String email;

    @SerializedName("rol_nombre")
    private String rol;

    public TrabajadorResponse() { }

    public int getIdTrabajador() { return idTrabajador; }

    // Devuelve un nombre seguro para evitar nulos en pantalla.
    public String getNombre() { return nombre != null ? nombre : "Sin Nombre"; }

    // Devuelve apellidos o vacío para componer el nombre completo sin romper UI.
    public String getApellidos() { return apellidos != null ? apellidos : ""; }

    // Devuelve un NIF seguro cuando no llega informado.
    public String getNif() { return nif != null ? nif : "---"; }

    public String getEmail() { return email; }

    // Devuelve un rol por defecto para mantener lógica de UI consistente.
    public String getRol() { return rol != null ? rol : "Trabajador"; }

    // Construye un texto listo para mostrar en listados.
    public String getNombreCompleto() { return getNombre() + " " + getApellidos(); }
}
