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

/**
 * Pantalla principal del trabajador.
 *
 * Responsabilidades principales:
 * - Fichar entrada/salida (GPS + opcional NFC)
 * - Mostrar resumen mensual y estado de horas
 * - Gestionar incidencias (crear / historial)
 * - Mostrar recordatorios locales de fichaje
 * - Gestionar cierre de sesión seguro (logout en servidor + limpieza local)
 */
public class MainActivity extends AppCompatActivity implements NfcFichajeController.Listener {

    // Request codes de permisos Android
    private static final int PERMISSION_REQUEST_CODE = 112; // Notificaciones (Android 13+)
    private static final int PERMISSION_ID = 44;            // Ubicación (GPS)

    // Nombre único del worker periódico de recordatorios
    private static final String WORK_UNIQUE_NAME = "recordatorio_fichaje_bg";

    // Gestión de sesión local (JWT, rol, etc.)
    private SessionManager sessionManager;

    // Cliente de ubicación para obtener lat/lon al fichar
    private FusedLocationProviderClient fusedLocationClient;

    // Botón principal de fichaje (cambia entre entrada/salida)
    private MaterialButton btnFicharMain;

    // Widgets del resumen mensual / horas extra
    private TextView tvHorasExtraValor;
    private TextView tvEstadoHoras;

    // Card y badges para mostrar estados de revisión del resumen
    private ConstraintLayout cardHorasExtra;
    private TextView tvBadgeRevision;
    private TextView tvInfoRevision;

    // Si el usuario escanea NFC pero falta permiso GPS, guardamos temporalmente el código
    private String pendingNfcCode = null;

    // Aviso pendiente (cuando llega recordatorio pero aún no hay permiso de notificaciones)
    private String avisoTituloPendiente = null;
    private String avisoMensajePendiente = null;

    // ViewModel principal (dashboard, fichajes, recordatorios, logout event, etc.)
    private MainViewModel vm;

    // Helper UI para incidencias (diálogos/toasts)
    private IncidenciaHelper incidenciaHelper;

    // ViewModel de incidencias (crear / historial)
    private IncidenciaViewModel ivm;

    // Controlador NFC desacoplado de la Activity
    private NfcFichajeController nfcController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ocultamos ActionBar para usar la UI custom de la app
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        // Inicialización de utilidades de sesión y ubicación
        sessionManager = new SessionManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicialización del controlador NFC
        nfcController = new NfcFichajeController(this);

        // ViewModel principal (dashboard/fichajes)
        vm = new ViewModelProvider(
                this,
                new MainViewModelFactory(new MainRepository())
        ).get(MainViewModel.class);

        // ViewModel de incidencias
        ivm = new ViewModelProvider(
                this,
                new IncidenciaViewModelFactory(new IncidenciaRepository())
        ).get(IncidenciaViewModel.class);

        // Helper visual para incidencias
        incidenciaHelper = new IncidenciaHelper(this);

        // Referencias UI principales
        btnFicharMain = findViewById(R.id.btnFicharMain);
        tvHorasExtraValor = findViewById(R.id.tvHorasExtraValor);
        tvEstadoHoras = findViewById(R.id.tvEstadoHoras);

        cardHorasExtra = findViewById(R.id.cardHorasExtra);
        tvBadgeRevision = findViewById(R.id.tvBadgeRevision);
        tvInfoRevision = findViewById(R.id.tvInfoRevision);

        // Estado inicial del panel de horas mientras se cargan datos
        tvHorasExtraValor.setText("...");
        tvEstadoHoras.setText("CALCULANDO...");
        if (tvBadgeRevision != null) tvBadgeRevision.setVisibility(View.GONE);
        if (tvInfoRevision != null) tvInfoRevision.setVisibility(View.GONE);

        // Botones secundarios del menú principal
        ImageView btnLogout = findViewById(R.id.btnLogoutIcon);
        AppCompatButton btnIncidencia = findViewById(R.id.btnIncidencia);
        AppCompatButton btnHistorial = findViewById(R.id.btnHistorial);
        AppCompatButton btnMisFichajes = findViewById(R.id.btnMisFichajes);
        AppCompatButton btnCambiarClave = findViewById(R.id.btnCambiarClave);
        AppCompatButton btnAdminPanel = findViewById(R.id.btnAdminPanel);

        // Acción de fichaje manual (sin NFC)
        btnFicharMain.setOnClickListener(v -> {
            btnFicharMain.setEnabled(false);
            btnFicharMain.setText("...");
            checkPermissionsAndFichar(null);
        });

        // Crear incidencia (abre diálogo y envía al backend con JWT)
        if (btnIncidencia != null) {
            btnIncidencia.setOnClickListener(v -> {
                incidenciaHelper.mostrarDialogoNuevaIncidencia((tipo, inicio, fin, comentario) -> {
                    String token = sessionManager.getAuthToken();
                    if (token == null) { irALogin(); return; }
                    ivm.crearIncidencia("Bearer " + token, tipo, inicio, fin, comentario);
                });
            });
        }

        // Historial de incidencias
        if (btnHistorial != null) {
            btnHistorial.setOnClickListener(v -> {
                String token = sessionManager.getAuthToken();
                if (token == null) { irALogin(); return; }
                ivm.cargarHistorial("Bearer " + token);
            });
        }

        // Historial de fichajes (últimos fichajes)
        if (btnMisFichajes != null) {
            btnMisFichajes.setOnClickListener(v -> {
                String token = sessionManager.getAuthToken();
                if (token == null) { irALogin(); return; }
                vm.pedirHistorialParaDialogo("Bearer " + token);
            });
        }

        // Cambio de contraseña
        if (btnCambiarClave != null) {
            btnCambiarClave.setOnClickListener(v -> mostrarDialogoCambioPassword());
        }

        // Logout seguro: intenta revocar token en servidor y luego limpia sesión local
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> cerrarSesionSegura());
        }

        // Botón de panel admin solo visible para roles admin
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

        // Si llegamos desde Login con un aviso, lo preparamos (título/mensaje)
        prepararAvisoLoginSiExiste();

        // Pedimos permisos de notificaciones si aplica y mostramos aviso pendiente si procede
        pedirPermisosNotificaciones();
        intentarMostrarAvisoPendiente();

        // Si ya hay sesión activa, programamos recordatorio periódico en background
        if (sessionManager.getAuthToken() != null) {
            scheduleRecordatorioWorker();
        }

        // Observadores LiveData (VM principal e incidencias)
        observarVM();
        observarIncidenciasVM();

        // Registramos/actualizamos token FCM del dispositivo en backend
        enviarTokenFCM();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Si no hay token local, mandamos a login
        String token = sessionManager.getAuthToken();
        if (token == null) {
            irALogin();
            return;
        }

        // Reactivamos lectura NFC al volver a primer plano
        if (nfcController != null) nfcController.onResume(this);

        // Mantenemos worker activo mientras hay sesión
        scheduleRecordatorioWorker();

        // Refresco de dashboard y comprobación de recordatorio al volver a la pantalla
        String bearer = "Bearer " + token;
        vm.cargarDashboard(bearer);
        vm.comprobarRecordatorio(bearer);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Pausamos lectura NFC al salir de primer plano
        if (nfcController != null) nfcController.onPause(this);
    }

    // =========================
    // Callbacks de NFC
    // =========================

    @Override
    public void onNfcReady(boolean enabled) {
        // Hook disponible por si quieres mostrar estado NFC en UI
    }

    @Override
    public void onTagValida(String nfcId) {
        // NFC válido -> intentamos fichar con validación GPS
        runOnUiThread(() -> {
            mostrarToastPop("Tarjeta detectada. Validando ubicación...", true);
            checkPermissionsAndFichar(nfcId);
        });
    }

    @Override
    public void onTagInvalida(String motivo, String payloadLeido) {
        // Tag leído pero no válido para la lógica de fichaje
        runOnUiThread(() -> mostrarToastPop("Error NFC: " + motivo, false));
    }

    @Override
    public void onNfcError(String motivo) {
        // Error técnico de lectura NFC
        runOnUiThread(() -> mostrarToastPop("Error Lectura: " + motivo, false));
    }

    // =========================
    // Observadores ViewModel principal
    // =========================

    private void observarVM() {

        // Estado de fichaje actual (dentro/fuera) -> actualiza botón principal
        vm.getDentro().observe(this, this::actualizarBotonFichaje);

        // Resumen mensual (horas teóricas, trabajadas, saldo, fiabilidad del cálculo)
        vm.getResumen().observe(this, r -> {

            // Reinicio visual antes de pintar el nuevo estado
            tvHorasExtraValor.setText("...");
            tvHorasExtraValor.setTextColor(ContextCompat.getColor(this, R.color.black));
            tvEstadoHoras.setText("CALCULANDO...");

            if (tvBadgeRevision != null) tvBadgeRevision.setVisibility(View.GONE);
            if (tvInfoRevision != null) tvInfoRevision.setVisibility(View.GONE);

            // Sin datos (respuesta nula)
            if (r == null) {
                tvHorasExtraValor.setText("+0.00 h");
                tvHorasExtraValor.setTextColor(ContextCompat.getColor(this, R.color.black));
                tvEstadoHoras.setText("SIN DATOS DEL MES");
                return;
            }

            // Calculo no confiable (faltan fichajes/pares completos)
            if (!r.isCalculoConfiable()) {
                tvHorasExtraValor.setText("+0.00 h");
                tvHorasExtraValor.setTextColor(ContextCompat.getColor(this, R.color.pop_yellow));
                tvEstadoHoras.setText("PENDIENTE DE REVISIÓN");

                if (tvBadgeRevision != null) tvBadgeRevision.setVisibility(View.VISIBLE);
                if (tvInfoRevision != null) tvInfoRevision.setVisibility(View.VISIBLE);
                return;
            }

            // Calculo confiable -> mostramos saldo positivo como horas extra
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

        // Click en card de resumen -> abre detalle completo
        if (cardHorasExtra != null) {
            cardHorasExtra.setOnClickListener(v -> {
                com.example.trabajoapi.data.ResumenResponse res = vm.getResumen().getValue();
                if (res != null) mostrarDetalleResumen(res);
            });
        }

        // Toasts genéricos del VM
        vm.getToastEvent().observe(this, e -> {
            if (e == null) return;
            String msg = e.getContentIfNotHandled();
            if (msg == null) return;

            String up = msg.toUpperCase();

            // Heurística simple para decidir icono exito
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

        // Recordatorio puntual de fichaje (diálogo + notificación local)
        vm.getRecordatorioEvent().observe(this, e -> {
            if (e == null) return;
            RecordatorioResponse r = e.getContentIfNotHandled();
            if (r == null) return;

            mostrarDialogoRecordatorio(r);

            // Si hay permiso, notificamos ya; si no, dejamos el aviso pendiente
            if (tienePermisoNotificaciones()) {
                mostrarNotificacionLocal(r.getTitulo(), r.getMensaje());
            } else {
                avisoTituloPendiente = r.getTitulo();
                avisoMensajePendiente = r.getMensaje();
                pedirPermisosNotificaciones();
            }
        });

        // Historial de fichajes para mostrar en diálogo simple
        vm.getHistorialDialogEvent().observe(this, e -> {
            if (e == null) return;
            List<FichajeResponse> lista = e.getContentIfNotHandled();
            if (lista == null) return;
            mostrarDialogoHistorialFichajes(lista);
        });

        // Evento de logout forzado desde VM (token inválido, sesión caducada, etc.)
        vm.getLogoutEvent().observe(this, e -> {
            if (e == null) return;
            Boolean must = e.getContentIfNotHandled();
            if (must != null && must) irALogin();
        });
    }

    /**
     * Muestra un diálogo con el detalle del resumen mensual.
     */
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

    /**
     * Convierte una lista de días en texto separado por comas.
     */
    private String joinDias(List<String> dias) {
        if (dias == null || dias.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dias.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(dias.get(i));
        }
        return sb.toString();
    }

    // =========================
    // Observadores ViewModel de incidencias
    // =========================

    private void observarIncidenciasVM() {
        // Toasts del flujo de incidencias
        ivm.getToastEvent().observe(this, e -> {
            if (e == null) return;
            String msg = e.getContentIfNotHandled();
            if (msg != null) {
                boolean ok = msg.toUpperCase().contains("SOLICITUD") || msg.toUpperCase().contains("ENVIAD");
                incidenciaHelper.mostrarToastPop(msg, ok);
            }
        });

        // Historial de incidencias
        ivm.getHistorialEvent().observe(this, e -> {
            if (e == null) return;
            List<IncidenciaResponse> lista = e.getContentIfNotHandled();
            if (lista != null) incidenciaHelper.mostrarDialogoHistorial(lista);
        });

        // Logout forzado desde flujo de incidencias (p. ej. token inválido)
        ivm.getLogoutEvent().observe(this, e -> {
            if (e == null) return;
            Boolean must = e.getContentIfNotHandled();
            if (must != null && must) irALogin();
        });
    }

    /**
     * Muestra los últimos fichajes del usuario en un diálogo de lista simple.
     */
    private void mostrarDialogoHistorialFichajes(List<FichajeResponse> lista) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("MIS ÚLTIMOS FICHAJES");

        if (lista == null || lista.isEmpty()) {
            builder.setMessage("No tienes registros de fichaje aún.");
        } else {
            String[] items = new String[lista.size()];
            for (int i = 0; i < lista.size(); i++) {
                FichajeResponse f = lista.get(i);

                // Formateo básico de fecha ISO -> texto legible
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

    /**
     * Cambia texto/colores del botón de fichaje según si el usuario está dentro o fuera.
     */
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

    // =========================
    // Flujo de fichaje (permisos + GPS + llamada VM)
    // =========================

    /**
     * Comprueba permiso de ubicación y continúa el flujo de fichaje.
     * Si no hay permiso, lo solicita y conserva el NFC leído si existe.
     */
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

            // Rehabilitamos el botón si el flujo era manual y se pidió permiso
            if (nfcCode == null) btnFicharMain.setEnabled(true);
        }
    }

    /**
     * Obtiene ubicación actual y delega el fichaje al ViewModel (manual o con NFC).
     */
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
                        // Sin ubicación -> avisamos y refrescamos estado de fichaje actual
                        mostrarToastPop("Activa el GPS", false);
                        vm.consultarEstadoFichaje("Bearer " + token);
                        if (nfcCode == null) btnFicharMain.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    // Error de ubicación -> restauramos UI y refrescamos estado
                    mostrarToastPop("Error GPS", false);
                    String token = sessionManager.getAuthToken();
                    if (token != null) vm.consultarEstadoFichaje("Bearer " + token);
                    if (nfcCode == null) btnFicharMain.setEnabled(true);
                });
    }

    // =========================
    // Notificaciones locales (recordatorios)
    // =========================

    /**
     * Comprueba permiso de notificaciones (Android 13+).
     */
    private boolean tienePermisoNotificaciones() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Solicita permiso de notificaciones si el sistema lo requiere.
     */
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

    /**
     * Recupera aviso enviado desde LoginActivity (si existía) para mostrarlo después.
     */
    private void prepararAvisoLoginSiExiste() {
        Intent i = getIntent();
        if (i == null) return;

        String titulo = i.getStringExtra("AVISO_TITULO");
        String mensaje = i.getStringExtra("AVISO_MENSAJE");

        if (titulo != null && mensaje != null) {
            avisoTituloPendiente = titulo;
            avisoMensajePendiente = mensaje;

            // Limpiamos extras para evitar mostrar el aviso otra vez tras recrear activity
            i.removeExtra("AVISO_TITULO");
            i.removeExtra("AVISO_MENSAJE");
            setIntent(i);
        }
    }

    /**
     * Muestra el aviso pendiente si ya tenemos permiso de notificaciones.
     */
    private void intentarMostrarAvisoPendiente() {
        if (avisoTituloPendiente == null || avisoMensajePendiente == null) return;

        if (tienePermisoNotificaciones()) {
            mostrarNotificacionLocal(avisoTituloPendiente, avisoMensajePendiente);
            avisoTituloPendiente = null;
            avisoMensajePendiente = null;
        }
    }

    /**
     * Construye y muestra una notificación local.
     */
    private void mostrarNotificacionLocal(String titulo, String cuerpo) {
        String channelId = "canal_fichajes_local_v1";

        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);

        // Canal obligatorio en Android O+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId,
                    "Avisos de Fichaje (Local)",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Al pulsar la notificación, volvemos a MainActivity
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

    // =========================
    // UI auxiliar (cambio password / toasts)
    // =========================

    /**
     * Diálogo simple para cambio de contraseña.
     */
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

    /**
     * Toast visual personalizado (éxito/error).
     */
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

    // =========================
    // Logout seguro (revocación JWT en backend)
    // =========================

    /**
     * Consideramos sesión inválida si el servidor responde 401 o 422.
     * 422 es común en Flask-JWT-Extended cuando el token es inválido/mal formado/expirado.
     */
    private boolean esSesionInvalida(int code) {
        return code == 401 || code == 422;
    }

    /**
     * Cierra sesión intentando revocar el token en servidor.
     *
     * Comportamiento:
     * - Si el logout servidor sale bien -> limpia local y vuelve al login
     * - Si el token ya no sirve (401/422) -> limpia local igualmente
     * - Si falla la red -> limpia local, pero avisa que no se confirmó revocación
     */
    private void cerrarSesionSegura() {
        // Cancelamos worker antes de salir de la sesión
        cancelRecordatorioWorker();

        String token = sessionManager.getAuthToken();
        if (token == null || token.trim().isEmpty()) {
            irALogin();
            return;
        }

        RetrofitClient.getInstance()
                .getMyApi()
                .logout("Bearer " + token)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        int code = response.code();

                        if (response.isSuccessful()) {
                            irALogin();
                            return;
                        }

                        // Si el token ya no es válido, igualmente cerramos sesión local
                        if (esSesionInvalida(code)) {
                            irALogin();
                            return;
                        }

                        // Otros errores HTTP: avisamos pero no bloqueamos cierre local
                        mostrarToastPop(
                                "No se pudo cerrar sesión en servidor (HTTP " + code + "). Se cerrará en el dispositivo.",
                                false
                        );
                        irALogin();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        // Fallo de red/servidor no accesible: cerramos local para no dejar sesión abierta en el móvil
                        mostrarToastPop(
                                "Sesión cerrada en el dispositivo. No se pudo confirmar la revocación en servidor.",
                                false
                        );
                        irALogin();
                    }
                });
    }

    /**
     * Limpia sesión local y navega a LoginActivity.
     */
    private void irALogin() {
        cancelRecordatorioWorker();
        sessionManager.clearSession();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    // =========================
    // Resultado de permisos Android
    // =========================

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Resultado permiso GPS (flujo fichaje)
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

        // Resultado permiso notificaciones (si había aviso pendiente, intentamos mostrarlo)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                intentarMostrarAvisoPendiente();
            }
        }
    }

    // =========================
    // FCM (push token del dispositivo)
    // =========================

    /**
     * Obtiene el token FCM del dispositivo y lo envía al backend.
     * Si falla, no bloquea la app (es un proceso auxiliar).
     */
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

    // =========================
    // WorkManager (recordatorios en background)
    // =========================

    /**
     * Programa (o actualiza) el worker periódico que comprueba recordatorios de fichaje.
     */
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

    /**
     * Cancela el worker de recordatorios (se usa al cerrar sesión).
     */
    private void cancelRecordatorioWorker() {
        WorkManager.getInstance(this).cancelUniqueWork(WORK_UNIQUE_NAME);
    }

    // =========================
    // Diálogo de recordatorio
    // =========================

    /**
     * Muestra recordatorio de fichaje con acción rápida para fichar ahora.
     */
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