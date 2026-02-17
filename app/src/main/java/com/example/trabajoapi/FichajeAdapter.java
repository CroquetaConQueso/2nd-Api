package com.example.trabajoapi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trabajoapi.data.FichajeResponse;
import java.util.ArrayList;
import java.util.List;

public class FichajeAdapter extends RecyclerView.Adapter<FichajeAdapter.ViewHolder> {

    private List<FichajeResponse> lista = new ArrayList<>();

    // Sustituye la lista actual y refresca el RecyclerView.
    public void setLista(List<FichajeResponse> nuevaLista) {
        this.lista = nuevaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla el layout del item propio para renderizar cada fichaje.
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fichaje, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FichajeResponse item = lista.get(position);

        String fecha = item.getFechaHora() != null ? item.getFechaHora().replace("T", " ") : "---";
        if (fecha.length() > 16) fecha = fecha.substring(0, 16);

        String tipo = item.getTipo() != null ? item.getTipo().toUpperCase() : "REGISTRO";

        holder.tvTipo.setText(tipo);
        holder.tvFecha.setText(fecha);

        // Muestra coordenadas de forma compacta para que no ensucie el item.
        holder.tvUbi.setText(String.format("Lat: %.4f, Lon: %.4f", item.getLatitud(), item.getLongitud()));

        // Ajusta color e icono para distinguir entrada/salida de un vistazo.
        if ("ENTRADA".equals(tipo)) {
            holder.tvTipo.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.pop_green));
            holder.imgIcon.setImageResource(R.drawable.ic_fichaje_entrada);
        } else {
            holder.tvTipo.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.pop_pink));
            holder.imgIcon.setImageResource(R.drawable.ic_fichaje_salida);
        }
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTipo, tvFecha, tvUbi;
        ImageView imgIcon;

        ViewHolder(View v) {
            super(v);
            // Enlaza las vistas del item para reutilizarlas sin buscar cada vez.
            tvTipo = v.findViewById(R.id.tvTipoFichaje);
            tvFecha = v.findViewById(R.id.tvFechaFichaje);
            tvUbi = v.findViewById(R.id.tvUbicacionFichaje);
            imgIcon = v.findViewById(R.id.imgTipoFichaje);
        }
    }
}
