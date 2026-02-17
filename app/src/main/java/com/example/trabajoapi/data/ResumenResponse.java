package com.example.trabajoapi.data;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO de respuesta para /api/resumen
 *
 * Objetivo:
 * - Mantener compatibilidad con la versión anterior (mes/teoricas/trabajadas/saldo)
 * - Añadir metadatos del cálculo para reflejar casos con fichajes incompletos
 *   (y evitar mostrar un saldo "falso" cuando faltan pares ENTRADA/SALIDA).
 *
 * Nota:
 * - Los campos *_seg vienen del backend como enteros (segundos) para cálculos precisos.
 * - Los campos en horas (double) son valores ya redondeados que se muestran en UI.
 */
public class ResumenResponse {

    // Mes en formato "MM/YYYY" o texto equivalente (según backend)
    private String mes;

    // Horas teóricas del periodo (ej: mes), ya en horas y normalmente redondeadas.
    private double teoricas;

    // Horas trabajadas del periodo, ya en horas y normalmente redondeadas.
    private double trabajadas;

    // Saldo en horas: trabajadas - teóricas (positivo = extra, negativo = debes horas).
    private double saldo;

    // --- Campos extra para precisión y auditoría ---

    // Segundos teóricos totales del periodo (para evitar acumulación de errores por redondeo).
    @SerializedName("teoricas_seg")
    private long teoricasSeg;

    // Segundos trabajados totales del periodo.
    @SerializedName("trabajadas_seg")
    private long trabajadasSeg;

    // Segundos de saldo (trabajadasSeg - teoricasSeg).
    @SerializedName("saldo_seg")
    private long saldoSeg;

    // Lista de fechas (YYYY-MM-DD) donde el fichaje está incompleto o con inconsistencias.
    @SerializedName("dias_incompletos")
    private List<String> diasIncompletos;

    // Número de días con fichajes incompletos (atajo para UI).
    @SerializedName("num_dias_incompletos")
    private int numDiasIncompletos;

    // True si el cálculo se considera confiable (sin días incompletos), false si requiere revisión.
    @SerializedName("calculo_confiable")
    private boolean calculoConfiable;

    // --- Getters (Gson rellena por reflexión, no hace falta constructor) ---

    public String getMes() {
        return mes;
    }

    public double getTeoricas() {
        return teoricas;
    }

    public double getTrabajadas() {
        return trabajadas;
    }

    public double getSaldo() {
        return saldo;
    }

    public long getTeoricasSeg() {
        return teoricasSeg;
    }

    public long getTrabajadasSeg() {
        return trabajadasSeg;
    }

    public long getSaldoSeg() {
        return saldoSeg;
    }

    public List<String> getDiasIncompletos() {
        return diasIncompletos;
    }

    public int getNumDiasIncompletos() {
        return numDiasIncompletos;
    }

    public boolean isCalculoConfiable() {
        return calculoConfiable;
    }
}
