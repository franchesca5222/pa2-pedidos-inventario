package com.isil.edu.pe.spedidos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "anulacion_pedido",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_anulacion_pedido_id",
            columnNames = "pedido_id"
        )
    }
)
public class AnulacionPedido {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "pedido_id", nullable = false)
  private Long pedidoId;

  @Column(name = "producto_codigo", nullable = false, length = 30)
  private String productoCodigo;

  @Column(name = "cantidad_devuelta", nullable = false)
  private int cantidadDevuelta;

  @Column(nullable = false)
  private LocalDateTime fecha;

  @Column(length = 500)
  private String motivo;

  protected AnulacionPedido() {
    // Requerido por JPA.
  }

  public AnulacionPedido(
      Long pedidoId,
      String productoCodigo,
      int cantidadDevuelta,
      String motivo
  ) {
    if (pedidoId == null || pedidoId <= 0) {
      throw new IllegalArgumentException(
          "El identificador del pedido debe ser positivo."
      );
    }

    if (productoCodigo == null || productoCodigo.isBlank()) {
      throw new IllegalArgumentException(
          "El código del producto es obligatorio."
      );
    }

    if (cantidadDevuelta <= 0) {
      throw new IllegalArgumentException(
          "La cantidad devuelta debe ser mayor que cero."
      );
    }

    String motivoNormalizado = motivo == null ? null : motivo.trim();

    if (motivoNormalizado != null && motivoNormalizado.length() > 500) {
      throw new IllegalArgumentException(
          "El motivo no puede superar los 500 caracteres."
      );
    }

    this.pedidoId = pedidoId;
    this.productoCodigo = productoCodigo.trim();
    this.cantidadDevuelta = cantidadDevuelta;
    this.fecha = LocalDateTime.now();
    this.motivo = motivoNormalizado;
  }

  public Long getId() {
    return id;
  }

  public Long getPedidoId() {
    return pedidoId;
  }

  public String getProductoCodigo() {
    return productoCodigo;
  }

  public int getCantidadDevuelta() {
    return cantidadDevuelta;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public String getMotivo() {
    return motivo;
  }
}
