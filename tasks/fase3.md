# Fase 3 — Registro, inicio de sesión y JWT en PokéBattle

## Objetivo

Actualizar el proyecto construido con `init.md` y `fase2.md` para incorporar autenticación de jugadores mediante usuario, contraseña y JSON Web Token (JWT).

Al finalizar esta fase, la API deberá permitir:

- registrar un jugador con usuario y contraseña;
- almacenar la contraseña de forma segura con BCrypt;
- devolver un JWT inmediatamente después del registro;
- iniciar sesión y obtener un nuevo JWT;
- identificar al jugador autenticado mediante el `subject` del token;
- proteger únicamente la creación de equipos;
- impedir que un jugador autenticado cree equipos para otro jugador;
- conservar públicas las demás rutas durante esta versión.

> Este archivo es la continuación de `init.md` y `fase2.md`. Se debe actualizar el proyecto existente, no crear un proyecto nuevo. El paquete base continúa siendo `sv.edu.udb.pokebattle`.

## 1. Flujo que se implementará

1. El jugador envía `usuario` y `password` a `/api/auth/registro`.
2. La API valida que el usuario no exista.
3. La contraseña se transforma en un hash BCrypt; nunca se almacena en texto plano.
4. Se crea el registro de `Jugador`.
5. La API genera un JWT cuyo `subject` es el UUID del jugador.
6. El cliente conserva el token.
7. Para crear un equipo, el cliente envía `Authorization: Bearer TOKEN`.
8. La API valida la firma, vigencia y propietario del token.
9. Las demás rutas permanecen públicas en esta fase.

## 2. Antes de iniciar

Abrir una terminal en la raíz del repositorio y verificar la fase anterior:

```bash
mvn clean verify
mvn spring-boot:run
```

Detener la aplicación y crear una rama de trabajo:

```bash
git switch -c feature/fase-3-jwt
```

## 3. Archivos que se agregarán o modificarán

### Archivos nuevos

```text
src/main/java/sv/edu/udb/pokebattle/
├── config/
│   └── SecurityConfig.java
├── controller/
│   └── AuthController.java
├── dto/
│   ├── AuthResponse.java
│   ├── LoginRequest.java
│   └── RegistroRequest.java
├── exception/
│   ├── AccesoDenegadoException.java
│   └── CredencialesInvalidasException.java
└── service/
    └── AuthService.java
```

### Archivos que se modificarán

```text
pom.xml
src/main/resources/application.properties
src/main/java/sv/edu/udb/pokebattle/model/Jugador.java
src/main/java/sv/edu/udb/pokebattle/repository/JugadorRepository.java
src/main/java/sv/edu/udb/pokebattle/controller/JugadorController.java
src/main/java/sv/edu/udb/pokebattle/controller/EquipoController.java
src/main/java/sv/edu/udb/pokebattle/service/JugadorService.java
src/main/java/sv/edu/udb/pokebattle/exception/ApiExceptionHandler.java
```

## 4. Agregar Spring Security y JWT

Abrir `pom.xml` y agregar las dependencias siguientes dentro de `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

No se debe agregar una versión manual, porque el proyecto utiliza el manejo de dependencias de Spring Boot.

Descargar las nuevas dependencias:

```bash
mvn dependency:resolve
```

## 5. Configurar las propiedades del JWT

Agregar al final de `src/main/resources/application.properties`:

```properties
# Configuración de JWT para el laboratorio
security.jwt.issuer=pokebattle-api
security.jwt.expiration-minutes=120
security.jwt.secret=${JWT_SECRET:01234567890123456789012345678901}
```

El valor predeterminado permite ejecutar el laboratorio localmente. No debe utilizarse en producción.

Para definir una clave diferente en macOS o Linux:

```bash
export JWT_SECRET="cambiar-por-una-clave-segura-de-al-menos-32-caracteres"
mvn spring-boot:run
```

En PowerShell:

```powershell
$env:JWT_SECRET="cambiar-por-una-clave-segura-de-al-menos-32-caracteres"
mvn spring-boot:run
```

## 6. Actualizar el modelo `Jugador`

Abrir `model/Jugador.java` y agregar los campos siguientes dentro de la clase:

```java
@Column(nullable = false, unique = true, length = 40)
private String usuario;

@Column(nullable = false, length = 60)
private String passwordHash;
```

El campo `passwordHash` almacenará el resultado de BCrypt. No se debe crear un campo llamado `password` en la entidad ni devolver el hash en un DTO.

En esta versión, `nombre` conservará el nombre visible del jugador y tendrá inicialmente el mismo valor que `usuario`.

> H2 está configurado con `create-drop`. Al reiniciar la aplicación, las tablas se crearán nuevamente con las columnas nuevas y se perderán los datos anteriores.

## 7. Actualizar `JugadorRepository`

Reemplazar el contenido de `repository/JugadorRepository.java`:

```java
package sv.edu.udb.pokebattle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.pokebattle.model.Jugador;

import java.util.Optional;
import java.util.UUID;

public interface JugadorRepository extends JpaRepository<Jugador, UUID> {
    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByUsuarioIgnoreCase(String usuario);

    Optional<Jugador> findByUsuarioIgnoreCase(String usuario);
}
```

## 8. Crear los DTO de autenticación

Crear `dto/RegistroRequest.java`:

```java
package sv.edu.udb.pokebattle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank
        @Size(min = 4, max = 40)
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "solo puede contener letras, números, punto, guion y guion bajo")
        String usuario,

        @NotBlank
        @Size(min = 8, max = 72)
        String password) {
}
```

Crear `dto/LoginRequest.java`:

```java
package sv.edu.udb.pokebattle.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String usuario,
        @NotBlank String password) {
}
```

Crear `dto/AuthResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        JugadorResponse jugador) {
}
```

`expiresIn` se expresará en segundos y `tokenType` tendrá el valor `Bearer`.

## 9. Crear las excepciones de autenticación

Crear `exception/CredencialesInvalidasException.java`:

```java
package sv.edu.udb.pokebattle.exception;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
```

Crear `exception/AccesoDenegadoException.java`:

```java
package sv.edu.udb.pokebattle.exception;

public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
```

## 10. Crear la configuración de seguridad

Crear `src/main/java/sv/edu/udb/pokebattle/config/SecurityConfig.java`:

```java
package sv.edu.udb.pokebattle.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey jwtSecretKey(@Value("${security.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "security.jwt.secret debe contener al menos 32 bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(
            SecretKey secretKey,
            @Value("${security.jwt.issuer}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/jugadores/*/equipos")
                        .authenticated()
                        .anyRequest()
                        .permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()))
                .build();
    }
}
```

Esta configuración aplica las siguientes reglas:

- no crea sesiones en el servidor;
- valida el JWT en cada solicitud protegida;
- exige autenticación solamente para `POST /api/jugadores/{jugadorId}/equipos`;
- mantiene públicas las rutas de registro, login, catálogo, partidas, combate e historial;
- devuelve HTTP `401 Unauthorized` cuando falta el token o su firma/vigencia no es válida.

## 11. Crear `AuthService`

Crear `src/main/java/sv/edu/udb/pokebattle/service/AuthService.java`:

```java
package sv.edu.udb.pokebattle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.pokebattle.dto.AuthResponse;
import sv.edu.udb.pokebattle.dto.JugadorResponse;
import sv.edu.udb.pokebattle.dto.LoginRequest;
import sv.edu.udb.pokebattle.dto.RegistroRequest;
import sv.edu.udb.pokebattle.exception.ConflictoException;
import sv.edu.udb.pokebattle.exception.CredencialesInvalidasException;
import sv.edu.udb.pokebattle.model.Jugador;
import sv.edu.udb.pokebattle.repository.JugadorRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final JugadorRepository jugadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Value("${security.jwt.issuer}")
    private String issuer;

    @Value("${security.jwt.expiration-minutes}")
    private long expirationMinutes;

    public AuthResponse registrar(RegistroRequest dto) {
        String usuario = normalizar(dto.usuario());

        if (jugadorRepository.existsByUsuarioIgnoreCase(usuario)) {
            throw new ConflictoException("El usuario ya está registrado");
        }

        Jugador jugador = new Jugador();
        jugador.setUsuario(usuario);
        jugador.setNombre(usuario);
        jugador.setPasswordHash(passwordEncoder.encode(dto.password()));

        Jugador guardado = jugadorRepository.save(jugador);
        return crearRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest dto) {
        Jugador jugador = jugadorRepository
                .findByUsuarioIgnoreCase(normalizar(dto.usuario()))
                .orElseThrow(() -> new CredencialesInvalidasException(
                        "Usuario o contraseña incorrectos"));

        if (!passwordEncoder.matches(
                dto.password(),
                jugador.getPasswordHash())) {
            throw new CredencialesInvalidasException(
                    "Usuario o contraseña incorrectos");
        }

        return crearRespuesta(jugador);
    }

    private AuthResponse crearRespuesta(Jugador jugador) {
        Instant emitidoEn = Instant.now();
        Instant expiraEn = emitidoEn.plus(
                expirationMinutes,
                ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(emitidoEn)
                .expiresAt(expiraEn)
                .subject(jugador.getId().toString())
                .claim("usuario", jugador.getUsuario())
                .build();

        JwsHeader encabezado = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(encabezado, claims))
                .getTokenValue();

        return new AuthResponse(
                token,
                "Bearer",
                expirationMinutes * 60,
                JugadorResponse.desde(jugador));
    }

    private String normalizar(String usuario) {
        return usuario.trim().toLowerCase(Locale.ROOT);
    }
}
```

El JWT no contiene la contraseña ni su hash. El `subject` contiene el UUID que la API utilizará para identificar al jugador autenticado.

## 12. Crear `AuthController`

Crear `src/main/java/sv/edu/udb/pokebattle/controller/AuthController.java`:

```java
package sv.edu.udb.pokebattle.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.pokebattle.dto.AuthResponse;
import sv.edu.udb.pokebattle.dto.LoginRequest;
import sv.edu.udb.pokebattle.dto.RegistroRequest;
import sv.edu.udb.pokebattle.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registrar(
            @Valid @RequestBody RegistroRequest dto) {
        return service.registrar(dto);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest dto) {
        return service.login(dto);
    }
}
```

## 13. Retirar el registro anterior de jugadores

En `init.md`, el registro se realizaba con `POST /api/jugadores` y no solicitaba contraseña. Esa ruta debe retirarse para evitar jugadores sin credenciales.

Reemplazar `controller/JugadorController.java`:

```java
package sv.edu.udb.pokebattle.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.pokebattle.dto.JugadorResponse;
import sv.edu.udb.pokebattle.service.JugadorService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jugadores")
@RequiredArgsConstructor
public class JugadorController {
    private final JugadorService service;

    @GetMapping
    public List<JugadorResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public JugadorResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }
}
```

Abrir `service/JugadorService.java` y eliminar:

- el método `crear(CrearJugadorRequest dto)`;
- el import de `CrearJugadorRequest`;
- el import de `ConflictoException`.

Los métodos `listar`, `obtener` y `buscarEntidad` deben conservarse.

El archivo `CrearJugadorRequest.java` puede eliminarse porque deja de utilizarse.

## 14. Verificar que el token pertenece al jugador

Spring Security comprueba que el token sea válido. Además, la aplicación debe verificar que el UUID de la ruta sea igual al `subject` del JWT.

Abrir `controller/EquipoController.java` y agregar estos imports:

```java
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import sv.edu.udb.pokebattle.exception.AccesoDenegadoException;
```

Reemplazar únicamente el método `crear` por el siguiente:

```java
@PostMapping("/api/jugadores/{jugadorId}/equipos")
@ResponseStatus(HttpStatus.CREATED)
public EquipoResponse crear(
        @PathVariable UUID jugadorId,
        @Valid @RequestBody CrearEquipoRequest dto,
        @AuthenticationPrincipal Jwt jwt) {
    UUID jugadorAutenticado = UUID.fromString(jwt.getSubject());

    if (!jugadorAutenticado.equals(jugadorId)) {
        throw new AccesoDenegadoException(
                "No puedes crear equipos para otro jugador");
    }

    return service.crear(jugadorId, dto);
}
```

No se deben modificar en esta fase las rutas para listar equipos, consultar un equipo, agregar Pokémon o cambiar Pokémon.

## 15. Actualizar el manejador de excepciones

Abrir `exception/ApiExceptionHandler.java` y agregar estos métodos dentro de la clase:

```java
@ExceptionHandler(CredencialesInvalidasException.class)
@ResponseStatus(HttpStatus.UNAUTHORIZED)
ErrorResponse credenciales(CredencialesInvalidasException ex) {
    return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
}

@ExceptionHandler(AccesoDenegadoException.class)
@ResponseStatus(HttpStatus.FORBIDDEN)
ErrorResponse accesoDenegado(AccesoDenegadoException ex) {
    return error(HttpStatus.FORBIDDEN, ex.getMessage());
}
```

No es necesario agregar imports para estas dos excepciones porque `ApiExceptionHandler` se encuentra en el mismo paquete.

## 16. Compilar y ejecutar

Compilar el proyecto:

```bash
mvn clean verify
```

Iniciar la API:

```bash
mvn spring-boot:run
```

La API estará disponible en:

```text
http://localhost:8080
```

## 17. Rutas de autenticación

| Método | Ruta | Autenticación | Función |
|---|---|---|---|
| `POST` | `/api/auth/registro` | Pública | Crear jugador y devolver JWT |
| `POST` | `/api/auth/login` | Pública | Validar credenciales y devolver JWT |
| `GET` | `/api/jugadores` | Pública | Listar jugadores |
| `GET` | `/api/jugadores/{id}` | Pública | Consultar un jugador |
| `POST` | `/api/jugadores/{jugadorId}/equipos` | Bearer JWT | Crear un equipo propio |

Todas las demás rutas permanecen públicas temporalmente.

## 18. Probar el registro en Postman

Crear una solicitud:

```http
POST http://localhost:8080/api/auth/registro
Content-Type: application/json

{
  "usuario": "ash",
  "password": "Pikachu123"
}
```

Respuesta esperada con HTTP `201 Created`:

```json
{
  "accessToken": "eyJraWQiOiJ...",
  "tokenType": "Bearer",
  "expiresIn": 7200,
  "jugador": {
    "id": "UUID-DEL-JUGADOR",
    "nombre": "ash"
  }
}
```

Guardar en variables de Postman:

```text
baseUrl=http://localhost:8080
token=VALOR-DE-accessToken
jugadorId=VALOR-DE-jugador.id
```

El token no debe incluir manualmente la palabra `Bearer` en la variable.

### Script opcional de Postman

En la pestaña **Tests** o **Post-response** de la solicitud de registro se puede utilizar:

```javascript
const respuesta = pm.response.json();
pm.environment.set("token", respuesta.accessToken);
pm.environment.set("jugadorId", respuesta.jugador.id);
```

## 19. Probar el inicio de sesión

Crear una solicitud:

```http
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "usuario": "ash",
  "password": "Pikachu123"
}
```

La respuesta debe contener un JWT nuevo y los datos del mismo jugador.

Para probar credenciales inválidas:

```http
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "usuario": "ash",
  "password": "incorrecta"
}
```

La API debe responder HTTP `401 Unauthorized` sin indicar si falló el usuario o la contraseña.

## 20. Crear un equipo con JWT

Crear una solicitud en Postman:

```http
POST {{baseUrl}}/api/jugadores/{{jugadorId}}/equipos
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "nombre": "Equipo Kanto"
}
```

La API debe responder HTTP `201 Created`.

En la pestaña **Authorization** de Postman también puede seleccionarse:

```text
Type: Bearer Token
Token: {{token}}
```

No se debe escribir `Bearer Bearer TOKEN`.

## 21. Pruebas negativas obligatorias

### Crear equipo sin token

Ejecutar la solicitud anterior sin el encabezado `Authorization`.

Resultado esperado:

```text
HTTP 401 Unauthorized
```

### Crear equipo con token inválido

```http
Authorization: Bearer token-invalido
```

Resultado esperado:

```text
HTTP 401 Unauthorized
```

### Crear equipo para otro jugador

1. Registrar un segundo jugador.
2. Conservar el token del primer jugador.
3. Enviar el UUID del segundo jugador en la ruta.

```http
POST {{baseUrl}}/api/jugadores/UUID-DEL-SEGUNDO-JUGADOR/equipos
Authorization: Bearer {{tokenDelPrimerJugador}}
Content-Type: application/json

{
  "nombre": "Equipo no permitido"
}
```

Resultado esperado:

```text
HTTP 403 Forbidden
```

### Registrar un usuario repetido

Repetir el registro de `ash`.

Resultado esperado:

```text
HTTP 409 Conflict
```

## 22. Verificar el hash en H2

Abrir:

```text
http://localhost:8080/h2-console
```

Usar:

```text
JDBC URL: jdbc:h2:mem:pokebattle
User Name: sa
Password: dejar vacío
```

Ejecutar:

```sql
SELECT ID, NOMBRE, USUARIO, PASSWORD_HASH
FROM JUGADOR;
```

`PASSWORD_HASH` debe comenzar normalmente con `$2a$`, `$2b$` o `$2y$`. La contraseña `Pikachu123` no debe aparecer almacenada en texto plano.

## 23. Comprobar qué rutas están protegidas

Sin enviar un token, estas rutas deben continuar funcionando:

```http
GET /api/jugadores
GET /api/catalogo/pokemon?page=0
GET /api/equipos/{equipoId}
POST /api/equipos/{equipoId}/pokemon
POST /api/partidas
GET /api/partidas/{partidaId}/estado
POST /api/partidas/{partidaId}/acciones/atacar
```

Solamente esta ruta exige JWT:

```http
POST /api/jugadores/{jugadorId}/equipos
```

Esta limitación es intencional para la primera versión de autenticación. En una fase posterior se podrán proteger las acciones de equipos y partidas.

## 24. Errores comunes

### Todas las rutas responden `401`

Revisar el orden de las reglas en `SecurityConfig`. La regla específica de creación de equipos debe aparecer antes de `.anyRequest().permitAll()`.

### La aplicación no inicia por la clave JWT

La clave debe contener al menos 32 bytes para HS256. Revisar `JWT_SECRET` y evitar caracteres que el terminal interprete de forma especial.

### Se recibe `401` aun enviando el token

Comprobar que:

- el encabezado se llama `Authorization`;
- el valor comienza exactamente con `Bearer `;
- no se agregó dos veces la palabra `Bearer`;
- el token corresponde a la ejecución actual de la aplicación;
- el token no está vencido;
- `JWT_SECRET` no cambió después de emitirlo.

### Se recibe `403` al crear el equipo

El token es válido, pero su `subject` no coincide con el `jugadorId` de la ruta. Utilizar el UUID incluido en `jugador.id` al registrarse o iniciar sesión.

### Error por columnas obligatorias al crear jugadores

Confirmar que se retiró `POST /api/jugadores`. Todos los jugadores nuevos deben crearse mediante `/api/auth/registro` para que `usuario` y `passwordHash` tengan valor.

## 25. Criterios de finalización

La fase está completa cuando se demuestra desde Postman que:

1. un jugador puede registrarse con usuario y contraseña;
2. la respuesta del registro contiene un JWT y el UUID del jugador;
3. la contraseña se almacena como hash BCrypt;
4. el jugador puede iniciar sesión y obtener un token nuevo;
5. las credenciales incorrectas devuelven HTTP `401`;
6. crear un equipo sin token devuelve HTTP `401`;
7. crear un equipo con el token propio devuelve HTTP `201`;
8. intentar crear un equipo para otro jugador devuelve HTTP `403`;
9. las demás rutas continúan funcionando sin token;
10. el proyecto supera `mvn clean verify`.

## 26. Guardar los cambios en Git

Comprobar los archivos modificados:

```bash
git status
```

Ejecutar la validación final:

```bash
mvn clean verify
```

Guardar los cambios:

```bash
git add .
git commit -m "feat: agregar registro login y autenticacion JWT"
```

## Resultado esperado

PokéBattle conserva las funciones de catálogo, equipos, partidas y combate creadas en las fases anteriores. La API incorpora ahora identidad persistida, autenticación sin sesión mediante JWT y autorización por propietario para la creación de equipos, sin exponer ni almacenar contraseñas en texto plano.
