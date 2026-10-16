# isil-2026-ii
DESARROLLO DE APLICACIONES EMPRESARIALES AVANZADO
# Sistema de Pedidos Spring Boot PA2

Autora: Franchesca Burga Paredes.

Proyecto basado en el material del docente:
https://github.com/carnol1989/isil-2026-ii
Rama de referencia: feature/sistema-pedidos-spring.

## Objetivo

Anular pedidos mediante una devolución de stock al servicio remoto de
Inventario y registrar la anulación en H2.

Se conservan el registro y listado de pedidos existentes.
Los pedidos anulados no se eliminan físicamente.

## Requisitos

- Java 21.
- Maven.
- Inventario disponible en el puerto 8082.
- Puerto 8081 disponible.

Tecnologías: Spring Boot, Spring MVC, Spring Data JPA y H2.

## Configuración

Archivo:

src/main/resources/application.properties

Configuración de integración:

```properties
server.port=8081
server.servlet.context-path=/sistema-pedidos
inventario.api.base-url=http://localhost:8082/inventario-service/api/inventario
inventario.api.connect-timeout-ms=2000
inventario.api.read-timeout-ms=3000
```

InventarioHttpConfig configura el bean inventarioHttpClient.
El nuevo cliente reutiliza ese bean y sus tiempos de espera.

Las credenciales locales de H2 se configuran en application.properties.
No incluir credenciales reales de otros entornos en el repositorio.

## Orden de arranque

1. SQL Server.
2. Inventario mediante WildFly.
3. Pedidos mediante Spring Boot.

Desde la carpeta sistema-pedidos-springboot:

```powershell
mvn test
mvn spring-boot:run
```

Formulario:

http://localhost:8081/sistema-pedidos/pedidos

Consola H2:

http://localhost:8081/sistema-pedidos/h2-console

URL JDBC:

jdbc:h2:mem:pedidosdb

H2 funciona en memoria y utiliza create-drop. Reiniciar Pedidos elimina
los pedidos y anulaciones locales, pero no reinicia el stock de SQL Server.

## API de anulaciones

### Registrar anulación

POST http://localhost:8081/sistema-pedidos/api/v1/anulaciones

Solicitud:

```json
{
  "pedidoId": 1,
  "motivo": "Solicitud del cliente"
}
```

El motivo es opcional y admite hasta 500 caracteres.

Ejemplo ejecutable:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/sistema-pedidos/api/v1/anulaciones" `
  -ContentType "application/json" `
  -Body '{"pedidoId":1,"motivo":"Solicitud del cliente"}' |
  ConvertTo-Json
```

Respuesta correcta: HTTP 201 con id, pedidoId, productoCodigo,
cantidadDevuelta, fecha y motivo.

### Consultar anulaciones

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/sistema-pedidos/api/v1/anulaciones" |
  ConvertTo-Json -Depth 5
```

Respuesta: HTTP 200 con el listado de anulaciones.

## Respuestas de error

- 400: identificador inválido o motivo demasiado largo.
- 404: pedido inexistente.
- 409: pedido ya anulado.
- 502: Inventario rechaza la devolución mediante una respuesta HTTP de error.
- 503: Inventario no disponible, error técnico o respuesta no confirmable.

## Modificaciones por capa

- PedidoEstado: REGISTRADO y ANULADO.
- Pedido: estado inicial REGISTRADO y operación anular.
- AnulacionPedido: historial de devoluciones por anulación.
- AnulacionPedidoRepository: persistencia y consulta por pedidoId.
- DevolucionStockClient: contrato de integración.
- DevolucionStockRestClient: comunicación HTTP/JSON.
- AnulacionPedidoServiceImpl: coordinación del flujo.
- AnulacionPedidoController: POST y GET mediante DTO.

La tabla anulacion_pedido tiene una restricción UNIQUE sobre pedido_id.

El Service recibe PedidoRepository, AnulacionPedidoRepository y
DevolucionStockClient mediante constructor. El Controller recibe el
Service mediante constructor.

## Flujo de anulación

1. Validar la solicitud.
2. Buscar el pedido y comprobar que esté REGISTRADO.
3. Comprobar que no tenga una anulación previa.
4. Solicitar la devolución remota.
5. Cambiar el pedido a ANULADO.
6. Guardar el pedido y la anulación en H2.

## Pruebas unitarias nuevas

Clase:

src/test/java/com/isil/edu/pe/spedidos/service/impl/AnulacionPedidoServiceImplTest.java

Pruebas:

- anulacionCorrectaDevuelveStockYGuardaRegistro.
- anulacionDuplicadaNoDevuelveStockNiGuarda.
- falloInventarioNoAnulaNiGuardaRegistro.

Ejecutar:

```powershell
mvn test
```

Resultado obtenido: 8 pruebas totales, incluidas 3 nuevas de PA2,
sin fallos ni errores.

Las tres pruebas nuevas utilizan JUnit y Mockito sin iniciar Spring Boot
ni Inventario. Las pruebas preexistentes pueden iniciar el contexto.

Los mocks comprueban la lógica y las llamadas; no demuestran un rollback
real entre bases ni sustituyen las pruebas funcionales.

## Reproducción de los escenarios funcionales

1. Anulación correcta: registrar 2 unidades, comprobar la reserva,
   anular y verificar devolución, historial y estado ANULADO.
2. Duplicada: repetir la anulación; esperar 409 sin nuevo incremento.
3. Inexistente: anular un ID ausente; esperar 404 sin cambios.
4. Inventario indisponible: registrar otro pedido, detener Inventario
   e intentar anular; esperar 503, estado REGISTRADO y ninguna anulación.
5. Contrato de Inventario: probar devolución válida, cantidades inválidas
   y código inexistente, comprobando las respuestas y el stock.

Consultas de verificación en H2:

```sql
SELECT id, cliente, producto_codigo, cantidad, estado
FROM pedido
ORDER BY id;

SELECT *
FROM anulacion_pedido
ORDER BY id;
```

Conservar Pedidos funcionando durante las evidencias para no perder H2.

## Frontera transaccional y limitaciones

@Transactional administra únicamente los cambios locales en H2.
La devolución remota tiene su propia transacción en Inventario.

Un fallo remoto impide continuar con los guardados locales. Si la
devolución se confirma y después falla H2, la devolución no se revierte
automáticamente.

Un timeout puede dejar un resultado remoto incierto. No se debe repetir
automáticamente un POST sin un mecanismo de idempotencia.

El control de estado y UNIQUE evita duplicados en el flujo secuencial,
pero no garantiza idempotencia distribuida ante solicitudes concurrentes.

La interfaz JSP existente no muestra el estado; se verifica mediante H2.
No se implementó el reto opcional de compensación.

## Relación con PA1

No se implementó nuevamente la edición completa de pedidos.
Cambiar cantidades o productos requeriría coordinar reservas y
devoluciones de stock entre los sistemas.