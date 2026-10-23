package com.spring.demo.modelo;

import java.time.LocalDateTime;

public class ReservaModelo {

    private String codigoReserva;
    private String codigoEstudiante;
    private String tipoDeporte;
    private int horasReserva = 1;
    private String estado;
    private double montoPagar = 0.0;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String estadoDevolucion = "PENDIENTE";
    private String tipoDano = "NINGUNO";
    private double montoPenalidad = 0.0;
    private String pagoRealizado; // Inicia nulo para mostrar el selector la primera vez

    public ReservaModelo() {
    }

    public ReservaModelo(String codigoReserva, String codigoEstudiante, String tipoDeporte, int horasReserva, String estado, double montoPagar, LocalDateTime fechaInicio) {
        this.codigoReserva = codigoReserva;
        this.codigoEstudiante = codigoEstudiante;
        this.tipoDeporte = tipoDeporte;
        this.horasReserva = horasReserva;
        this.estado = estado;
        this.montoPagar = 0.0;
        this.fechaInicio = fechaInicio;
    }

    public ReservaModelo(String codigoReserva, String codigoEstudiante, String tipoDeporte, int horasReserva, String estado, double montoPagar, LocalDateTime fechaInicio, String pagoRealizado) {
        this.codigoReserva = codigoReserva;
        this.codigoEstudiante = codigoEstudiante;
        this.tipoDeporte = tipoDeporte;
        this.horasReserva = horasReserva;
        this.estado = estado;
        this.montoPagar = 0.0;
        this.fechaInicio = fechaInicio;
        this.pagoRealizado = pagoRealizado;
    }

    public String getCodigoReserva() {
        return codigoReserva;
    }

    public void setCodigoReserva(String codigoReserva) {
        this.codigoReserva = codigoReserva;
    }

    public String getCodigoEstudiante() {
        return codigoEstudiante;
    }

    public void setCodigoEstudiante(String codigoEstudiante) {
        this.codigoEstudiante = codigoEstudiante;
    }

    public String getTipoDeporte() {
        return tipoDeporte;
    }

    public void setTipoDeporte(String tipoDeporte) {
        this.tipoDeporte = tipoDeporte;
    }

    public int getHorasReserva() {
        return horasReserva;
    }

    public void setHorasReserva(int horasReserva) {
        this.horasReserva = horasReserva;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public double getMontoPagar() {
        return montoPagar;
    }

    public void setMontoPagar(double montoPagar) {
        this.montoPagar = 0.0;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public String getEstadoDevolucion() {
        return estadoDevolucion != null ? estadoDevolucion : "PENDIENTE";
    }

    public void setEstadoDevolucion(String estadoDevolucion) {
        this.estadoDevolucion = estadoDevolucion;
    }

    public String getTipoDano() {
        return tipoDano != null ? tipoDano : "NINGUNO";
    }

    public void setTipoDano(String tipoDano) {
        this.tipoDano = tipoDano;
    }

    public double getMontoPenalidad() {
        return montoPenalidad;
    }

    public void setMontoPenalidad(double montoPenalidad) {
        this.montoPenalidad = montoPenalidad;
    }

    public double getMontoTotal() {
        return this.montoPenalidad;
    }

    public String getPagoRealizado() {
        return pagoRealizado;
    }

    public void setPagoRealizado(String pagoRealizado) {
        this.pagoRealizado = pagoRealizado;
    }
    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }
    public String getDuracionUso() {
        if (fechaInicio == null) return "-";
        LocalDateTime fin = (fechaFin != null) ? fechaFin : LocalDateTime.now();
        long minutosTotales = java.time.Duration.between(fechaInicio, fin).toMinutes();
        
        if (minutosTotales <= 0) return "1 min"; 
        long horas = minutosTotales / 60;
        long mins = minutosTotales % 60;
        
        if (horas > 0) {
            return horas + "h " + mins + "m";
        }
        return mins + " m";
    }
}