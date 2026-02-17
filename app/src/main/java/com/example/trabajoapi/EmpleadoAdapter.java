package com.example.trabajoapi;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trabajoapi.data.TrabajadorResponse;
import java.util.List;

public class EmpleadoAdapter extends RecyclerView.Adapter<EmpleadoAdapter.ViewHolder> {

    private List<TrabajadorResponse> lista;
    private OnItemClickListener listener;

    // Permite reaccionar al click de un empleado desde la pantalla que use el adapter.
    public interface OnItemClickListener {
        void onItemClick(TrabajadorResponse empleado);
    }

    // Recibe la lista a pintar y el callback para el click.
    public EmpleadoAdapter(List<TrabajadorResponse> lista, OnItemClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla el item de empleado que se repetirá en la lista.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_empleado, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrabajadorResponse item = lista.get(position);

        holder.tvNombre.setText(item.getNombreCompleto());
        holder.tvNif.setText(item.getNif());

        String rol = item.getRol();
        holder.tvRol.setText(rol.toUpperCase());

        // Marca el rol con un fondo distinto para diferenciar administradores del resto.
        if (rol.equalsIgnoreCase("Administrador") || rol.contains("Admin") || rol.contains("admin")) {
            holder.tvRol.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
        } else {
            holder.tvRol.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00BCD4")));
        }

        // Propaga el click del item hacia fuera sin acoplar la lógica aquí.
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvNif, tvRol;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Mantiene referencias para reutilizar vistas al hacer scroll.
            tvNombre = itemView.findViewById(R.id.tvNombreEmpleado);
            tvNif = itemView.findViewById(R.id.tvNifEmpleado);
            tvRol = itemView.findViewById(R.id.tvRolEmpleado);
        }
    }
}
