package com.spring.demo.servicio;

import java.util.ArrayList;
import com.spring.demo.modelo.ReservaModelo;
import org.springframework.stereotype.Repository;

@Repository
public class RegistroRepositoryMemory {

    private final ArrayList<ReservaModelo> registros = new ArrayList<>();
    private final ArrayList<ReservaModelo> historial = new ArrayList<>();
    private int secuencial = 1001;
    private final int STOCK_MAXIMO = 2;

    public void agregar(ReservaModelo registro) {
        if (registro.getCodigoReserva() == null || registro.getCodigoReserva().trim().isEmpty()) {
            registro.setCodigoReserva("RES-" + secuencial++);
        }

        long aprobadosActuales = registros.stream()
                .filter(r -> r.getTipoDeporte().equalsIgnoreCase(registro.getTipoDeporte())
                        && "APROBADO".equalsIgnoreCase(r.getEstado()))
                .count();

        if (registro.getEstado() == null || registro.getEstado().trim().isEmpty()) {
            if (aprobadosActuales >= STOCK_MAXIMO) {
                registro.setEstado("OBSERVADO");
            } else {
                registro.setEstado("APROBADO");
            }
        }
        registros.add(registro);
    }

    public ArrayList<ReservaModelo> listar() {
        return registros;
    }

    public ReservaModelo buscarPorCodigo(String codigo) {
        return registros.stream()
                .filter(r -> r.getCodigoReserva().equalsIgnoreCase(codigo))
                .findFirst()
                .orElse(null);
    }

    public ArrayList<ReservaModelo> filtrarPorEstado(String estado) {
        ArrayList<ReservaModelo> resultado = new ArrayList<>();
        for (ReservaModelo r : registros) {
            if (r.getEstado().equalsIgnoreCase(estado)) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    public void eliminarPorCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return;
        }
        String codigoLimpio = codigo.trim();

        ReservaModelo aEliminar = null;
        for (ReservaModelo r : registros) {
            if (r.getCodigoReserva() != null && r.getCodigoReserva().trim().equalsIgnoreCase(codigoLimpio)) {
                aEliminar = r;
                break;
            }
        }

        if (aEliminar != null) {
            if (aEliminar.getFechaFin() == null) {
                aEliminar.setFechaFin(java.time.LocalDateTime.now());
            }
            
            aEliminar.setEstado("FINALIZADO");

            ReservaModelo historicoItem = new ReservaModelo();
            historicoItem.setCodigoReserva(aEliminar.getCodigoReserva());
            historicoItem.setCodigoEstudiante(aEliminar.getCodigoEstudiante());
            historicoItem.setTipoDeporte(aEliminar.getTipoDeporte());
            historicoItem.setHorasReserva(aEliminar.getHorasReserva());
            historicoItem.setFechaInicio(aEliminar.getFechaInicio());
            historicoItem.setFechaFin(aEliminar.getFechaFin());
            historicoItem.setEstado("FINALIZADO");
            historicoItem.setEstadoDevolucion(aEliminar.getEstadoDevolucion());
            historicoItem.setTipoDano(aEliminar.getTipoDano());
            historicoItem.setMontoPenalidad(aEliminar.getMontoPenalidad());

            if (!historial.contains(historicoItem)) {
                historial.add(historicoItem);
            }

            String deporte = aEliminar.getTipoDeporte();
            String estadoEliminado = aEliminar.getEstado();

            registros.removeIf(r -> r.getCodigoReserva() != null 
                    && r.getCodigoReserva().trim().equalsIgnoreCase(codigoLimpio));

            if ("APROBADO".equalsIgnoreCase(estadoEliminado) && deporte != null) {
                long bajasTotales = streamBajas(deporte);
                long aprobadosActuales = registros.stream()
                    .filter(r -> r.getTipoDeporte() != null 
                            && r.getTipoDeporte().equalsIgnoreCase(deporte) 
                            && "APROBADO".equalsIgnoreCase(r.getEstado()))
                    .count();

                if ((aprobadosActuales + bajasTotales) < STOCK_MAXIMO) {
                    for (ReservaModelo r : registros) {
                        if (r.getTipoDeporte() != null 
                                && r.getTipoDeporte().equalsIgnoreCase(deporte) 
                                && "OBSERVADO".equalsIgnoreCase(r.getEstado())) {
                            r.setEstado("APROBADO");
                            break;
                        }
                    }
                }
            }
        }
    }
    private long streamBajas(String deporte) {
        long bajasActivas = registros.stream()
            .filter(r -> deporte.equalsIgnoreCase(r.getTipoDeporte()))
            .filter(r -> "PERDIDO".equalsIgnoreCase(r.getEstadoDevolucion()) 
                      || ("DANADO".equalsIgnoreCase(r.getEstadoDevolucion()) 
                          && "HUECO".equalsIgnoreCase(r.getTipoDano())))
            .count();

        long bajasHist = historial.stream()
            .filter(r -> deporte.equalsIgnoreCase(r.getTipoDeporte()))
            .filter(r -> "PERDIDO".equalsIgnoreCase(r.getEstadoDevolucion()) 
                      || ("DANADO".equalsIgnoreCase(r.getEstadoDevolucion()) 
                          && "HUECO".equalsIgnoreCase(r.getTipoDano())))
            .count();

        return bajasActivas + bajasHist;
    }

    public ArrayList<ReservaModelo> listarHistorial() {
        return historial;
    }
  
    public double calcularIndicador() {
        if (registros.isEmpty()) return 0.0;
        long conteoMaximo = registros.stream().filter(r -> r.getHorasReserva() == 2).count();
        return ((double) conteoMaximo / registros.size()) * 100.0;
    }
}