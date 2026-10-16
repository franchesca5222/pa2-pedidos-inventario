package com.isil.edu.pe.spedidos.client.dto;

public record DevolucionStockResponse(
    String codigo,
    Integer cantidadDevuelta,
    Integer stockRestante
) {
}
