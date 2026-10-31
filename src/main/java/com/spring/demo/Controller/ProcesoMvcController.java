package com.spring.demo.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.spring.demo.modelo.ReservaModelo;
import com.spring.demo.servicio.ProcesoService;

@Controller
public class ProcesoMvcController {

    @Autowired
    private ProcesoService servicio;

    private final int STOCK_MAXIMO_POR_DEPORTE = 2;

    @GetMapping("/reservas")
    public String listarReservas(@RequestParam(name = "estado", required = false) String estado, Model model) {
        List<ReservaModelo> todas = servicio.listarReservas();

        if (estado != null && !estado.trim().isEmpty()) {
            todas = servicio.filtrarPorEstado(estado);
        }

        List<ReservaModelo> activas = todas.stream()
                .filter(r -> !"FINALIZADO".equalsIgnoreCase(r.getEstado()))
                .collect(Collectors.toList());

        List<ReservaModelo> historial = servicio.listarHistorial();

        model.addAttribute("reservas", activas);
        model.addAttribute("historial", historial);
        model.addAttribute("indicador", servicio.obtenerIndicador());

        return "proceso";
    }

    @GetMapping("/reservas/nuevo")
    public String mostrarFormulario(Model model) {
        if (!model.containsAttribute("reserva")) {
            model.addAttribute("reserva", new ReservaModelo());
        }

        Map<String, Integer> stockDisponible = new HashMap<>();
        String[] deportes = {"FUTBOL", "VOLEY", "BÁSQUET"};

        for (String deporte : deportes) {
            long ocupados = servicio.listarReservas().stream()
                    .filter(r -> r.getTipoDeporte() != null
                            && r.getTipoDeporte().equalsIgnoreCase(deporte)
                            && "APROBADO".equalsIgnoreCase(r.getEstado()))
                    .count();
            
            long deBaja = servicio.obtenerBalonesBajaPorDeporte(deporte);
            
            int disponible = Math.max(0, STOCK_MAXIMO_POR_DEPORTE - (int) ocupados - (int) deBaja);
            
            stockDisponible.put(deporte, disponible);
        }

        model.addAttribute("stockDisponible", stockDisponible);
        return "nuevo_registro";
    }

    @PostMapping("/reservas/guardar")
    public String guardarReserva(@ModelAttribute("reserva") ReservaModelo reserva, RedirectAttributes redirectAttributes) {
    	String mensajeValidacion = servicio.validarLimiteReservasPorEstudiante(reserva.getCodigoEstudiante());
        if (mensajeValidacion != null) {
            redirectAttributes.addFlashAttribute("errorMessage", mensajeValidacion);
            redirectAttributes.addFlashAttribute("reserva", reserva);
            return "redirect:/reservas/nuevo";
        }
        long deBaja = servicio.obtenerBalonesBajaPorDeporte(reserva.getTipoDeporte());

        if (deBaja >= STOCK_MAXIMO_POR_DEPORTE) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "No se pueden generar reservas por hoy para " + reserva.getTipoDeporte() 
                    + " por falta de balones disponibles (equipos fuera de servicio). Esperar hasta nuevo aviso.");
            redirectAttributes.addFlashAttribute("reserva", reserva);
            return "redirect:/reservas/nuevo";
        }

        long aprobados = servicio.listarReservas().stream()
                .filter(r -> r.getTipoDeporte() != null
                        && r.getTipoDeporte().equalsIgnoreCase(reserva.getTipoDeporte())
                        && "APROBADO".equalsIgnoreCase(r.getEstado()))
                .count();

        if ("APROBADO".equalsIgnoreCase(reserva.getEstado())) {
            if ((aprobados + deBaja) >= STOCK_MAXIMO_POR_DEPORTE) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "No se puede agregar otro registro APROBADO para " + reserva.getTipoDeporte() 
                        + " porque no hay balones operativos libres por el momento.");
                redirectAttributes.addFlashAttribute("reserva", reserva);
                return "redirect:/reservas/nuevo";
            }
        } 
        else if (reserva.getEstado() == null || reserva.getEstado().trim().isEmpty() || "-- Automático --".equalsIgnoreCase(reserva.getEstado())) {
            if ((aprobados + deBaja) >= STOCK_MAXIMO_POR_DEPORTE) {
                reserva.setEstado("OBSERVADO");
            } else {
                reserva.setEstado("APROBADO");
            }
        }

        servicio.registrarReserva(reserva);
        revisarObservados(reserva.getTipoDeporte());
        return "redirect:/reservas";
    }
    @GetMapping("/reservas/eliminar/{codigo}")
    public String eliminarReserva(@PathVariable("codigo") String codigo) {
        ReservaModelo reserva = servicio.listarReservas().stream()
                .filter(r -> r.getCodigoReserva().equalsIgnoreCase(codigo))
                .findFirst()
                .orElse(null);

        String deporte = null;
        if (reserva != null) {
            deporte = reserva.getTipoDeporte();
            reserva.setEstado("FINALIZADO");
            
            servicio.eliminarReserva(codigo);
            
        } else {
            servicio.eliminarReserva(codigo);
        }

        if (deporte != null) {
            revisarObservados(deporte);
        }

        return "redirect:/reservas";
    }

    @PostMapping("/reservas/devolver")
    public String registrarDevolucion(@RequestParam("codigoReserva") String codigoReserva,
                                       @RequestParam("estadoDevolucion") String estadoDevolucion,
                                       @RequestParam(name = "tipoDano", defaultValue = "NINGUNO") String tipoDano) {
        servicio.registrarDevolucion(codigoReserva, estadoDevolucion, tipoDano);
        
        ReservaModelo reserva = servicio.listarReservas().stream()
                .filter(r -> r.getCodigoReserva().equalsIgnoreCase(codigoReserva))
                .findFirst()
                .orElse(null);

        if (reserva != null) {
            revisarObservados(reserva.getTipoDeporte());
        }

        return "redirect:/reservas";
    }

    @PostMapping("/reservas/pago")
    public String actualizarPago(@RequestParam("codigoReserva") String codigoReserva,
                                 @RequestParam("pagoRealizado") String pagoRealizado) {
        
        ReservaModelo reserva = servicio.listarReservas().stream()
                .filter(r -> r.getCodigoReserva() != null && r.getCodigoReserva().equalsIgnoreCase(codigoReserva))
                .findFirst()
                .orElse(null);

        if (reserva != null) {
            reserva.setPagoRealizado(pagoRealizado);
        } else {
            ReservaModelo reservaHistorica = servicio.listarHistorial().stream()
                    .filter(r -> r.getCodigoReserva() != null && r.getCodigoReserva().equalsIgnoreCase(codigoReserva))
                    .findFirst()
                    .orElse(null);
            
            if (reservaHistorica != null) {
                reservaHistorica.setPagoRealizado(pagoRealizado);
            }
        }
        return "redirect:/reservas";
    }

    private void revisarObservados(String deporte) {
        if (deporte == null) return;

        long aprobados = servicio.listarReservas().stream()
                .filter(r -> r.getTipoDeporte() != null
                        && r.getTipoDeporte().equalsIgnoreCase(deporte)
                        && "APROBADO".equalsIgnoreCase(r.getEstado()))
                .count();
        
        long deBaja = servicio.obtenerBalonesBajaPorDeporte(deporte);

        int cuposDisponibles = Math.max(0, STOCK_MAXIMO_POR_DEPORTE - (int) aprobados - (int) deBaja);

        if (cuposDisponibles > 0) {
            List<ReservaModelo> observados = servicio.listarReservas().stream()
                    .filter(r -> r.getTipoDeporte() != null
                            && r.getTipoDeporte().equalsIgnoreCase(deporte)
                            && "OBSERVADO".equalsIgnoreCase(r.getEstado()))
                    .collect(Collectors.toList());

            for (int i = 0; i < Math.min(cuposDisponibles, observados.size()); i++) {
                observados.get(i).setEstado("APROBADO");
            }
        }
    }
    @GetMapping("/reservas/stock")
    public String verControlStock(Model model) {
        model.addAttribute("bajasFutbol", servicio.obtenerBalonesBajaPorDeporte("FUTBOL"));
        model.addAttribute("bajasVoley", servicio.obtenerBalonesBajaPorDeporte("VOLEY"));
        model.addAttribute("bajasBasquet", servicio.obtenerBalonesBajaPorDeporte("BÁSQUET"));
        return "stock";
    }

    @PostMapping("/reservas/stock/reabastecer")
    public String reabastecerStock(@RequestParam("deporte") String deporte) {
        servicio.reabastecerStockDeporte(deporte);
        return "redirect:/reservas/stock";
    }
    @GetMapping("/reservas/historial/pdf")
    public String descargarHistorialPdf(RedirectAttributes redirectAttributes, Model model) {
        List<ReservaModelo> historial = servicio.listarHistorial();
        if (historial == null || historial.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "No es posible descargar el historial sin registros actualmente.");
            return "redirect:/reservas";
        }
        
        model.addAttribute("historial", historial);
        model.addAttribute("fechaGeneracion", java.time.LocalDateTime.now());
        return "historial_pdf"; 
    }
}