package pe.edu.isil.inventario.domain;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductoInventarioDevolucionTest {

  @Test
  void devolucionValidaIncrementaStock() {
    // Preparación
    ProductoInventario producto = new ProductoInventario(
        "LAP-001", "Laptop", new BigDecimal("2500.00"), 8
    );

    // Ejecución
    producto.devolverStock(2);

    // Verificación
    assertEquals(10, producto.getStock());
  }

  @Test
  void devolucionInvalidaNoModificaStock() {
    // Preparación
    ProductoInventario producto = new ProductoInventario(
        "LAP-001", "Laptop", new BigDecimal("2500.00"), 8
    );

    // Ejecución y verificación
    assertThrows(
        IllegalArgumentException.class,
        () -> producto.devolverStock(0)
    );
    assertEquals(8, producto.getStock());

    assertThrows(
        IllegalArgumentException.class,
        () -> producto.devolverStock(-2)
    );
    assertEquals(8, producto.getStock());
  }
}