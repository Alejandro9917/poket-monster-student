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

## Guía completa de combate (fase 1 y fase 2)

Esta sección documenta el contrato completo necesario para jugar una partida.
Las rutas marcadas con **Fase 2** requieren que la implementación de combate de
`tasks/fase2.md` esté incorporada; en el estado actual del código esas rutas
aún no están disponibles.

### 1. Formar equipos

Cada jugador debe crear un equipo con exactamente tres Pokémon. Cada Pokémon
debe incluir entre uno y cuatro movimientos válidos para ese Pokémon.

| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/api/equipos/{equipoId}/pokemon` | Agrega un Pokémon al equipo. |
| `PUT` | `/api/equipos/{equipoId}/pokemon/{pokemonEquipoId}` | Reemplaza un Pokémon y sus movimientos. |
| `GET` | `/api/equipos/{equipoId}` | Consulta el equipo, Pokémon y UUID de movimientos. |

Ejemplo para agregar a Pikachu usando movimientos de PokéAPI:

```bash
curl -X POST "$BASE_URL/api/equipos/UUID-EQUIPO/pokemon" \
  -H 'Content-Type: application/json' \
  -d '{"pokemonApiId":25,"movimientosApiId":[85,98]}'
```

`pokemonApiId` y los valores de `movimientosApiId` son identificadores
numéricos de PokéAPI. Para atacar después se usa el UUID devuelto para cada
movimiento guardado en el equipo, no el ID numérico de PokéAPI.

### 2. Confirmar e iniciar la partida

Después de crear la sala y unir al segundo participante, ambos jugadores deben
confirmar. Solo entonces la partida puede iniciarse.

| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/api/partidas/{partidaId}/jugadores/{jugadorId}/confirmar` | Confirma al participante. |
| `POST` | `/api/partidas/{partidaId}/iniciar` | Inicia el combate cuando ambos confirmaron. |

```bash
curl -X POST "$BASE_URL/api/partidas/UUID-PARTIDA/jugadores/UUID-ASH/confirmar"
curl -X POST "$BASE_URL/api/partidas/UUID-PARTIDA/jugadores/UUID-MISTY/confirmar"
curl -X POST "$BASE_URL/api/partidas/UUID-PARTIDA/iniciar"
```

Al iniciar, la partida pasa a `EN_CURSO`, `numeroTurno` comienza en `1` y se
elige el primer turno según la mayor velocidad base de los equipos. El primer
Pokémon de cada equipo queda activo con su vida máxima.

### 3. Consultar estado y turno — **Fase 2**

| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/api/partidas/{partidaId}/estado` | Muestra participantes, Pokémon, vida, activos, estado, turno y ganador. |
| `GET` | `/api/partidas/{partidaId}/turno` | Indica quién debe realizar la siguiente acción. |

Antes de cada acción, consulta el turno:

```bash
curl "$BASE_URL/api/partidas/UUID-PARTIDA/turno"
```

Solo el UUID incluido en `jugadorId` puede actuar. Una acción fuera de turno
recibe `409 Conflict`.

### 4. Jugar un turno — **Fase 2**

El jugador con el turno puede atacar con el Pokémon activo o cambiarlo por un
Pokémon no debilitado. Ambos consumen el turno.

| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/api/partidas/{partidaId}/acciones/atacar` | Ejecuta un movimiento del Pokémon activo. |
| `POST` | `/api/partidas/{partidaId}/acciones/cambiar-pokemon` | Cambia el Pokémon activo. |
| `POST` | `/api/partidas/{partidaId}/acciones/rendirse` | Finaliza la partida y concede la victoria al rival. |

Atacar usando el UUID del movimiento seleccionado:

```bash
curl -X POST "$BASE_URL/api/partidas/UUID-PARTIDA/acciones/atacar" \
  -H 'Content-Type: application/json' \
  -d '{"jugadorId":"UUID-JUGADOR-CON-TURNO","movimientoId":"UUID-MOVIMIENTO"}'
```

Cambiar el Pokémon activo:

```bash
curl -X POST "$BASE_URL/api/partidas/UUID-PARTIDA/acciones/cambiar-pokemon" \
  -H 'Content-Type: application/json' \
  -d '{"jugadorId":"UUID-JUGADOR-CON-TURNO","pokemonEquipoId":"UUID-POKEMON"}'
```

Un ataque puede fallar según la precisión. Si acierta, resta vida sin permitir
valores negativos. Cuando un Pokémon llega a cero se marca como debilitado y
se activa automáticamente el siguiente Pokémon disponible de su equipo.

Para rendirse no es necesario tener el turno:

```bash
curl -X POST "$BASE_URL/api/partidas/UUID-PARTIDA/acciones/rendirse" \
  -H 'Content-Type: application/json' \
  -d '{"jugadorId":"UUID-JUGADOR"}'
```

### 5. Finalizar y consultar historial — **Fase 2**

La partida termina cuando un participante no tiene Pokémon disponibles o se
rinde. Su estado pasa a `FINALIZADA`, `turnoDe` queda en `null` y se registra
el ganador y la fecha de finalización.

| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/api/partidas/{partidaId}/turnos` | Lista ataques, cambios y rendiciones por número de turno. |
| `GET` | `/api/partidas/{partidaId}/movimientos` | Lista ataques, daño, vida restante y resultado. |
| `GET` | `/api/partidas/{partidaId}/resultado` | Devuelve estado final, ganador y fecha de cierre. |

```bash
curl "$BASE_URL/api/partidas/UUID-PARTIDA/turnos"
curl "$BASE_URL/api/partidas/UUID-PARTIDA/movimientos"
curl "$BASE_URL/api/partidas/UUID-PARTIDA/resultado"
```

La API debe rechazar cambios hacia un Pokémon debilitado, movimientos de otro
Pokémon, acciones tras finalizar y actualizaciones simultáneas. Estos casos se
responden como `400` o `409`, según corresponda.

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
