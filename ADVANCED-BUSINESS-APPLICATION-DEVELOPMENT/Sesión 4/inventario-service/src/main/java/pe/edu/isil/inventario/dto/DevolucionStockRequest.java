package pe.edu.isil.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DevolucionStockRequest {

  @NotNull(message = "La cantidad es obligatoria.")
  @Min(value = 1, message = "La cantidad debe ser mayor que cero.")
  private Integer cantidad;

  public DevolucionStockRequest() {
  }

  public Integer getCantidad() {
    return cantidad;
  }

  public void setCantidad(Integer cantidad) {
    this.cantidad = cantidad;
  }
}
