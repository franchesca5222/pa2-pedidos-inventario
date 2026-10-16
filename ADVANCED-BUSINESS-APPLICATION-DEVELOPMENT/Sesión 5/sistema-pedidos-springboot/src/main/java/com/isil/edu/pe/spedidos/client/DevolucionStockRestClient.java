package com.isil.edu.pe.spedidos.client;

import com.isil.edu.pe.spedidos.client.dto.ApiError;
import com.isil.edu.pe.spedidos.client.dto.DevolucionStockRequest;
import com.isil.edu.pe.spedidos.client.dto.DevolucionStockResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class DevolucionStockRestClient implements DevolucionStockClient {

  private final RestClient restClient;
  private final String baseUrl;

  public DevolucionStockRestClient(
      @Qualifier("inventarioHttpClient") RestClient restClient,
      @Value("${inventario.api.base-url}") String baseUrl
  ) {
    this.restClient = restClient;
    this.baseUrl = baseUrl;
  }

  @Override
  public DevolucionStockResponse devolverStock(String codigo, int cantidad) {
    if (codigo == null || codigo.isBlank() || cantidad <= 0) {
      throw new InventarioClientException(
          400,
          "El código y una cantidad positiva son obligatorios."
      );
    }

    try {
      DevolucionStockResponse respuesta = restClient
          .post()
          .uri(
              baseUrl + "/productos/codigo/{codigo}/devoluciones",
              codigo.trim()
          )
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .body(new DevolucionStockRequest(cantidad))
          .retrieve()
          .body(DevolucionStockResponse.class);

      validarRespuesta(respuesta, codigo.trim(), cantidad);

      return respuesta;

    } catch (RestClientResponseException e) {
      if (e.getStatusCode().is5xxServerError()) {
        throw new InventarioNoDisponibleException(
            "Inventario presentó un error al devolver stock (HTTP "
                + e.getStatusCode().value() + ").",
            e
        );
      }

      throw convertirError(e);

    } catch (ResourceAccessException e) {
      throw new InventarioNoDisponibleException(
          "No se pudo confirmar la devolución por un error de conexión "
              + "o tiempo de espera. Revise el inventario antes de repetir.",
          e
      );

    } catch (RestClientException e) {
      throw new InventarioNoDisponibleException(
          "No se pudo interpretar la respuesta de Inventario. "
              + "Revise el stock antes de repetir la operación.",
          e
      );
    }
  }

  private void validarRespuesta(
      DevolucionStockResponse respuesta,
      String codigo,
      int cantidad
  ) {
    if (respuesta == null
        || !codigo.equals(respuesta.codigo())
        || respuesta.cantidadDevuelta() == null
        || respuesta.cantidadDevuelta() != cantidad
        || respuesta.stockRestante() == null
        || respuesta.stockRestante() < 0) {
      throw new InventarioNoDisponibleException(
          "Inventario devolvió una respuesta incompleta o inconsistente. "
              + "Revise el stock antes de repetir la operación."
      );
    }
  }

  private InventarioClientException convertirError(
      RestClientResponseException e
  ) {
    String mensaje = "Inventario respondió HTTP "
        + e.getStatusCode().value() + ".";

    try {
      ApiError error = e.getResponseBodyAs(ApiError.class);

      if (error != null
          && error.getMessage() != null
          && !error.getMessage().isBlank()) {
        mensaje = error.getMessage();
      }
    } catch (RuntimeException ignored) {
      // Conserva el mensaje genérico si el error no contiene el JSON esperado.
    }

    return new InventarioClientException(
        e.getStatusCode().value(),
        mensaje
    );
  }
}