# Inventario Service PA2

Autora: Franchesca Burga Paredes.

Proyecto basado en el material del docente:
https://github.com/carnol1989/isil-2026-ii
Rama de referencia: feature/sistema-pedidos-spring.

## Objetivo

Exponer una API REST para devolver stock de productos. Esta operación es
utilizada por el sistema de pedidos cuando se anula un pedido.

## Requisitos

- Java 21.
- Maven.
- SQL Server con TCP/IP habilitado y puerto 1433.
- Autenticación SQL Server habilitada.
- Puerto HTTP 8082 disponible.

El proyecto utiliza Jakarta EE 11 y WildFly 41. El plugin Maven provisiona
WildFly y el soporte de Microsoft SQL Server.

## Preparación de la base

Para una instalación nueva, ejecutar en SSMS:

database/01_crear_inventario.sql

El script crea InventarioDB, el usuario académico y los productos iniciales.
Revisar las credenciales del script antes de ejecutarlo.

La conexión del servidor se configura localmente en:

config/database.properties

Configurar la URL JDBC, el usuario y la contraseña de la instalación.
No publicar contraseñas reales en Git ni incluirlas en las evidencias.

La unidad InventarioPU utiliza el datasource:

java:jboss/datasources/InventarioDS

Pedidos no se conecta directamente a esta base.

## Orden de arranque

1. Iniciar SQL Server.
2. Iniciar Inventario en WildFly.
3. Iniciar sistema-pedidos-springboot.

Desde la carpeta inventario-service:

```powershell
mvn test
mvn wildfly:run
```

WildFly utiliza HTTP 8082 y administración 9992.

Detener WildFly antes de ejecutar mvn clean, porque el servidor utiliza
archivos dentro de target.

## API de devolución

Método: POST

URL:

http://localhost:8082/inventario-service/api/inventario/productos/codigo/{codigo}/devoluciones

Solicitud:

```json
{
  "cantidad": 2
}
```

Ejemplo de respuesta HTTP 200:

```json
{
  "codigo": "LAP-001",
  "cantidadDevuelta": 2,
  "stockRestante": 10
}
```

Ejemplo ejecutable en PowerShell:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8082/inventario-service/api/inventario/productos/codigo/LAP-001/devoluciones" `
  -ContentType "application/json" `
  -Body '{"cantidad":2}'
```

Cada ejecución válida incrementa el stock. Consultar el valor actual antes
de repetir una devolución.

## Consulta de stock

```powershell
Invoke-RestMethod -Uri "http://localhost:8082/inventario-service/api/inventario/productos/codigo/LAP-001/stock"
```

## Validaciones

- Cantidad cero o negativa: HTTP 400.
- Cantidad ausente: solicitud inválida.
- Código inexistente: HTTP 404.
- Conflicto de actualización concurrente: error controlado.

## Modificaciones por capa

- DTO: DevolucionStockRequest y DevolucionStockResponse.
- ProductoResource: endpoint de devoluciones por código.
- InventarioService: operación transaccional devolverStockPorCodigo.
- ProductoInventario: operación de dominio devolverStock.

La operación incrementa el stock dentro de una transacción local.
Se conserva @Version para el control de concurrencia optimista.

## Pruebas unitarias nuevas

Clase:

src/test/java/pe/edu/isil/inventario/domain/ProductoInventarioDevolucionTest.java

Pruebas:

- devolucionValidaIncrementaStock.
- devolucionInvalidaNoModificaStock.

Ejecutar:

```powershell
mvn test
```

Resultado obtenido: 4 pruebas totales, incluidas 2 nuevas de PA2,
sin fallos ni errores.

Estas pruebas no necesitan WildFly ni SQL Server.

## Pruebas funcionales

1. Consultar el stock antes de devolver.
2. Devolver una cantidad positiva y comprobar el incremento.
3. Enviar cantidad 0 y -2: comprobar HTTP 400.
4. Consultar que las solicitudes inválidas no cambiaron el stock.
5. Enviar un código inexistente: comprobar HTTP 404.

## Limitaciones

La API de devolución no implementa idempotencia ni reintentos automáticos.
Una devolución repetida puede incrementar nuevamente el stock.

La transacción de Inventario no incluye la base H2 de Pedidos.
No se implementó el reto opcional de compensación.