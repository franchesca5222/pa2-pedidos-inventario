package com.isil.edu.pe.spedidos.service.impl;

import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.client.dto.DevolucionStockResponse;
import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.entity.PedidoEstado;
import com.isil.edu.pe.spedidos.exception.AnulacionPedidoException;
import com.isil.edu.pe.spedidos.repository.AnulacionPedidoRepository;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnulacionPedidoServiceImplTest {

  @Mock
  private PedidoRepository pedidoRepository;

  @Mock
  private AnulacionPedidoRepository anulacionPedidoRepository;

  @Mock
  private DevolucionStockClient devolucionStockClient;

  private AnulacionPedidoServiceImpl service;

  @BeforeEach
  void prepararServicio() {
    service = new AnulacionPedidoServiceImpl(
        pedidoRepository,
        anulacionPedidoRepository,
        devolucionStockClient
    );
  }

  @Test
  void anulacionCorrectaDevuelveStockYGuardaRegistro() {
    // Preparación
    Pedido pedido = crearPedido();

    when(pedidoRepository.findById(1L))
        .thenReturn(Optional.of(pedido));

    when(anulacionPedidoRepository.existsByPedidoId(1L))
        .thenReturn(false);

    when(devolucionStockClient.devolverStock("LAP-001", 2))
        .thenReturn(new DevolucionStockResponse("LAP-001", 2, 10));

    when(anulacionPedidoRepository.save(any(AnulacionPedido.class)))
        .thenAnswer(invocacion -> invocacion.getArgument(0));

    // Ejecución
    AnulacionPedido resultado = service.anularPedido(
        1L,
        "Solicitud del cliente"
    );

    // Verificación
    assertEquals(PedidoEstado.ANULADO, pedido.getEstado());
    assertEquals(1L, resultado.getPedidoId());
    assertEquals("LAP-001", resultado.getProductoCodigo());
    assertEquals(2, resultado.getCantidadDevuelta());
    assertEquals("Solicitud del cliente", resultado.getMotivo());
    assertNotNull(resultado.getFecha());

    verify(devolucionStockClient, times(1))
        .devolverStock("LAP-001", 2);

    verify(pedidoRepository).save(pedido);

    ArgumentCaptor<AnulacionPedido> captor =
        ArgumentCaptor.forClass(AnulacionPedido.class);

    verify(anulacionPedidoRepository).save(captor.capture());
    assertSame(resultado, captor.getValue());
  }

  @Test
  void anulacionDuplicadaNoDevuelveStockNiGuarda() {
    // Preparación
    Pedido pedido = crearPedido();
    pedido.anular();

    when(pedidoRepository.findById(1L))
        .thenReturn(Optional.of(pedido));

    // Ejecución
    AnulacionPedidoException error = assertThrows(
        AnulacionPedidoException.class,
        () -> service.anularPedido(1L, "Segunda solicitud")
    );

    // Verificación
    assertEquals(409, error.getStatus());
    assertEquals(PedidoEstado.ANULADO, pedido.getEstado());

    verifyNoInteractions(devolucionStockClient);
    verify(pedidoRepository, never()).save(any(Pedido.class));
    verify(anulacionPedidoRepository, never())
        .save(any(AnulacionPedido.class));
  }

  @Test
  void falloInventarioNoAnulaNiGuardaRegistro() {
    // Preparación
    Pedido pedido = crearPedido();

    when(pedidoRepository.findById(1L))
        .thenReturn(Optional.of(pedido));

    when(anulacionPedidoRepository.existsByPedidoId(1L))
        .thenReturn(false);

    when(devolucionStockClient.devolverStock("LAP-001", 2))
        .thenThrow(new InventarioNoDisponibleException(
            "Inventario no disponible."
        ));

    // Ejecución
    assertThrows(
        InventarioNoDisponibleException.class,
        () -> service.anularPedido(1L, "Solicitud del cliente")
    );

    // Verificación
    assertEquals(PedidoEstado.REGISTRADO, pedido.getEstado());

    verify(devolucionStockClient, times(1))
        .devolverStock("LAP-001", 2);

    verify(pedidoRepository, never()).save(any(Pedido.class));
    verify(anulacionPedidoRepository, never())
        .save(any(AnulacionPedido.class));
  }

  private Pedido crearPedido() {
    Pedido pedido = new Pedido(
        "Franchesca",
        "LAP-001",
        "Laptop",
        new BigDecimal("2500.00"),
        2,
        new BigDecimal("5000.00")
    );

    pedido.setId(1L);
    return pedido;
  }
}
