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

### Equipos

| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/api/jugadores/{jugadorId}/equipos` | Crea un equipo que pertenece al jugador. |
| `GET` | `/api/jugadores/{jugadorId}/equipos` | Lista los equipos de un jugador. |

Para crear un equipo:

```bash
curl -X POST http://localhost:8080/api/jugadores/UUID-DEL-JUGADOR/equipos \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"Equipo Kanto"}'
```

Guarda el valor `id` de la respuesta como `equipoId`.

### Partidas y salas

| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/api/partidas` | Crea una sala y agrega al primer participante. |
| `POST` | `/api/partidas/{partidaId}/unirse` | Agrega el segundo participante a la sala. |
| `GET` | `/api/partidas/{partidaId}` | Consulta el estado y participantes de una sala. |

## Guía: crear una partida

> Al reiniciar la API se eliminan todos los datos, pues H2 funciona en memoria.
> Ejecuta estos pasos en el mismo ciclo de ejecución.

1. Inicia la aplicación y define la URL base:

   ```bash
   BASE_URL=http://localhost:8080
   ```

2. Crea dos jugadores y guarda los UUID recibidos:

   ```bash
   curl -X POST "$BASE_URL/api/jugadores" \
     -H 'Content-Type: application/json' -d '{"nombre":"Ash"}'

   curl -X POST "$BASE_URL/api/jugadores" \
     -H 'Content-Type: application/json' -d '{"nombre":"Misty"}'
   ```

3. Crea un equipo para cada jugador. Sustituye los UUID por los obtenidos en
   el paso anterior y guarda los `id` de ambas respuestas:

   ```bash
   curl -X POST "$BASE_URL/api/jugadores/UUID-ASH/equipos" \
     -H 'Content-Type: application/json' -d '{"nombre":"Equipo Ash"}'

   curl -X POST "$BASE_URL/api/jugadores/UUID-MISTY/equipos" \
     -H 'Content-Type: application/json' -d '{"nombre":"Equipo Misty"}'
   ```

4. El primer jugador crea la sala:

   ```bash
   curl -X POST "$BASE_URL/api/partidas" \
     -H 'Content-Type: application/json' \
     -d '{"jugadorId":"UUID-ASH","equipoId":"UUID-EQUIPO-ASH"}'
   ```

   La respuesta es `201 Created`, con estado `ESPERANDO_JUGADOR`. Guarda su
   campo `id` como `partidaId`; `codigoSala` es el identificador corto de la
   sala para compartir con el rival.

5. El segundo jugador se une a la sala:

   ```bash
   curl -X POST "$BASE_URL/api/partidas/UUID-PARTIDA/unirse" \
     -H 'Content-Type: application/json' \
     -d '{"jugadorId":"UUID-MISTY","equipoId":"UUID-EQUIPO-MISTY"}'
   ```

   La sala pasa a `ESPERANDO_CONFIRMACION` y debe devolver dos participantes.

6. Comprueba la sala cuando lo necesites:

   ```bash
   curl "$BASE_URL/api/partidas/UUID-PARTIDA"
   ```

La API rechaza equipos inexistentes (`404`), equipos pertenecientes a otro
jugador (`400`), un jugador repetido o una sala llena (`409`).

## Estado del combate

El flujo de sala está disponible hasta que ambos jugadores se unen. Para jugar
una batalla completa aún faltan en el código actual los endpoints para agregar
Pokémon y movimientos a un equipo, confirmar e iniciar la partida, atacar,
cambiar Pokémon, rendirse y consultar el historial. Por tanto, **no es posible
iniciar ni jugar el combate todavía**; esta guía no pretende presentar esas
operaciones pendientes como funcionalidades disponibles.

Cuando se implemente el resto de `tasks/fase2.md`, el flujo continuará así:

1. Cada equipo deberá tener exactamente tres Pokémon y al menos un movimiento
   válido por Pokémon.
2. Los dos participantes confirmarán la sala e iniciarán la partida.
3. El jugador indicado por el turno atacará, cambiará su Pokémon o se rendirá.
4. Se podrá consultar estado, turnos, movimientos, daño y resultado final.

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
