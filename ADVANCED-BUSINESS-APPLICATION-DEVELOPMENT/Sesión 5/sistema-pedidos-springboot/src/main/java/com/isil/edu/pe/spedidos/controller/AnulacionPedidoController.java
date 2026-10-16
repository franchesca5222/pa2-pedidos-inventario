package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.controller.dto.AnulacionPedidoRequest;
import com.isil.edu.pe.spedidos.controller.dto.AnulacionPedidoResponse;
import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import com.isil.edu.pe.spedidos.exception.AnulacionPedidoException;
import com.isil.edu.pe.spedidos.service.AnulacionPedidoService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    value = "/api/v1/anulaciones",
    produces = MediaType.APPLICATION_JSON_VALUE
)
public class AnulacionPedidoController {

  private final AnulacionPedidoService anulacionPedidoService;

  public AnulacionPedidoController(
      AnulacionPedidoService anulacionPedidoService
  ) {
    this.anulacionPedidoService = anulacionPedidoService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AnulacionPedidoResponse> anular(
      @RequestBody AnulacionPedidoRequest request
  ) {
    if (request == null) {
      throw new AnulacionPedidoException(
          400,
          "Debe enviar los datos de la anulación."
      );
    }

    AnulacionPedido anulacion = anulacionPedidoService.anularPedido(
        request.pedidoId(),
        request.motivo()
    );

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(AnulacionPedidoResponse.from(anulacion));
  }

  @GetMapping
  public List<AnulacionPedidoResponse> listar() {
    return anulacionPedidoService.listarAnulaciones()
        .stream()
        .map(AnulacionPedidoResponse::from)
        .toList();
  }

  @ExceptionHandler(AnulacionPedidoException.class)
  public ResponseEntity<Map<String, Object>> manejarErrorAnulacion(
      AnulacionPedidoException e
  ) {
    return ResponseEntity
        .status(e.getStatus())
        .body(Map.<String, Object>of(
            "status", e.getStatus(),
            "message", e.getMessage()
        ));
  }

  @ExceptionHandler(InventarioNoDisponibleException.class)
  public ResponseEntity<Map<String, Object>> manejarInventarioNoDisponible(
      InventarioNoDisponibleException e
  ) {
    return ResponseEntity
        .status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.<String, Object>of(
            "status", 503,
            "message", e.getMessage()
        ));
  }
}
