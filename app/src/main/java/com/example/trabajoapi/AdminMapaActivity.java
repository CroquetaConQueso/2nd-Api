package com.example.trabajoapi;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.trabajoapi.data.SessionManager;
import com.example.trabajoapi.data.repository.AdminRepository;
import com.example.trabajoapi.ui.admin.mapa.AdminMapaViewModel;
import com.example.trabajoapi.ui.admin.mapa.AdminMapaViewModelFactory;
import com.google.android.material.slider.Slider;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.List;

public class AdminMapaActivity extends AppCompatActivity {

    private MapView map;
    private SessionManager sessionManager;
    private AdminMapaViewModel vm;

    private GeoPoint ubicacionActual;
    private int radioActual = 50;

    private TextView tvRadioValor;
    private Slider sliderRadio;

    private Marker markerEmpresa;
    private Polygon circuloRadio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Configuración OSMDroid
            Configuration.getInstance().load(
                    getApplicationContext(),
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
            );
            Configuration.getInstance().setUserAgentValue(getPackageName());

            setContentView(R.layout.activity_admin_mapa);

            sessionManager = new SessionManager(this);

            map = findViewById(R.id.map);
            if (map == null) {
                mostrarErrorCritico("Error: No se encuentra el ID 'map' en el XML");
                return;
            }

            sliderRadio = findViewById(R.id.sliderRadio);
            tvRadioValor = findViewById(R.id.tvRadioValor);

            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            map.getController().setZoom(18.0);

            // Tap en mapa para mover la ubicación de empresa
            MapEventsReceiver mReceive = new MapEventsReceiver() {
                @Override public boolean singleTapConfirmedHelper(GeoPoint p) { moverEmpresa(p); return true; }
                @Override public boolean longPressHelper(GeoPoint p) { moverEmpresa(p); return true; }
            };
            map.getOverlays().add(new MapEventsOverlay(mReceive));

            // Slider para ajustar radio
            if (sliderRadio != null) {
                sliderRadio.setValueFrom(10f);
                sliderRadio.setValueTo(500f);
                sliderRadio.setValue(radioActual);
                sliderRadio.addOnChangeListener((slider, value, fromUser) -> {
                    radioActual = (int) value;
                    actualizarVisuales();
                });
            }

            vm = new ViewModelProvider(
                    this,
                    new AdminMapaViewModelFactory(new AdminRepository())
            ).get(AdminMapaViewModel.class);

            // Guardar configuración en backend
            View btnGuardar = findViewById(R.id.btnGuardarMapa);
            if (btnGuardar != null) {
                btnGuardar.setOnClickListener(v -> {
                    if (ubicacionActual == null) {
                        mostrarToastPop("Selecciona una ubicación primero", false);
                        return;
                    }
                    String token = sessionManager.getAuthToken();
                    if (token == null) { irALogin(); return; }

                    vm.guardarConfiguracion("Bearer " + token,
                            ubicacionActual.getLatitude(),
                            ubicacionActual.getLongitude(),
                            radioActual
                    );
                });
            }

            // Volver sin guardar
            View btnVolver = findViewById(R.id.btnVolverMapa);
            if (btnVolver != null) btnVolver.setOnClickListener(v -> finish());

            observarVM();

            String token = sessionManager.getAuthToken();
            if (token == null) { irALogin(); return; }
            vm.cargarConfiguracion("Bearer " + token);

        } catch (Exception e) {
            Log.e("AdminMapa", "Error fatal en onCreate", e);
            mostrarErrorCritico("Error iniciando mapa: " + e.getMessage());
            finish();
        }
    }

    private void observarVM() {
        // Recibe configuración desde backend y pinta estado inicial
        vm.getConfig().observe(this, cfg -> {
            if (cfg == null) return;

            try {
                if (cfg.getLatitud() != null && cfg.getLongitud() != null) {
                    ubicacionActual = new GeoPoint(cfg.getLatitud(), cfg.getLongitud());

                    if (cfg.getRadio() != null && cfg.getRadio() > 0) {
                        radioActual = cfg.getRadio();
                    }

                    if (sliderRadio != null) sliderRadio.setValue(radioActual);
                    if (map != null) map.getController().setCenter(ubicacionActual);
                    actualizarVisuales();
                } else {
                    mostrarToastPop("Sin ubicación previa. Toca el mapa.", false);
                    if (map != null) map.getController().setCenter(new GeoPoint(40.416775, -3.703790));
                }
            } catch (Exception e) {
                Log.e("AdminMapa", "Error procesando config", e);
            }
        });

        // Mensajes de feedback del ViewModel
        vm.getToastEvent().observe(this, e -> {
            if (e == null) return;
            String msg = e.getContentIfNotHandled();
            if (msg != null) mostrarToastPop(msg, !msg.toUpperCase().contains("ERROR"));
        });

        // Sesión inválida / token caducado
        vm.getGoLoginEvent().observe(this, e -> {
            if (e == null) return;
            if (Boolean.TRUE.equals(e.getContentIfNotHandled())) irALogin();
        });

        // Guardado correcto -> cerrar pantalla
        vm.getSavedEvent().observe(this, e -> {
            if (e == null) return;
            if (Boolean.TRUE.equals(e.getContentIfNotHandled())) finish();
        });
    }

    private void moverEmpresa(GeoPoint nuevoPunto) {
        ubicacionActual = nuevoPunto;
        actualizarVisuales();
    }

    private void actualizarVisuales() {
        if (map == null || ubicacionActual == null) return;

        try {
            if (markerEmpresa != null) map.getOverlays().remove(markerEmpresa);
            if (circuloRadio != null) map.getOverlays().remove(circuloRadio);

            markerEmpresa = new Marker(map);
            markerEmpresa.setPosition(ubicacionActual);
            markerEmpresa.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            markerEmpresa.setTitle("Sede Empresa");
            map.getOverlays().add(markerEmpresa);

            circuloRadio = new Polygon(map);
            List<GeoPoint> puntos = Polygon.pointsAsCircle(ubicacionActual, radioActual);
            circuloRadio.setPoints(puntos);
            circuloRadio.getOutlinePaint().setColor(Color.BLACK);
            circuloRadio.getOutlinePaint().setStrokeWidth(5f);
            circuloRadio.getFillPaint().setColor(Color.argb(70, 233, 30, 99));

            if (!map.getOverlays().isEmpty()) {
                map.getOverlays().add(0, circuloRadio);
            } else {
                map.getOverlays().add(circuloRadio);
            }

            if (tvRadioValor != null) tvRadioValor.setText(radioActual + " m");

            map.invalidate();

        } catch (Exception e) {
            Log.e("AdminMapa", "Error dibujando mapa", e);
        }
    }

    private void mostrarToastPop(String mensaje, boolean esExito) {
        try {
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.layout_toast_pop, null);
            TextView text = layout.findViewById(R.id.toastText);
            text.setText(mensaje);
            ImageView icon = layout.findViewById(R.id.toastIcon);
            icon.setImageResource(esExito ? R.drawable.ic_pop_success : R.drawable.ic_pop_error);

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();
        } catch (Exception e) {
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarErrorCritico(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    private void irALogin() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}
