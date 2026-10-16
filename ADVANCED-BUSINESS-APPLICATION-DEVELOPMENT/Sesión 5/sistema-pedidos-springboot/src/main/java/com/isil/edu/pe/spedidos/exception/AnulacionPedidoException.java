package com.isil.edu.pe.spedidos.exception;

public class AnulacionPedidoException extends RuntimeException {

  private final int status;

  public AnulacionPedidoException(int status, String mensaje) {
    super(mensaje);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}