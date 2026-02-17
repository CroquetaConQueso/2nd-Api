package com.example.trabajoapi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.trabajoapi.data.FichajeResponse;
import com.example.trabajoapi.data.IncidenciaHelper;
import com.example.trabajoapi.data.IncidenciaResponse;
import com.example.trabajoapi.data.RecordatorioResponse;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;
import com.example.trabajoapi.data.repository.IncidenciaRepository;
import com.example.trabajoapi.data.repository.MainRepository;
import com.example.trabajoapi.nfc.NfcFichajeController;
import com.example.trabajoapi.ui.incidencia.IncidenciaViewModel;
import com.example.trabajoapi.ui.incidencia.IncidenciaViewModelFactory;
import com.example.trabajoapi.ui.main.MainViewModel;
import com.example.trabajoapi.ui.main.MainViewModelFactory;
import com.example.trabajoapi.work.TrabajadorRecordatorio;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements NfcFichajeController.Listener {

    private static final int PERMISSION_REQUEST_CODE = 112;
    private static final int PERMISSION_ID = 44;
    private static final String WORK_UNIQUE_NAME = "recordatorio_fichaje_bg";

    private SessionManager sessionManager;
    private FusedLocationProviderClient fusedLocationClient;

    private MaterialButton btnFicharMain;
    private TextView tvHorasExtraValor;
    private TextView tvEstadoHoras;

    private ConstraintLayout cardHorasExtra;
    private TextView tvBadgeRevision;
    private TextView tvInfoRevision;

    private String pendingNfcCode = null;

    private String avisoTituloPendiente = null;
    private String avisoMensajePendiente = null;

    private MainViewModel vm;

    private IncidenciaHelper incidenciaHelper;
    private IncidenciaViewModel ivm;

    private NfcFichajeController nfcController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        nfcController = new NfcFichajeController(this);

        vm = new ViewModelProvider(
                this,
                new MainViewModelFactory(new MainRepository())
        ).get(MainViewModel.class);

        ivm = new ViewModelProvider(
                this,
                new IncidenciaViewModelFactory(new IncidenciaRepository())
        ).get(IncidenciaViewModel.class);

        incidenciaHelper = new IncidenciaHelper(this);

        btnFicharMain = findViewById(R.id.btnFicharMain);
        tvHorasExtraValor = findViewById(R.id.tvHorasExtraValor);
        tvEstadoHoras = findViewById(R.id.tvEstadoHoras);

        cardHorasExtra = findViewById(R.id.cardHorasExtra);
        tvBadgeRevision = findViewById(R.id.tvBadgeRevision);
        tvInfoRevision = findViewById(R.id.tvInfoRevision);

        tvHorasExtraValor.setText("...");
        tvEstadoHoras.setText("CALCULANDO...");
        if (tvBadgeRevision != null) tvBadgeRevision.setVisibility(View.GONE);
        if (tvInfoRevision != null) tvInfoRevision.setVisibility(View.GONE);

        ImageView btnLogout = findViewById(R.id.btnLogoutIcon);
        AppCompatButton btnIncidencia = findViewById(R.id.btnIncidencia);
        AppCompatButton btnHistorial = findViewById(R.id.btnHistorial);
        AppCompatButton btnMisFichajes = findViewById(R.id.btnMisFichajes);
        AppCompatButton btnCambiarClave = findViewById(R.id.btnCambiarClave);
        AppCompatButton btnAdminPanel = findViewById(R.id.btnAdminPanel);

        btnFicharMain.setOnClickListener(v -> {
            btnFicharMain.setEnabled(false);
            btnFicharMain.setText("...");
            checkPermissionsAndFichar(null);
        });

        if (btnIncidencia != null) {
            btnIncidencia.setOnClickListener(v -> {
                incidenciaHelper.mostrarDialogoNuevaIncidencia((tipo, inicio, fin, comentario) -> {
                    String token = sessionManager.getAuthToken();
                    if (token == null) { irALogin(); return; }
                    ivm.crearIncidencia("Bearer " + token, tipo, inicio, fin, comentario);
                });
            });
        }

        if (btnHistorial != null) {
            btnHistorial.setOnClickListener(v -> {
                String token = sessionManager.getAuthToken();
                if (token == null) { irALogin(); return; }
                ivm.cargarHistorial("Bearer " + token);
            });
        }

        if (btnMisFichajes != null) {
            btnMisFichajes.setOnClickListener(v -> {
                String token = sessionManager.getAuthToken();
                if (token == null) { irALogin(); return; }
                vm.pedirHistorialParaDialogo("Bearer " + token);
            });
        }

        if (btnCambiarClave != null) {
            btnCambiarClave.setOnClickListener(v -> mostrarDialogoCambioPassword());
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                cancelRecordatorioWorker();
                sessionManager.clearSession();
                irALogin();
            });
        }

        if (btnAdminPanel != null) {
            if (sessionManager.isAdmin()) {
                btnAdminPanel.setVisibility(View.VISIBLE);
                btnAdminPanel.setOnClickListener(v ->
                        startActivity(new Intent(MainActivity.this, AdminActivity.class))
                );
            } else {
                btnAdminPanel.setVisibility(View.GONE);
            }
        }

        prepararAvisoLoginSiExiste();
        pedirPermisosNotificaciones();
        intentarMostrarAvisoPendiente();

        if (sessionManager.getAuthToken() != null) {
            scheduleRecordatorioWorker();
        }

        observarVM();
        observarIncidenciasVM();

        enviarTokenFCM();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String token = sessionManager.getAuthToken();
        if (token == null) {
            irALogin();
            return;
        }

        if (nfcController != null) nfcController.onResume(this);

        scheduleRecordatorioWorker();
        String bearer = "Bearer " + token;
        vm.cargarDashboard(bearer);
        vm.comprobarRecordatorio(bearer);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcController != null) nfcController.onPause(this);
    }

    @Override
    public void onNfcReady(boolean enabled) {}

    @Override
    public void onTagValida(String nfcId) {
        runOnUiThread(() -> {
            mostrarToastPop("Tarjeta detectada. Validando ubicación...", true);
            checkPermissionsAndFichar(nfcId);
        });
    }

    @Override
    public void onTagInvalida(String motivo, String payloadLeido) {
        runOnUiThread(() -> mostrarToastPop("Error NFC: " + motivo, false));
    }

    @Override
    public void onNfcError(String motivo) {
        runOnUiThread(() -> mostrarToastPop("Error Lectura: " + motivo, false));
    }

    private void observarVM() {

        vm.getDentro().observe(this, this::actualizarBotonFichaje);

        vm.getResumen().observe(this, r -> {

            tvHorasExtraValor.setText("...");
            tvHorasExtraValor.setTextColor(ContextCompat.getColor(this, R.color.black));
            tvEstadoHoras.setText("CALCULANDO...");

            if (tvBadgeRevision != null) tvBadgeRevision.setVisibility(View.GONE);
            if (tvInfoRevision != null) tvInfoRevision.setVisibility(View.GONE);

            if (r == null) {
                tvHorasExtraValor.setText("+0.00 h");
                tvHorasExtraValor.setTextColor(ContextCompat.getColor(this, R.color.black));
                tvEstadoHoras.setText("SIN DATOS DEL MES");
                return;
            }

            if (!r.isCalculoConfiable()) {
                tvHorasExtraValor.setText("+0.00 h");
                tvHorasExtraValor.setTextColor(ContextCompat.getColor(this, R.color.pop_yellow));
                tvEstadoHoras.setText("PENDIENTE DE REVISIÓN");

                if (tvBadgeRevision != null) tvBadgeRevision.setVisibility(View.VISIBLE);
                if (tvInfoRevision != null) tvInfoRevision.setVisibility(View.VISIBLE);
                return;
            }

            double saldo = r.getSaldo();
            double extra = Math.max(0.0, saldo);

            tvHorasExtraValor.setText(String.format("+%.2f h", extra));

            if (extra > 0.0) {
                tvHorasExtraValor.setTextColor(ContextCompat.getColor(this, R.color.pop_green));
                tvEstadoHoras.setText("HORAS EXTRA ACUMULADAS");
            } else {
                tvHorasExtraValor.setTextColor(ContextCompat.getColor(this, R.color.black));
                if (saldo < 0) {
                    tvEstadoHoras.setText("SIN HORAS EXTRA (MES EN NEGATIVO)");
                } else {
                    tvEstadoHoras.setText("AÚN NO HAY HORAS EXTRA ESTE MES");
                }
            }
        });

        if (cardHorasExtra != null) {
            cardHorasExtra.setOnClickListener(v -> {
                com.example.trabajoapi.data.ResumenResponse res = vm.getResumen().getValue();
                if (res != null) mostrarDetalleResumen(res);
            });
        }

        vm.getToastEvent().observe(this, e -> {
            if (e == null) return;
            String msg = e.getContentIfNotHandled();
            if (msg == null) return;

            String up = msg.toUpperCase();

            boolean ok =
                    up.contains("EXITOSA")
                            || up.contains("OK")
                            || up.contains("CORRECT")
                            || up.contains("BIENVEN")
                            || up.contains("INICIAD")
                            || up.contains("INICIO")
                            || up.contains("GUARDAD")
                            || up.contains("ENVIAD")
                            || up.contains("CREAD")
                            || msg.contains("Clave cambiada");

            mostrarToastPop(msg, ok);
        });

        vm.getRecordatorioEvent().observe(this, e -> {
            if (e == null) return;
            RecordatorioResponse r = e.getContentIfNotHandled();
            if (r == null) return;

            mostrarDialogoRecordatorio(r);

            if (tienePermisoNotificaciones()) {
                mostrarNotificacionLocal(r.getTitulo(), r.getMensaje());
            } else {
                avisoTituloPendiente = r.getTitulo();
                avisoMensajePendiente = r.getMensaje();
                pedirPermisosNotificaciones();
            }
        });

        vm.getHistorialDialogEvent().observe(this, e -> {
            if (e == null) return;
            List<FichajeResponse> lista = e.getContentIfNotHandled();
            if (lista == null) return;
            mostrarDialogoHistorialFichajes(lista);
        });

        vm.getLogoutEvent().observe(this, e -> {
            if (e == null) return;
            Boolean must = e.getContentIfNotHandled();
            if (must != null && must) irALogin();
        });
    }

    private void mostrarDetalleResumen(com.example.trabajoapi.data.ResumenResponse r) {
        if (r == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("Mes: ").append(r.getMes()).append("\n\n");
        sb.append("Teóricas: ").append(String.format("%.2f", r.getTeoricas())).append(" h\n");
        sb.append("Trabajadas: ").append(String.format("%.2f", r.getTrabajadas())).append(" h\n");
        sb.append("Diferencia: ").append(String.format("%.2f", r.getSaldo())).append(" h\n\n");

        if (!r.isCalculoConfiable()) {
            sb.append("Cálculo pendiente de revisión.\n");
            List<String> dias = r.getDiasIncompletos();
            if (dias != null && !dias.isEmpty()) {
                sb.append("Días incompletos: ").append(joinDias(dias)).append("\n");
            } else {
                sb.append("Motivo: faltan fichajes o pares ENTRADA/SALIDA.\n");
            }
        } else {
            if (r.getSaldo() >= 0) {
                sb.append("Horas extra (mes): ").append(String.format("%.2f", r.getSaldo())).append(" h\n");
            } else {
                sb.append("Horas extra (mes): 0.00 h\n");
                sb.append("Horas pendientes (mes): ").append(String.format("%.2f", Math.abs(r.getSaldo()))).append(" h\n");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("RESUMEN MENSUAL")
                .setMessage(sb.toString())
                .setPositiveButton("CERRAR", null)
                .show();
    }

    private String joinDias(List<String> dias) {
        if (dias == null || dias.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dias.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(dias.get(i));
        }
        return sb.toString();
    }

    private void observarIncidenciasVM() {
        ivm.getToastEvent().observe(this, e -> {
            if (e == null) return;
            String msg = e.getContentIfNotHandled();
            if (msg != null) {
                boolean ok = msg.toUpperCase().contains("SOLICITUD") || msg.toUpperCase().contains("ENVIAD");
                incidenciaHelper.mostrarToastPop(msg, ok);
            }
        });

        ivm.getHistorialEvent().observe(this, e -> {
            if (e == null) return;
            List<IncidenciaResponse> lista = e.getContentIfNotHandled();
            if (lista != null) incidenciaHelper.mostrarDialogoHistorial(lista);
        });

        ivm.getLogoutEvent().observe(this, e -> {
            if (e == null) return;
            Boolean must = e.getContentIfNotHandled();
            if (must != null && must) irALogin();
        });
    }

    private void mostrarDialogoHistorialFichajes(List<FichajeResponse> lista) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("MIS ÚLTIMOS FICHAJES");

        if (lista == null || lista.isEmpty()) {
            builder.setMessage("No tienes registros de fichaje aún.");
        } else {
            String[] items = new String[lista.size()];
            for (int i = 0; i < lista.size(); i++) {
                FichajeResponse f = lista.get(i);
                String rawFecha = f.getFechaHora();
                String fechaLimpia = "Sin fecha";
                if (rawFecha != null) {
                    fechaLimpia = rawFecha.replace("T", " ");
                    if (fechaLimpia.length() > 16) fechaLimpia = fechaLimpia.substring(0, 16);
                }
                items[i] = (f.getTipo() != null ? f.getTipo() : "REGISTRO") + "\n" + fechaLimpia;
            }
            builder.setItems(items, null);
        }
        builder.setPositiveButton("CERRAR", null);
        builder.show();
    }

    private void actualizarBotonFichaje(boolean estoyDentro) {
        btnFicharMain.setEnabled(true);
        if (estoyDentro) {
            btnFicharMain.setText("FICHAR\nSALIDA");
            btnFicharMain.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pop_pink)));
            btnFicharMain.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            btnFicharMain.setText("FICHAR\nENTRADA");
            btnFicharMain.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pop_green)));
            btnFicharMain.setTextColor(ContextCompat.getColor(this, R.color.black));
        }
    }

    private void checkPermissionsAndFichar(String nfcCode) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionYFichar(nfcCode);
        } else {
            pendingNfcCode = nfcCode;

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_ID
            );

            if (nfcCode == null) btnFicharMain.setEnabled(true);
        }
    }

    private void obtenerUbicacionYFichar(String nfcCode) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    String token = sessionManager.getAuthToken();
                    if (token == null) { irALogin(); return; }

                    if (location != null) {
                        if (nfcCode != null) {
                            vm.realizarFichajeNfc("Bearer " + token, location.getLatitude(), location.getLongitude(), nfcCode);
                        } else {
                            vm.fichar("Bearer " + token, location.getLatitude(), location.getLongitude(), null);
                        }
                    } else {
                        mostrarToastPop("Activa el GPS", false);
                        vm.consultarEstadoFichaje("Bearer " + token);
                        if (nfcCode == null) btnFicharMain.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    mostrarToastPop("Error GPS", false);
                    String token = sessionManager.getAuthToken();
                    if (token != null) vm.consultarEstadoFichaje("Bearer " + token);
                    if (nfcCode == null) btnFicharMain.setEnabled(true);
                });
    }

    private boolean tienePermisoNotificaciones() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void pedirPermisosNotificaciones() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (!tienePermisoNotificaciones()) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    private void prepararAvisoLoginSiExiste() {
        Intent i = getIntent();
        if (i == null) return;
        String titulo = i.getStringExtra("AVISO_TITULO");
        String mensaje = i.getStringExtra("AVISO_MENSAJE");
        if (titulo != null && mensaje != null) {
            avisoTituloPendiente = titulo;
            avisoMensajePendiente = mensaje;
            i.removeExtra("AVISO_TITULO");
            i.removeExtra("AVISO_MENSAJE");
            setIntent(i);
        }
    }

    private void intentarMostrarAvisoPendiente() {
        if (avisoTituloPendiente == null || avisoMensajePendiente == null) return;
        if (tienePermisoNotificaciones()) {
            mostrarNotificacionLocal(avisoTituloPendiente, avisoMensajePendiente);
            avisoTituloPendiente = null;
            avisoMensajePendiente = null;
        }
    }

    private void mostrarNotificacionLocal(String titulo, String cuerpo) {
        String channelId = "canal_fichajes_local_v1";
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId,
                    "Avisos de Fichaje (Local)",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, intent,
                android.app.PendingIntent.FLAG_ONE_SHOT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        androidx.core.app.NotificationCompat.Builder builder =
                new androidx.core.app.NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(titulo != null ? titulo : "Aviso")
                        .setContentText(cuerpo != null ? cuerpo : "")
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify(101, builder.build());
    }

    private void mostrarDialogoCambioPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("CAMBIAR CONTRASEÑA");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etActual = new EditText(this);
        etActual.setHint("Actual");
        etActual.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etActual);

        final EditText etNueva = new EditText(this);
        etNueva.setHint("Nueva (min 6 caracteres)");
        etNueva.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNueva);

        builder.setView(layout);

        builder.setPositiveButton("GUARDAR", (dialog, which) -> {
            String actual = etActual.getText().toString().trim();
            String nueva = etNueva.getText().toString().trim();

            if (actual.isEmpty() || nueva.isEmpty()) {
                mostrarToastPop("Completa ambos campos", false);
                return;
            }
            if (nueva.length() < 6) {
                mostrarToastPop("Mínimo 6 caracteres", false);
                return;
            }

            String token = sessionManager.getAuthToken();
            if (token != null) vm.cambiarPassword("Bearer " + token, actual, nueva);
        });

        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }

    private void mostrarToastPop(String mensaje, boolean esExito) {
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
    }

    private void irALogin() {
        cancelRecordatorioWorker();
        sessionManager.clearSession();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String nfc = pendingNfcCode;
                pendingNfcCode = null;
                obtenerUbicacionYFichar(nfc);
            } else {
                pendingNfcCode = null;
                btnFicharMain.setEnabled(true);
            }
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                intentarMostrarAvisoPendiente();
            }
        }
    }

    private void enviarTokenFCM() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) return;

                    String tokenFCM = task.getResult();
                    String authToken = sessionManager.getAuthToken();
                    if (authToken == null) return;

                    String bearer = "Bearer " + authToken;

                    com.example.trabajoapi.data.FcmTokenRequest request =
                            new com.example.trabajoapi.data.FcmTokenRequest(tokenFCM);

                    RetrofitClient.getInstance().getMyApi().saveFcmToken(bearer, request)
                            .enqueue(new Callback<Void>() {
                                @Override public void onResponse(Call<Void> call, Response<Void> response) { }
                                @Override public void onFailure(Call<Void> call, Throwable t) { }
                            });
                });
    }

    private void scheduleRecordatorioWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                TrabajadorRecordatorio.class,
                15, TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                WORK_UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
        );
    }

    private void cancelRecordatorioWorker() {
        WorkManager.getInstance(this).cancelUniqueWork(WORK_UNIQUE_NAME);
    }

    private void mostrarDialogoRecordatorio(RecordatorioResponse r) {
        if (r == null) return;

        String titulo = (r.getTitulo() != null && !r.getTitulo().trim().isEmpty())
                ? r.getTitulo()
                : "Recordatorio de fichaje";

        String msg = (r.getMensaje() != null && !r.getMensaje().trim().isEmpty())
                ? r.getMensaje()
                : "Te falta fichar. Revisa tu estado.";

        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(msg)
                .setCancelable(true)
                .setPositiveButton("FICHAR AHORA", (d, w) -> btnFicharMain.performClick())
                .setNegativeButton("CERRAR", null)
                .show();
    }
}
