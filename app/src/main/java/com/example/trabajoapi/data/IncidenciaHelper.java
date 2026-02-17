package com.example.trabajoapi.data;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.trabajoapi.R;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class IncidenciaHelper {

    public interface NuevaIncidenciaListener {
        void onEnviar(String tipo, String inicio, String fin, String comentario);
    }

    private final Context context;

    public IncidenciaHelper(Context context) {
        this.context = context;
    }

    // Abre el formulario de solicitud y devuelve los datos validados a través del listener.
    public void mostrarDialogoNuevaIncidencia(NuevaIncidenciaListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("NUEVA INCIDENCIA");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final Spinner spinnerTipo = new Spinner(context);
        // Mantiene los valores en el formato esperado por el backend.
        String[] tipos = {"VACACIONES", "BAJA", "ASUNTOS PROPIOS", "HORAS EXTRA", "OLVIDO"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, tipos);
        spinnerTipo.setAdapter(adapter);
        layout.addView(spinnerTipo);

        final EditText etInicio = new EditText(context);
        etInicio.setHint("Fecha Inicio (Toca aquí)");
        etInicio.setFocusable(false);
        etInicio.setOnClickListener(v -> mostrarCalendario(etInicio));
        layout.addView(etInicio);

        final EditText etFin = new EditText(context);
        etFin.setHint("Fecha Fin (Toca aquí)");
        etFin.setFocusable(false);
        etFin.setOnClickListener(v -> mostrarCalendario(etFin));
        layout.addView(etFin);

        final EditText etComentario = new EditText(context);
        etComentario.setHint("Motivo (Opcional)");
        layout.addView(etComentario);

        builder.setView(layout);

        builder.setPositiveButton("ENVIAR", (dialog, which) -> {
            String tipo = spinnerTipo.getSelectedItem().toString();
            String inicio = etInicio.getText().toString().trim();
            String fin = etFin.getText().toString().trim();
            String comentario = etComentario.getText().toString().trim();

            if (inicio.isEmpty() || fin.isEmpty()) {
                mostrarToastPop("Fechas obligatorias", false);
                return;
            }

            if (listener != null) listener.onEnviar(tipo, inicio, fin, comentario);
        });

        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Lanza el selector de fecha y escribe el resultado en formato YYYY-MM-DD.
    private void mostrarCalendario(final EditText editText) {
        Calendar cal = Calendar.getInstance();
        int anio = cal.get(Calendar.YEAR);
        int mes = cal.get(Calendar.MONTH);
        int dia = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
            String fechaFormat = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            editText.setText(fechaFormat);
        }, anio, mes, dia);

        dpd.show();
    }

    // Muestra el historial en tarjetas, aplicando estilo según estado y comentario del admin.
    public void mostrarDialogoHistorial(List<IncidenciaResponse> lista) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("MIS SOLICITUDES");

        ScrollView scrollView = new ScrollView(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        if (lista == null || lista.isEmpty()) {
            TextView tv = new TextView(context);
            tv.setText("No tienes solicitudes recientes.");
            layout.addView(tv);
        } else {
            for (IncidenciaResponse inc : lista) {
                LinearLayout card = new LinearLayout(context);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackgroundColor(Color.WHITE);
                card.setPadding(30, 30, 30, 30);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 40);
                card.setLayoutParams(params);

                String estadoIcono = "⏳ PENDIENTE";
                int colorFondo = Color.parseColor("#FEF9C3");

                if ("APROBADA".equalsIgnoreCase(inc.getEstado())) {
                    estadoIcono = "✅ APROBADA";
                    colorFondo = Color.parseColor("#DCFCE7");
                } else if ("RECHAZADA".equalsIgnoreCase(inc.getEstado())) {
                    estadoIcono = "❌ RECHAZADA";
                    colorFondo = Color.parseColor("#FEE2E2");
                }

                TextView tvHeader = new TextView(context);
                tvHeader.setText(inc.getTipo() + "\n" + estadoIcono);
                tvHeader.setTextSize(16);
                tvHeader.setPadding(20, 20, 20, 20);
                tvHeader.setBackgroundColor(colorFondo);
                tvHeader.setTextColor(Color.BLACK);
                card.addView(tvHeader);

                TextView tvFechas = new TextView(context);
                tvFechas.setText("📅 " + inc.getFechaInicio() + " -> " + inc.getFechaFin());
                tvFechas.setTextColor(Color.DKGRAY);
                tvFechas.setPadding(10, 20, 10, 0);
                card.addView(tvFechas);

                if (inc.getComentarioAdmin() != null && !inc.getComentarioAdmin().isEmpty()) {
                    TextView tvAdmin = new TextView(context);
                    tvAdmin.setText("💬 Admin: " + inc.getComentarioAdmin());
                    tvAdmin.setPadding(10, 10, 10, 10);
                    tvAdmin.setTextColor(Color.BLACK);
                    card.addView(tvAdmin);
                }

                layout.addView(card);

                View line = new View(context);
                line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4));
                line.setBackgroundColor(Color.BLACK);
                layout.addView(line);

                View spacer = new View(context);
                spacer.setLayoutParams(new LinearLayout.LayoutParams(1, 40));
                layout.addView(spacer);
            }
        }

        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("CERRAR", null);
        builder.show();
    }

    // Muestra un toast con el estilo “pop” de la app y cae a toast normal si falla.
    public void mostrarToastPop(String mensaje, boolean esExito) {
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            View layout = inflater.inflate(R.layout.layout_toast_pop, null);

            TextView text = layout.findViewById(R.id.toastText);
            text.setText(mensaje);

            ImageView icon = layout.findViewById(R.id.toastIcon);
            icon.setImageResource(esExito ? R.drawable.ic_pop_success : R.drawable.ic_pop_error);

            Toast toast = new Toast(context);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            toast.show();
        } catch (Exception e) {
            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show();
        }
    }
}
