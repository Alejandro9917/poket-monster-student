# Documentación técnica — PokéBattle

PokéBattle es un proyecto académico para demostrar programación orientada a objetos mediante una API de partidas Pokémon y una interfaz web. Está formado por tres piezas:

```text
Navegador ──> Next.js (puerto 3000) ──> Spring Boot API (puerto 8080) ──> H2
                                              │
                                              └──────────────> PokéAPI pública
```

La API conserva los jugadores, equipos, partidas y el estado de combate. Los datos de catálogo (Pokémon y movimientos) se consultan desde PokéAPI y se copian al equipo cuando el usuario elige un Pokémon.

## Puesta en marcha y configuración

Desde la raíz del repositorio se levantan ambos servicios con Docker:

```bash
docker compose -f docker/compose.yml up --build -d
```

- Frontend: `http://localhost:3000`
- API: `http://localhost:8080`
- Consola H2: `http://localhost:8080/h2-console`

Los archivos principales para modificar la configuración son:

| Necesidad | Archivo | Cambio habitual |
| --- | --- | --- |
| Puerto, H2, PokéAPI y JWT | `src/main/resources/application.properties` | URL de BD, secreto JWT o URL externa |
| Servicios y puertos Docker | `docker/compose.yml` | Puertos publicados o variables de entorno |
| Imagen del API | `docker/Dockerfile` | JDK, construcción o arranque de Spring |
| Imagen del frontend | `frontend/Dockerfile` | URL pública del API en tiempo de build |
| URL que usa el navegador | `frontend/.env.example` o `NEXT_PUBLIC_API_URL` | Host del backend |

Para desarrollo local se necesitan Java 21 y Node.js. El backend se ejecuta con Maven y el frontend con `npm run dev` dentro de `frontend/`. En producción el valor de `NEXT_PUBLIC_API_URL` se incrusta al construir Next.js, por lo que debe configurarse antes de crear la imagen.

## Convenciones de la API

La base de todas las rutas es `http://localhost:8080/api`. Las peticiones y respuestas usan JSON. Los identificadores propios del proyecto son UUID; `pokemonApiId` y `movimientosApiId` son identificadores numéricos de PokéAPI.

Errores de validación y de negocio se devuelven mediante el manejador central en `exception/ApiExceptionHandler.java`. Entre los estados relevantes están `400` (datos inválidos), `401` (token o credenciales inválidos), `403` (sin permiso), `404` (recurso inexistente) y `409` (regla de negocio/conflicto).

## Endpoints

### Autenticación

| Método y ruta | Cuerpo | Resultado |
| --- | --- | --- |
| `POST /api/auth/registro` | `{"usuario":"misty","password":"secreto123"}` | Crea un jugador y devuelve un JWT. `usuario` tiene 4–40 caracteres alfanuméricos, punto, guion o guion bajo; contraseña 8–72 caracteres. |
| `POST /api/auth/login` | `{"usuario":"misty","password":"secreto123"}` | Comprueba credenciales y devuelve un JWT nuevo. |

Ambas rutas responden con una estructura como esta:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 7200,
  "jugador": {
    "id": "uuid-del-jugador",
    "nombre": "misty",
    "usuario": "misty"
  }
}
```

### Jugadores y equipos

| Método y ruta | Cuerpo / parámetros | Comportamiento |
| --- | --- | --- |
| `GET /api/jugadores` | — | Lista los jugadores registrados. |
| `GET /api/jugadores/{jugadorId}` | UUID en ruta | Devuelve un jugador. |
| `POST /api/jugadores/{jugadorId}/equipos` | `{"nombre":"Equipo agua"}` + `Authorization: Bearer <JWT>` | Crea un equipo para el dueño del token. Es la ruta de equipos protegida actualmente. |
| `GET /api/jugadores/{jugadorId}/equipos` | UUID en ruta | Lista equipos y sus Pokémon. |
| `GET /api/equipos/{equipoId}` | UUID en ruta | Obtiene el detalle de un equipo. |
| `POST /api/equipos/{equipoId}/pokemon` | Ver ejemplo inferior | Agrega un Pokémon al equipo (máximo 3). |
| `PUT /api/equipos/{equipoId}/pokemon/{pokemonId}` | Igual que agregar | Sustituye datos y movimientos de un Pokémon existente. |

Ejemplo para agregar un Pokémon. Debe seleccionar entre uno y cuatro movimientos distintos que realmente estén disponibles para ese Pokémon:

```json
{
  "pokemonApiId": 6,
  "movimientosApiId": [53, 126, 257]
}
```

Al guardarlo, el backend verifica la relación Pokémon–movimiento contra PokéAPI y persiste una copia del nombre, sprite, estadísticas y movimientos. Por ello el combate no necesita volver a consultar PokéAPI.

> Nota de seguridad: la creación de equipos está protegida y valida que el `sub` del token sea igual a `{jugadorId}`. Las demás rutas de equipos, partidas y combate están públicas en la configuración actual. Si se incorporan usuarios reales, se debe extender la autorización en `config/SecurityConfig.java` y validar que cada acción pertenezca al jugador autenticado.

### Catálogo de Pokémon

| Método y ruta | Parámetros | Comportamiento |
| --- | --- | --- |
| `GET /api/catalogo/pokemon` | `page` (inicia en 0) | Página de 50 Pokémon de PokéAPI. |
| `GET /api/catalogo/pokemon/buscar` | `nombre` | Filtra por nombre y devuelve hasta 20 coincidencias. |
| `GET /api/catalogo/pokemon/{idONombre}` | Id o nombre | Devuelve nombre, sprite, tipos y estadísticas base. |
| `GET /api/catalogo/pokemon/{idONombre}/movimientos` | `page` y `size` (1–50, por defecto 20) | Lista los movimientos que PokéAPI declara disponibles para el Pokémon. |

Las respuestas paginadas tienen el formato `contenido`, `page`, `size`, `totalElementos` y `totalPaginas`.

### Ciclo de una partida

Una partida pasa por estos estados: `ESPERANDO_JUGADOR` → `ESPERANDO_CONFIRMACION` → `EN_CURSO` → `FINALIZADA`.

| Método y ruta | Cuerpo / parámetros | Comportamiento |
| --- | --- | --- |
| `POST /api/partidas` | `{"jugadorId":"uuid","equipoId":"uuid"}` | Crea una sala, agrega al primer participante y genera un código de ocho caracteres. |
| `GET /api/partidas/codigo/{codigo}` | Código de sala | Busca una partida para poder unirse a ella. |
| `POST /api/partidas/{partidaId}/unirse` | `{"jugadorId":"uuid","equipoId":"uuid"}` | Agrega al segundo jugador. La sala pasa a esperar confirmaciones. |
| `POST /api/partidas/{partidaId}/jugadores/{jugadorId}/confirmar` | — | Confirma que ese participante está listo. |
| `POST /api/partidas/{partidaId}/iniciar` | — | Inicia cuando hay dos confirmaciones y cada equipo tiene exactamente tres Pokémon. El primer turno es del equipo cuyo Pokémon más veloz tenga mayor velocidad base. |
| `GET /api/partidas/{partidaId}` | UUID en ruta | Muestra código, estado, turno y participantes. |

La creación y unión verifican que el equipo indicado pertenezca al jugador. La respuesta de partida incluye `id`, `codigoSala`, `estado`, `turnoDeJugadorId` y participantes con su confirmación.

### Combate

Estas rutas se habilitan una vez que la partida está en `EN_CURSO`.

| Método y ruta | Cuerpo | Comportamiento |
| --- | --- | --- |
| `GET /api/partidas/{partidaId}/estado` | — | Estado completo: turno, ganador, participantes y sus Pokémon con vida, activo/debilitado y movimientos. |
| `GET /api/partidas/{partidaId}/turno` | — | Jugador al que corresponde actuar y número de turno. |
| `POST /api/partidas/{partidaId}/acciones/atacar` | `{"jugadorId":"uuid","movimientoId":"uuid"}` | Usa un movimiento del Pokémon activo, descuenta vida y alterna turno. Si no hay relevo posible, finaliza la partida. |
| `POST /api/partidas/{partidaId}/acciones/cambiar-pokemon` | `{"jugadorId":"uuid","pokemonEquipoId":"uuid"}` | Cambia el Pokémon activo por uno no debilitado y entrega el turno al rival. |
| `POST /api/partidas/{partidaId}/acciones/rendirse` | `{"jugadorId":"uuid"}` | Finaliza la partida y declara ganador al rival. |

El daño es una fórmula académica simple basada en ataque base, poder del movimiento y defensa base. No intenta reproducir todas las reglas oficiales de Pokémon.

## Integración con PokéAPI

La fuente externa es [PokéAPI](https://pokeapi.co/) v2. La URL base configurable es `pokeapi.base-url`, actualmente `https://pokeapi.co/api/v2`.

La integración se divide en:

1. `config/RestClientConfig.java` crea un `RestClient` con esa URL base.
2. `client/PokeApiClient.java` implementa las solicitudes a `/pokemon` y `/move` y define los DTO que representan solo los campos usados.
3. `service/CatalogoService.java` transforma las respuestas externas en DTO del proyecto para catálogo, búsqueda, detalle y movimientos.
4. `service/EquipoService.java` vuelve a consultar el Pokémon y cada movimiento al guardar un equipo. Así confirma que los movimientos seleccionados pertenecen al Pokémon y captura sus estadísticas para el juego.

Si la estructura de PokéAPI cambia o se quiere cambiar de proveedor, el punto de adaptación principal es `PokeApiClient`. Si solo cambia la URL, basta con `pokeapi.base-url` en `application.properties`. El servicio de búsqueda actual obtiene primero el listado completo remoto y filtra en memoria; para tráfico alto conviene reemplazarlo por caché o un índice local.

## Base de datos y modelo persistente

La aplicación usa H2 en memoria:

```properties
spring.datasource.url=jdbc:h2:mem:pokebattle;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.jpa.hibernate.ddl-auto=create-drop
```

Esto significa que la base se crea al arrancar y se destruye al detener el proceso o contenedor. Es apropiado para el ejercicio, pero no para conservar datos en producción. La consola se habilita en `/h2-console`.

Las entidades están en `src/main/java/sv/edu/udb/pokebattle/model/` y los repositorios JPA equivalentes en `repository/`:

```text
Jugador 1 ─── N Equipo 1 ─── N PokemonEquipo 1 ─── N MovimientoSeleccionado
                    │
                    └── se referencia desde ParticipantePartida

Partida 1 ─── N ParticipantePartida
ParticipantePartida 1 ─── N PokemonBatalla
```

- `Jugador` almacena identidad, usuario único y hash BCrypt; nunca la contraseña en texto plano.
- `Equipo` pertenece a un jugador y puede contener hasta tres `PokemonEquipo` por regla de servicio.
- `PokemonEquipo` es la instantánea del catálogo usada por el juego: IDs externos, stats, sprite y movimientos seleccionados.
- `Partida` conserva código, estado, turno, ganador y usa `@Version` para control optimista de concurrencia.
- `ParticipantePartida` relaciona una partida con jugador y equipo; cada pareja partida/jugador es única.
- `PokemonBatalla` conserva vida, estado activo y debilitado para esa partida sin alterar el equipo original.

Para usar una base persistente, sustituya las propiedades `spring.datasource.*` por las de PostgreSQL/MySQL, agregue su driver al `pom.xml` y cambie `spring.jpa.hibernate.ddl-auto` a una estrategia segura, idealmente migraciones con Flyway o Liquibase. No cambie los modelos sin revisar las relaciones y las migraciones resultantes.

## JWT: creación y validación

La seguridad se configura en `config/SecurityConfig.java` y la autenticación en `service/AuthService.java`.

1. Registro guarda la contraseña con `BCryptPasswordEncoder`.
2. Registro o login crean un JWT HMAC-SHA256 (HS256).
3. El token contiene `iss` (`security.jwt.issuer`), `iat`, `exp`, `sub` (UUID del jugador) y el claim `usuario`.
4. El cliente lo envía como `Authorization: Bearer <accessToken>`.
5. Spring Security, mediante el recurso OAuth2, verifica firma, emisor y expiración antes de atender rutas protegidas.

Configuración relevante:

```properties
security.jwt.issuer=pokebattle-api
security.jwt.expiration-minutes=120
security.jwt.secret=${JWT_SECRET:01234567890123456789012345678901}
```

`security.jwt.secret` debe tener al menos 32 bytes. El valor por defecto existe para desarrollo; en un entorno compartido debe entregarse por la variable de entorno `JWT_SECRET` y nunca versionarse. Cambiar el secreto invalida inmediatamente los tokens existentes.

Actualmente `SecurityConfig` exige autenticación para `POST /api/jugadores/*/equipos`. Para proteger nuevas rutas, añada sus patrones a `authorizeHttpRequests` y use `@AuthenticationPrincipal Jwt jwt` cuando necesite comprobar el dueño del recurso. Mantener la API sin sesiones (`STATELESS`) permite que el backend escale sin estado de sesión en memoria.

## Frontend e integración con el API

El frontend es Next.js y está en `frontend/`:

| Archivo | Responsabilidad |
| --- | --- |
| `frontend/app/page.tsx` | Pantalla cliente, estados, llamadas HTTP y flujo de usuario. |
| `frontend/app/globals.css` | Componentes visuales reutilizables mediante capas y utilidades Tailwind. |
| `frontend/tailwind.config.ts` | Paleta (`forest`, `panel`, `line`, `gold`, `sky`) y archivos que Tailwind analiza. |
| `frontend/.env.example` | Ejemplo de `NEXT_PUBLIC_API_URL`. |
| `frontend/Dockerfile` | Compilación y ejecución de Next.js. |

`page.tsx` define la constante de URL:

```ts
const A = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
```

Su función `api()` concentra `fetch`, JSON y la lectura de mensajes de error. Tras registro o login guarda `{ user, token }` en `localStorage` bajo `pokebattle-session-v3`. El token se añade hoy al crear equipos. El cierre de sesión limpia `localStorage`, `sessionStorage` y cookies del navegador antes de volver al inicio.

Flujo implementado en la interfaz:

1. Registrar o iniciar sesión.
2. Crear y seleccionar equipos.
3. Buscar Pokémon, consultar sus movimientos, seleccionar hasta cuatro y agregarlo al equipo.
4. Elegir un equipo completo, crear una sala o buscarla por código y unirse.

La API expone confirmación, inicio y combate, pero la pantalla actual no incluye todavía controles para confirmar/iniciar una partida ni el tablero de combate. Para incorporarlos, extienda el estado de `page.tsx` y consuma las rutas de las secciones «Ciclo de una partida» y «Combate»; no hace falta alterar el backend para ese primer paso.

El backend habilita CORS solo para `http://localhost:3000` en `config/WebConfig.java`. Si se publica el frontend en otro origen, agregue explícitamente su URL a `allowedOrigins`; no use comodines junto con credenciales/autenticación.

## Guía de modificación rápida

| Si se quiere cambiar… | Revisar primero |
| --- | --- |
| Una ruta, request o respuesta | Controller, DTO asociado y Service dentro de `src/main/java/sv/edu/udb/pokebattle/` |
| Una regla del juego | `service/PartidaService.java` o `service/CombateService.java` |
| Límite de Pokémon/movimientos | `service/EquipoService.java` y validaciones en `dto/GuardarPokemonRequest.java` |
| Datos que se guardan de PokéAPI | `client/PokeApiClient.java` y `service/EquipoService.java` |
| Esquema de BD | `model/`, `repository/` y una migración si la BD deja de ser H2 efímera |
| Duración, emisor o secreto JWT | `application.properties`, `SecurityConfig.java` y `AuthService.java` |
| Permisos de rutas | `config/SecurityConfig.java` |
| CORS | `config/WebConfig.java` |
| Interfaz, navegación o llamadas del navegador | `frontend/app/page.tsx` |
| Paleta, espaciados y componentes de la interfaz | `frontend/app/globals.css` y `frontend/tailwind.config.ts` |

Después de cambios de backend ejecute pruebas/compilación Maven; después de cambios del frontend ejecute `npm run build` dentro de `frontend/`. Para actualizar los contenedores, vuelva a ejecutar `docker compose -f docker/compose.yml up --build -d`.
