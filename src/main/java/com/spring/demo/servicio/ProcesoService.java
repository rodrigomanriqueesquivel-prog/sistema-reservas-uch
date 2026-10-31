package com.spring.demo.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.spring.demo.modelo.ReservaModelo;

@Service
public class ProcesoService {

    @Autowired
    private RegistroRepositoryMemory repository;

    public void registrarReserva(ReservaModelo reserva) {
        reserva.setMontoPagar(0.0);
        reserva.setFechaInicio(LocalDateTime.now());
        repository.agregar(reserva);
    }

    public ArrayList<ReservaModelo> listarReservas() {
        return repository.listar();
    }
    
    public ArrayList<ReservaModelo> listarHistorial() {
        return repository.listarHistorial(); 
    }

    public ArrayList<ReservaModelo> filtrarPorEstado(String estado) {
        return repository.filtrarPorEstado(estado);
    }

    public void eliminarReserva(String codigo) {
        repository.eliminarPorCodigo(codigo);
    }

    public double obtenerIndicador() {
        return repository.calcularIndicador();
    }

    public void registrarDevolucion(String codigoReserva, String estadoDevolucion, String tipoDano) {
        ReservaModelo reserva = repository.buscarPorCodigo(codigoReserva);
        if (reserva != null) {
        	reserva.setFechaFin(LocalDateTime.now());
            double penalidad = 0.0;
            String deporte = reserva.getTipoDeporte() != null ? reserva.getTipoDeporte().toUpperCase() : "";

            if ("DANADO".equalsIgnoreCase(estadoDevolucion)) {
                if ("FUTBOL".equals(deporte)) {
                    if ("HUECO".equalsIgnoreCase(tipoDano)) {
                        penalidad = 10.0;
                    } else if ("PELADO".equalsIgnoreCase(tipoDano)) {
                        penalidad = 6.0;
                    }
                } else if ("VOLEY".equals(deporte)) {
                    if ("HUECO".equalsIgnoreCase(tipoDano)) {
                        penalidad = 10.0;
                    }
                } else if ("BÁSQUET".equals(deporte) || "BASQUET".equals(deporte)) {
                    if ("HUECO".equalsIgnoreCase(tipoDano)) {
                        penalidad = 13.0;
                    }
                }
            } else if ("PERDIDO".equalsIgnoreCase(estadoDevolucion)) {
                if ("FUTBOL".equals(deporte)) {
                    penalidad = 20.0;
                } else if ("VOLEY".equals(deporte)) {
                    penalidad = 18.0;
                } else if ("BÁSQUET".equals(deporte) || "BASQUET".equals(deporte)) {
                    penalidad = 20.0;
                }
            }

            reserva.setEstadoDevolucion(estadoDevolucion);
            reserva.setTipoDano(tipoDano);
            reserva.setMontoPenalidad(penalidad);
        }
    }
    public long obtenerBalonesBajaPorDeporte(String deporte) {
        if (deporte == null) return 0;

        long bajasActivas = 0;
        if (repository != null && repository.listar() != null) {
            bajasActivas = repository.listar().stream()
                .filter(r -> deporte.equalsIgnoreCase(r.getTipoDeporte()))
                .filter(r -> "PERDIDO".equalsIgnoreCase(r.getEstadoDevolucion()) 
                          || ("DANADO".equalsIgnoreCase(r.getEstadoDevolucion()) 
                              && "HUECO".equalsIgnoreCase(r.getTipoDano())))
                .count();
        }

        long bajasHistorial = 0;
        if (listarHistorial() != null) {
            bajasHistorial = listarHistorial().stream()
                .filter(r -> deporte.equalsIgnoreCase(r.getTipoDeporte()))
                .filter(r -> "PERDIDO".equalsIgnoreCase(r.getEstadoDevolucion()) 
                          || ("DANADO".equalsIgnoreCase(r.getEstadoDevolucion()) 
                              && "HUECO".equalsIgnoreCase(r.getTipoDano())))
                .count();
        }

        return bajasActivas + bajasHistorial;
    }
    public String validarLimiteReservasPorEstudiante(String codigoEstudiante) {
        if (codigoEstudiante == null || codigoEstudiante.trim().isEmpty()) {
            return null;
        }

        String codigoLimpio = codigoEstudiante.trim();
        java.time.LocalDate hoy = java.time.LocalDate.now();

        java.util.List<ReservaModelo> reservasHoy = listarHistorial().stream()
                .filter(r -> r.getCodigoEstudiante() != null 
                        && r.getCodigoEstudiante().trim().equalsIgnoreCase(codigoLimpio))
                .filter(r -> (r.getFechaFin() != null && r.getFechaFin().toLocalDate().equals(hoy))
                          || (r.getFechaInicio() != null && r.getFechaInicio().toLocalDate().equals(hoy)))
                .collect(java.util.stream.Collectors.toList());

        boolean tieneActiva = listarReservas().stream()
                .anyMatch(r -> r.getCodigoEstudiante() != null 
                        && r.getCodigoEstudiante().trim().equalsIgnoreCase(codigoLimpio));

        boolean estaEnHistorialHoy = !reservasHoy.isEmpty();

        if (tieneActiva && !estaEnHistorialHoy) {
            return "El estudiante con código " + codigoLimpio + " ya cuenta con una reserva activa en curso.";
        }

        if (reservasHoy.size() >= 2) {
            return "El estudiante con código " + codigoLimpio + " ya alcanzó el límite máximo de 2 reservas por día.";
        }

        if (reservasHoy.size() == 1) {
            ReservaModelo ultimaReserva = reservasHoy.get(0);
            java.time.LocalDateTime horaReferencia = ultimaReserva.getFechaFin() != null 
                    ? ultimaReserva.getFechaFin() 
                    : ultimaReserva.getFechaInicio();

            if (horaReferencia != null) {
                long minutosTranscurridos = java.time.Duration.between(horaReferencia, java.time.LocalDateTime.now()).toMinutes();
                
                if (minutosTranscurridos < 120) {
                    long minutosRestantes = 120 - minutosTranscurridos;
                    return "El estudiante con código " + codigoLimpio + " tiene una segunda reserva disponible, esperar " + minutosRestantes + " minuto(s) para poder usarla (regla de 2 horas de intervalo).";
                }
            }
        }

        return null; 
    }
    public void reabastecerStockDeporte(String deporte) {
        if (deporte == null) return;
        
        boolean reabastecido = false;

        if (repository != null) {
            if (repository.listarHistorial() != null) {
                for (ReservaModelo r : repository.listarHistorial()) {
                    if (r.getTipoDeporte() != null && r.getTipoDeporte().equalsIgnoreCase(deporte)) {
                        String estado = r.getEstadoDevolucion();
                        String dano = r.getTipoDano();
                        boolean esBaja = "PERDIDO".equalsIgnoreCase(estado) || 
                                         ("DANADO".equalsIgnoreCase(estado) && "HUECO".equalsIgnoreCase(dano));
                        if (esBaja) {
                            r.setEstadoDevolucion("CONFORME");
                            r.setTipoDano(null);
                            reabastecido = true;
                            break;
                        }
                    }
                }
            }
            
            if (!reabastecido && repository.listar() != null) {
                for (ReservaModelo r : repository.listar()) {
                    if (r.getTipoDeporte() != null && r.getTipoDeporte().equalsIgnoreCase(deporte)) {
                        String estado = r.getEstadoDevolucion();
                        String dano = r.getTipoDano();
                        boolean esBaja = "PERDIDO".equalsIgnoreCase(estado) || 
                                         ("DANADO".equalsIgnoreCase(estado) && "HUECO".equalsIgnoreCase(dano));
                        if (esBaja) {
                            r.setEstadoDevolucion("CONFORME");
                            r.setTipoDano(null);
                            reabastecido = true;
                            break;
                        }
                    }
                }
            }
        }
        if (reabastecido) {
            revisarObservados(deporte);
        }
    }
    private void revisarObservados(String deporte) {
        if (deporte == null || repository == null) return;
        
        final int STOCK_MAXIMO_POR_DEPORTE = 2;

        long aprobados = 0;
        if (repository.listar() != null) {
            aprobados = repository.listar().stream()
                    .filter(r -> r.getTipoDeporte() != null 
                            && r.getTipoDeporte().equalsIgnoreCase(deporte)
                            && "APROBADO".equalsIgnoreCase(r.getEstado()))
                    .count();
        }

        long deBaja = obtenerBalonesBajaPorDeporte(deporte);
        int cuposDisponibles = Math.max(0, STOCK_MAXIMO_POR_DEPORTE - (int) aprobados - (int) deBaja);

        if (cuposDisponibles > 0 && repository.listar() != null) {
            java.util.List<ReservaModelo> observados = repository.listar().stream()
                    .filter(r -> r.getTipoDeporte() != null 
                            && r.getTipoDeporte().equalsIgnoreCase(deporte)
                            && "OBSERVADO".equalsIgnoreCase(r.getEstado()))
                    .collect(java.util.stream.Collectors.toList());

            for (int i = 0; i < Math.min(cuposDisponibles, observados.size()); i++) {
                observados.get(i).setEstado("APROBADO");
            }
        }
    }
}