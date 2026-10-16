package com.isil.edu.pe.spedidos.service.impl;

import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import com.isil.edu.pe.spedidos.client.InventarioClientException;
import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.entity.PedidoEstado;
import com.isil.edu.pe.spedidos.exception.AnulacionPedidoException;
import com.isil.edu.pe.spedidos.repository.AnulacionPedidoRepository;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import com.isil.edu.pe.spedidos.service.AnulacionPedidoService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnulacionPedidoServiceImpl
    implements AnulacionPedidoService {

  private final PedidoRepository pedidoRepository;
  private final AnulacionPedidoRepository anulacionPedidoRepository;
  private final DevolucionStockClient devolucionStockClient;

  public AnulacionPedidoServiceImpl(
      PedidoRepository pedidoRepository,
      AnulacionPedidoRepository anulacionPedidoRepository,
      DevolucionStockClient devolucionStockClient
  ) {
    this.pedidoRepository = pedidoRepository;
    this.anulacionPedidoRepository = anulacionPedidoRepository;
    this.devolucionStockClient = devolucionStockClient;
  }

  @Override
  @Transactional
  public AnulacionPedido anularPedido(Long pedidoId, String motivo) {
    // Validar antes de realizar cualquier devolución.
    if (pedidoId == null || pedidoId <= 0) {
      throw new AnulacionPedidoException(
          400,
          "El identificador del pedido debe ser positivo."
      );
    }

    String motivoNormalizado = motivo == null ? null : motivo.trim();

    if (motivoNormalizado != null && motivoNormalizado.length() > 500) {
      throw new AnulacionPedidoException(
          400,
          "El motivo no puede superar los 500 caracteres."
      );
    }

    Pedido pedido = pedidoRepository.findById(pedidoId)
        .orElseThrow(() -> new AnulacionPedidoException(
            404,
            "No existe el pedido con id " + pedidoId + "."
        ));

    if (pedido.getEstado() != PedidoEstado.REGISTRADO
        || anulacionPedidoRepository.existsByPedidoId(pedidoId)) {
      throw new AnulacionPedidoException(
          409,
          "El pedido ya fue anulado o no permite anulación."
      );
    }

    // Construir y validar el registro antes de modificar el stock remoto.
    AnulacionPedido anulacion = new AnulacionPedido(
        pedido.getId(),
        pedido.getProductoCodigo(),
        pedido.getCantidad(),
        motivoNormalizado
    );

    try {
      devolucionStockClient.devolverStock(
          pedido.getProductoCodigo(),
          pedido.getCantidad()
      );
    } catch (InventarioClientException e) {
      // El pedido es local; un 404 remoto corresponde al producto,
      // no a un pedido inexistente.
      throw new AnulacionPedidoException(
          502,
          "Inventario rechazó la devolución (HTTP "
              + e.getStatus() + "): " + e.getMessage()
      );
    }

    // Solo se realizan estos cambios si la devolución fue confirmada.
    pedido.anular();
    pedidoRepository.save(pedido);

    return anulacionPedidoRepository.save(anulacion);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AnulacionPedido> listarAnulaciones() {
    return anulacionPedidoRepository.findAllByOrderByIdDesc();
  }
}