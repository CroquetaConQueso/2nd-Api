package com.example.trabajoapi.nfc;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.SystemClock;

public class NfcFichajeController implements NfcAdapter.ReaderCallback {

    public interface Listener {
        void onNfcReady(boolean enabled);
        void onTagValida(String nfcId);
        void onTagInvalida(String motivo, String payloadLeido);
        void onNfcError(String motivo);
    }

    private static final long DEBOUNCE_MS = 2500;

    private final Listener listener;
    private NfcAdapter nfcAdapter;
    private long lastRead = 0;

    public NfcFichajeController(Listener listener) {
        this.listener = listener;
    }

    // Activa el modo lectura NFC y notifica si el dispositivo lo soporta y está habilitado.
    public void onResume(Activity activity) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity);

        if (nfcAdapter == null) {
            if (listener != null) listener.onNfcReady(false);
            return;
        }

        boolean enabled = nfcAdapter.isEnabled();
        if (listener != null) listener.onNfcReady(enabled);

        if (!enabled) return;

        Bundle opts = new Bundle();
        opts.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

        int flags = NfcAdapter.FLAG_READER_NFC_A |
                NfcAdapter.FLAG_READER_NFC_B |
                NfcAdapter.FLAG_READER_NFC_F |
                NfcAdapter.FLAG_READER_NFC_V;

        nfcAdapter.enableReaderMode(activity, this, flags, opts);
    }

    // Desactiva el modo lectura para evitar consumo extra y lecturas fuera de contexto.
    public void onPause(Activity activity) {
        if (nfcAdapter != null) {
            try {
                nfcAdapter.disableReaderMode(activity);
            } catch (Exception ignored) { }
        }
    }

    // Lee el UID, aplica antirrebote y lo entrega a la pantalla como identificador hex.
    @Override
    public void onTagDiscovered(Tag tag) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastRead < DEBOUNCE_MS) return;
        lastRead = now;

        try {
            byte[] idBytes = tag.getId();

            if (idBytes == null || idBytes.length == 0) {
                if (listener != null) listener.onTagInvalida("La tarjeta no tiene UID legible", "");
                return;
            }

            String nfcId = toHexString(idBytes);

            if (listener != null) {
                listener.onTagValida(nfcId);
            }

        } catch (Exception e) {
            if (listener != null) listener.onNfcError("Error leyendo UID: " + e.getMessage());
        }
    }

    // Convierte el array de bytes a hexadecimal en mayúsculas para enviarlo al backend.
    private String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
