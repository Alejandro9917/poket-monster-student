# PokéBattle API

API REST académica para un juego de combate inspirado en Pokémon. El proyecto
demuestra POO mediante entidades, relaciones JPA, DTOs, servicios y controladores
en Spring Boot. Consulta datos públicos de [PokéAPI](https://pokeapi.co/).

El uso del proyecto está limitado a fines académicos; consulta [LICENSE](LICENSE).

## Requisitos

- Docker Desktop (recomendado), o Java 21 y Maven 3.9+.
- Conexión a Internet para los endpoints del catálogo, ya que consumen PokéAPI.

## Ejecutar con Docker

Desde la raíz del repositorio:

```bash
docker compose -f docker/compose.yml up --build -d
```

La API queda disponible en `http://localhost:8080`. Para ver los logs:

```bash
docker compose -f docker/compose.yml logs -f
```

Para detenerla:

```bash
docker compose -f docker/compose.yml down
```

## Ejecutar sin Docker

```bash
mvn clean verify
mvn spring-boot:run
```

La consola de H2 está disponible en `http://localhost:8080/h2-console`.
Usa la URL JDBC `jdbc:h2:mem:pokebattle`, usuario `sa` y contraseña vacía.

## Endpoints

Todas las respuestas son JSON. La URL base es `http://localhost:8080`.

### Jugadores

| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/api/jugadores` | Registra un jugador. |
| `GET` | `/api/jugadores` | Lista los jugadores registrados. |
| `GET` | `/api/jugadores/{id}` | Obtiene un jugador por UUID. |

Crear un jugador:

```bash
curl -X POST http://localhost:8080/api/jugadores \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"Ash"}'
```

Respuesta (`201 Created`):

```json
{
  "id": "c36f6867-b6e7-4b1f-84a4-d8374bf7c421",
  "nombre": "Ash"
}
```

El campo `nombre` es obligatorio, tiene un máximo de 40 caracteres y no puede
repetirse sin distinguir mayúsculas de minúsculas. Un nombre duplicado devuelve
`409 Conflict`.

### Catálogo Pokémon

| Método | Ruta | Parámetros | Descripción |
| --- | --- | --- | --- |
| `GET` | `/api/catalogo/pokemon` | `page` opcional, desde `0` | Lista 50 Pokémon por página. |
| `GET` | `/api/catalogo/pokemon/buscar` | `nombre` obligatorio | Busca hasta 20 Pokémon por nombre. |
| `GET` | `/api/catalogo/pokemon/{idONombre}` | ID o nombre | Obtiene detalle, estadísticas y tipos. |

Ejemplos:

```bash
curl 'http://localhost:8080/api/catalogo/pokemon?page=0'
curl 'http://localhost:8080/api/catalogo/pokemon/buscar?nombre=pika'
curl http://localhost:8080/api/catalogo/pokemon/pikachu
```

La respuesta paginada contiene `contenido`, `pagina`, `tamano`,
`totalElementos` y `totalPaginas`. Si PokéAPI no está disponible, la API
responde `502 Bad Gateway`.

## Estructura

```text
src/main/java/sv/edu/udb/pokebattle/
├── client/        Cliente HTTP para PokéAPI
├── config/        Configuración de Spring
├── controller/    Endpoints REST
├── dto/           Objetos de solicitud y respuesta
├── exception/     Excepciones de dominio
├── model/         Entidades y estados del dominio
├── repository/    Acceso a datos con JPA
└── service/       Reglas de aplicación
```

La base de datos H2 se ejecuta en memoria: sus datos se eliminan cuando la
aplicación se detiene.
