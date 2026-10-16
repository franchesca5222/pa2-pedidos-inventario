package pe.edu.isil.inventario.dto;

public class DevolucionStockResponse {

  private String codigo;
  private int cantidadDevuelta;
  private int stockRestante;

  public DevolucionStockResponse() {
  }

  public DevolucionStockResponse(
      String codigo,
      int cantidadDevuelta,
      int stockRestante
  ) {
    this.codigo = codigo;
    this.cantidadDevuelta = cantidadDevuelta;
    this.stockRestante = stockRestante;
  }

  public String getCodigo() {
    return codigo;
  }

  public int getCantidadDevuelta() {
    return cantidadDevuelta;
  }

  public int getStockRestante() {
    return stockRestante;
  }
}