# Inicialización de la API PokéBattle

## 1. Requisitos

- Java 21.
- Maven 3.9 o Maven Wrapper.
- IntelliJ IDEA, VS Code o un editor compatible con Java.
- Postman.

Verificar las instalaciones:

```bash
java --version
mvn --version
```

## 2. Crear el proyecto

Crear una carpeta vacía y entrar en ella:

```bash
mkdir pokebattle-api
cd pokebattle-api
```

Crear el archivo `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
        <relativePath/>
    </parent>

    <groupId>sv.edu.udb</groupId>
    <artifactId>pokebattle-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>pokebattle-api</name>
    <description>API para un juego de combate inspirado en Pokémon</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

Crear la estructura de carpetas:

```bash
mkdir -p src/main/java/sv/edu/udb/pokebattle/{client,config,controller,dto,exception,model,repository,service}
mkdir -p src/main/resources
mkdir -p src/test/java/sv/edu/udb/pokebattle
```

## 3. Configuración

Crear `src/main/resources/application.properties`:

```properties
spring.application.name=pokebattle-api

spring.datasource.url=jdbc:h2:mem:pokebattle;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.open-in-view=false
spring.jpa.show-sql=true

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

pokeapi.base-url=https://pokeapi.co/api/v2
```

Crear `src/main/java/sv/edu/udb/pokebattle/PokeBattleApplication.java`:

```java
package sv.edu.udb.pokebattle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PokeBattleApplication {
    public static void main(String[] args) {
        SpringApplication.run(PokeBattleApplication.class, args);
    }
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/config/RestClientConfig.java`:

```java
package sv.edu.udb.pokebattle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    RestClient pokeApiRestClient(
            RestClient.Builder builder,
            @Value("${pokeapi.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
```

## 4. Modelos

Crear `src/main/java/sv/edu/udb/pokebattle/model/Jugador.java`:

```java
package sv.edu.udb.pokebattle.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Jugador {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String nombre;

    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/model/Equipo.java`:

```java
package sv.edu.udb.pokebattle.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Equipo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Jugador jugador;

    @OneToMany(mappedBy = "equipo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PokemonEquipo> pokemon = new ArrayList<>();
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/model/PokemonEquipo.java`:

```java
package sv.edu.udb.pokebattle.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PokemonEquipo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Equipo equipo;

    private Integer pokemonApiId;
    private String nombre;
    private String sprite;
    private Integer hpBase;
    private Integer ataqueBase;
    private Integer defensaBase;
    private Integer velocidadBase;

    @OneToMany(mappedBy = "pokemon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovimientoSeleccionado> movimientos = new ArrayList<>();
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/model/MovimientoSeleccionado.java`:

```java
package sv.edu.udb.pokebattle.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class MovimientoSeleccionado {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private PokemonEquipo pokemon;

    private Integer movimientoApiId;
    private String nombre;
    private Integer poder;
    private Integer precisionMovimiento;
    private String tipo;
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/model/EstadoPartida.java`:

```java
package sv.edu.udb.pokebattle.model;

public enum EstadoPartida {
    ESPERANDO_JUGADOR,
    ESPERANDO_CONFIRMACION,
    EN_CURSO,
    FINALIZADA
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/model/Partida.java`:

```java
package sv.edu.udb.pokebattle.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Partida {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 8)
    private String codigoSala;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPartida estado = EstadoPartida.ESPERANDO_JUGADOR;

    @ManyToOne(fetch = FetchType.LAZY)
    private Jugador turnoDe;

    @ManyToOne(fetch = FetchType.LAZY)
    private Jugador ganador;

    @Column(nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    private LocalDateTime iniciadaEn;
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/model/ParticipantePartida.java`:

```java
package sv.edu.udb.pokebattle.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(
        columnNames = {"partida_id", "jugador_id"}))
public class ParticipantePartida {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "partida_id")
    private Partida partida;

    @ManyToOne(optional = false)
    @JoinColumn(name = "jugador_id")
    private Jugador jugador;

    @ManyToOne(optional = false)
    private Equipo equipo;

    private boolean confirmado;
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/model/PokemonBatalla.java`:

```java
package sv.edu.udb.pokebattle.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PokemonBatalla {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private ParticipantePartida participante;

    @ManyToOne(optional = false)
    private PokemonEquipo pokemonEquipo;

    private Integer vidaActual;
    private boolean activo;
    private boolean debilitado;
}
```

## 5. Repositorios

Crear `src/main/java/sv/edu/udb/pokebattle/repository/JugadorRepository.java`:

```java
package sv.edu.udb.pokebattle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.pokebattle.model.Jugador;

import java.util.UUID;

public interface JugadorRepository extends JpaRepository<Jugador, UUID> {
    boolean existsByNombreIgnoreCase(String nombre);
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/repository/EquipoRepository.java`:

```java
package sv.edu.udb.pokebattle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.pokebattle.model.Equipo;

import java.util.List;
import java.util.UUID;

public interface EquipoRepository extends JpaRepository<Equipo, UUID> {
    List<Equipo> findByJugadorId(UUID jugadorId);
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/repository/PokemonEquipoRepository.java`:

```java
package sv.edu.udb.pokebattle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.pokebattle.model.PokemonEquipo;

import java.util.UUID;

public interface PokemonEquipoRepository extends JpaRepository<PokemonEquipo, UUID> {
    long countByEquipoId(UUID equipoId);
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/repository/PartidaRepository.java`:

```java
package sv.edu.udb.pokebattle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.pokebattle.model.Partida;

import java.util.Optional;
import java.util.UUID;

public interface PartidaRepository extends JpaRepository<Partida, UUID> {
    Optional<Partida> findByCodigoSalaIgnoreCase(String codigoSala);
    boolean existsByCodigoSalaIgnoreCase(String codigoSala);
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/repository/ParticipantePartidaRepository.java`:

```java
package sv.edu.udb.pokebattle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.pokebattle.model.ParticipantePartida;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipantePartidaRepository
        extends JpaRepository<ParticipantePartida, UUID> {
    List<ParticipantePartida> findByPartidaId(UUID partidaId);

    Optional<ParticipantePartida> findByPartidaIdAndJugadorId(
            UUID partidaId, UUID jugadorId);

    boolean existsByPartidaIdAndJugadorId(UUID partidaId, UUID jugadorId);
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/repository/PokemonBatallaRepository.java`:

```java
package sv.edu.udb.pokebattle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.pokebattle.model.PokemonBatalla;

import java.util.UUID;

public interface PokemonBatallaRepository extends JpaRepository<PokemonBatalla, UUID> {
}
```

## 6. DTO

Crear `src/main/java/sv/edu/udb/pokebattle/dto/CrearJugadorRequest.java`:

```java
package sv.edu.udb.pokebattle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearJugadorRequest(
        @NotBlank @Size(max = 40) String nombre) {
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/dto/JugadorResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import sv.edu.udb.pokebattle.model.Jugador;

import java.util.UUID;

public record JugadorResponse(UUID id, String nombre) {
    public static JugadorResponse desde(Jugador jugador) {
        return new JugadorResponse(jugador.getId(), jugador.getNombre());
    }
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/dto/CrearEquipoRequest.java`:

```java
package sv.edu.udb.pokebattle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearEquipoRequest(
        @NotBlank @Size(max = 50) String nombre) {
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/dto/GuardarPokemonRequest.java`:

```java
package sv.edu.udb.pokebattle.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GuardarPokemonRequest(
        @NotNull @Positive Integer pokemonApiId,
        @NotEmpty @Size(max = 4) List<@Positive Integer> movimientosApiId) {
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/dto/MovimientoResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import sv.edu.udb.pokebattle.model.MovimientoSeleccionado;

import java.util.UUID;

public record MovimientoResponse(
        UUID id,
        Integer movimientoApiId,
        String nombre,
        Integer poder,
        Integer precision,
        String tipo) {
    public static MovimientoResponse desde(MovimientoSeleccionado movimiento) {
        return new MovimientoResponse(
                movimiento.getId(),
                movimiento.getMovimientoApiId(),
                movimiento.getNombre(),
                movimiento.getPoder(),
                movimiento.getPrecisionMovimiento(),
                movimiento.getTipo());
    }
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/dto/PokemonEquipoResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import sv.edu.udb.pokebattle.model.PokemonEquipo;

import java.util.List;
import java.util.UUID;

public record PokemonEquipoResponse(
        UUID id,
        Integer pokemonApiId,
        String nombre,
        String sprite,
        Integer hpBase,
        Integer ataqueBase,
        Integer defensaBase,
        Integer velocidadBase,
        List<MovimientoResponse> movimientos) {
    public static PokemonEquipoResponse desde(PokemonEquipo pokemon) {
        return new PokemonEquipoResponse(
                pokemon.getId(),
                pokemon.getPokemonApiId(),
                pokemon.getNombre(),
                pokemon.getSprite(),
                pokemon.getHpBase(),
                pokemon.getAtaqueBase(),
                pokemon.getDefensaBase(),
                pokemon.getVelocidadBase(),
                pokemon.getMovimientos().stream()
                        .map(MovimientoResponse::desde)
                        .toList());
    }
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/dto/EquipoResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import sv.edu.udb.pokebattle.model.Equipo;

import java.util.List;
import java.util.UUID;

public record EquipoResponse(
        UUID id,
        String nombre,
        UUID jugadorId,
        List<PokemonEquipoResponse> pokemon) {
    public static EquipoResponse desde(Equipo equipo) {
        return new EquipoResponse(
                equipo.getId(),
                equipo.getNombre(),
                equipo.getJugador().getId(),
                equipo.getPokemon().stream()
                        .map(PokemonEquipoResponse::desde)
                        .toList());
    }
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/dto/PokemonResumen.java`:

```java
package sv.edu.udb.pokebattle.dto;

public record PokemonResumen(int id, String nombre) {
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/dto/MovimientoResumen.java`:

```java
package sv.edu.udb.pokebattle.dto;

public record MovimientoResumen(
        int id,
        String nombre,
        Integer poder,
        Integer precision,
        String tipo) {
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/dto/PokemonDetalleResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import java.util.List;
import java.util.Map;

public record PokemonDetalleResponse(
        int id,
        String nombre,
        String sprite,
        Map<String, Integer> estadisticas,
        List<String> tipos) {
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/dto/PaginaResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import java.util.List;

public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas) {
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/dto/CrearPartidaRequest.java`:

```java
package sv.edu.udb.pokebattle.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CrearPartidaRequest(
        @NotNull UUID jugadorId,
        @NotNull UUID equipoId) {
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/dto/UnirsePartidaRequest.java`:

```java
package sv.edu.udb.pokebattle.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UnirsePartidaRequest(
        @NotNull UUID jugadorId,
        @NotNull UUID equipoId) {
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/dto/ParticipanteResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import java.util.UUID;

public record ParticipanteResponse(
        UUID jugadorId,
        UUID equipoId,
        boolean confirmado) {
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/dto/PartidaResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import sv.edu.udb.pokebattle.model.EstadoPartida;

import java.util.List;
import java.util.UUID;

public record PartidaResponse(
        UUID id,
        String codigoSala,
        EstadoPartida estado,
        UUID turnoDe,
        List<ParticipanteResponse> participantes) {
}
```

## 7. Cliente de PokéAPI

Crear `src/main/java/sv/edu/udb/pokebattle/client/PokeApiClient.java`:

```java
package sv.edu.udb.pokebattle.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PokeApiClient {
    private final RestClient pokeApiRestClient;

    public ListaResponse listar(int offset, int limit) {
        return pokeApiRestClient.get()
                .uri("/pokemon?offset={offset}&limit={limit}", offset, limit)
                .retrieve()
                .body(ListaResponse.class);
    }

    public PokemonResponse pokemon(String idONombre) {
        return pokeApiRestClient.get()
                .uri("/pokemon/{valor}", idONombre.toLowerCase())
                .retrieve()
                .body(PokemonResponse.class);
    }

    public MovimientoResponse movimiento(int id) {
        return pokeApiRestClient.get()
                .uri("/move/{id}", id)
                .retrieve()
                .body(MovimientoResponse.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecursoNombrado(String name, String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListaResponse(
            int count,
            String next,
            String previous,
            List<RecursoNombrado> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Estadistica(
            @JsonProperty("base_stat") int baseStat,
            RecursoNombrado stat) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tipo(RecursoNombrado type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sprite(
            @JsonProperty("front_default") String frontDefault) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovimientoPokemon(RecursoNombrado move) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PokemonResponse(
            int id,
            String name,
            Sprite sprites,
            List<Estadistica> stats,
            List<Tipo> types,
            List<MovimientoPokemon> moves) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovimientoResponse(
            int id,
            String name,
            Integer power,
            Integer accuracy,
            RecursoNombrado type) {
    }
}
```

## 8. Excepciones

Crear `src/main/java/sv/edu/udb/pokebattle/exception/RecursoNoEncontradoException.java`:

```java
package sv.edu.udb.pokebattle.exception;

public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/exception/ConflictoException.java`:

```java
package sv.edu.udb.pokebattle.exception;

public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/exception/ReglaNegocioException.java`:

```java
package sv.edu.udb.pokebattle.exception;

public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/exception/ErrorResponse.java`:

```java
package sv.edu.udb.pokebattle.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        int estado,
        String error,
        String mensaje,
        LocalDateTime fecha) {
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/exception/ApiExceptionHandler.java`:

```java
package sv.edu.udb.pokebattle.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse noEncontrado(RecursoNoEncontradoException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ErrorResponse conflicto(ConflictoException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse regla(ReglaNegocioException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse validacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .orElse("Solicitud inválida");
        return error(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(RestClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ErrorResponse apiExterna(RestClientException ex) {
        return error(HttpStatus.BAD_GATEWAY, "No fue posible consultar PokéAPI");
    }

    private ErrorResponse error(HttpStatus estado, String mensaje) {
        return new ErrorResponse(
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                LocalDateTime.now());
    }
}
```

## 9. Servicios

Crear `src/main/java/sv/edu/udb/pokebattle/service/JugadorService.java`:

```java
package sv.edu.udb.pokebattle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.pokebattle.dto.CrearJugadorRequest;
import sv.edu.udb.pokebattle.dto.JugadorResponse;
import sv.edu.udb.pokebattle.exception.ConflictoException;
import sv.edu.udb.pokebattle.exception.RecursoNoEncontradoException;
import sv.edu.udb.pokebattle.model.Jugador;
import sv.edu.udb.pokebattle.repository.JugadorRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class JugadorService {
    private final JugadorRepository repository;

    public JugadorResponse crear(CrearJugadorRequest dto) {
        String nombre = dto.nombre().trim();
        if (repository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("El nombre ya está registrado");
        }
        Jugador jugador = new Jugador();
        jugador.setNombre(nombre);
        return JugadorResponse.desde(repository.save(jugador));
    }

    @Transactional(readOnly = true)
    public List<JugadorResponse> listar() {
        return repository.findAll().stream()
                .map(JugadorResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public JugadorResponse obtener(UUID id) {
        return JugadorResponse.desde(buscarEntidad(id));
    }

    public Jugador buscarEntidad(UUID id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Jugador no encontrado"));
    }
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/service/CatalogoService.java`:

```java
package sv.edu.udb.pokebattle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sv.edu.udb.pokebattle.client.PokeApiClient;
import sv.edu.udb.pokebattle.dto.MovimientoResumen;
import sv.edu.udb.pokebattle.dto.PaginaResponse;
import sv.edu.udb.pokebattle.dto.PokemonDetalleResponse;
import sv.edu.udb.pokebattle.dto.PokemonResumen;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogoService {
    private static final int TAMANO_CATALOGO = 50;
    private final PokeApiClient client;

    public PaginaResponse<PokemonResumen> listar(int pagina) {
        var respuesta = client.listar(pagina * TAMANO_CATALOGO, TAMANO_CATALOGO);
        var contenido = respuesta.results().stream()
                .map(recurso -> new PokemonResumen(
                        extraerId(recurso.url()), recurso.name()))
                .toList();
        int totalPaginas = (int) Math.ceil(
                respuesta.count() / (double) TAMANO_CATALOGO);
        return new PaginaResponse<>(
                contenido,
                pagina,
                TAMANO_CATALOGO,
                respuesta.count(),
                totalPaginas);
    }

    public List<PokemonResumen> buscar(String nombre) {
        String filtro = nombre.trim().toLowerCase();
        int total = client.listar(0, 1).count();
        return client.listar(0, total).results().stream()
                .filter(recurso -> recurso.name().contains(filtro))
                .limit(20)
                .map(recurso -> new PokemonResumen(
                        extraerId(recurso.url()), recurso.name()))
                .toList();
    }

    public PokemonDetalleResponse detalle(String idONombre) {
        var pokemon = client.pokemon(idONombre);
        Map<String, Integer> estadisticas = pokemon.stats().stream()
                .collect(Collectors.toMap(
                        estadistica -> estadistica.stat().name(),
                        PokeApiClient.Estadistica::baseStat));
        List<String> tipos = pokemon.types().stream()
                .map(tipo -> tipo.type().name())
                .toList();
        return new PokemonDetalleResponse(
                pokemon.id(),
                pokemon.name(),
                pokemon.sprites().frontDefault(),
                estadisticas,
                tipos);
    }

    public PaginaResponse<MovimientoResumen> movimientos(
            String idONombre, int pagina, int tamano) {
        var movimientos = client.pokemon(idONombre).moves();
        int inicio = Math.min(pagina * tamano, movimientos.size());
        int fin = Math.min(inicio + tamano, movimientos.size());
        var contenido = movimientos.subList(inicio, fin).stream()
                .map(item -> client.movimiento(extraerId(item.move().url())))
                .map(movimiento -> new MovimientoResumen(
                        movimiento.id(),
                        movimiento.name(),
                        movimiento.power(),
                        movimiento.accuracy(),
                        movimiento.type().name()))
                .toList();
        int totalPaginas = (int) Math.ceil(movimientos.size() / (double) tamano);
        return new PaginaResponse<>(
                contenido,
                pagina,
                tamano,
                movimientos.size(),
                totalPaginas);
    }

    private int extraerId(String url) {
        String[] partes = url.replaceAll("/$", "").split("/");
        return Integer.parseInt(partes[partes.length - 1]);
    }
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/service/EquipoService.java`:

```java
package sv.edu.udb.pokebattle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.pokebattle.client.PokeApiClient;
import sv.edu.udb.pokebattle.dto.CrearEquipoRequest;
import sv.edu.udb.pokebattle.dto.EquipoResponse;
import sv.edu.udb.pokebattle.dto.GuardarPokemonRequest;
import sv.edu.udb.pokebattle.dto.PokemonEquipoResponse;
import sv.edu.udb.pokebattle.exception.ConflictoException;
import sv.edu.udb.pokebattle.exception.RecursoNoEncontradoException;
import sv.edu.udb.pokebattle.exception.ReglaNegocioException;
import sv.edu.udb.pokebattle.model.Equipo;
import sv.edu.udb.pokebattle.model.Jugador;
import sv.edu.udb.pokebattle.model.MovimientoSeleccionado;
import sv.edu.udb.pokebattle.model.PokemonEquipo;
import sv.edu.udb.pokebattle.repository.EquipoRepository;
import sv.edu.udb.pokebattle.repository.JugadorRepository;
import sv.edu.udb.pokebattle.repository.PokemonEquipoRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EquipoService {
    private final EquipoRepository equipoRepository;
    private final PokemonEquipoRepository pokemonRepository;
    private final JugadorRepository jugadorRepository;
    private final PokeApiClient pokeApi;

    public EquipoResponse crear(UUID jugadorId, CrearEquipoRequest dto) {
        Jugador jugador = jugadorRepository.findById(jugadorId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Jugador no encontrado"));
        Equipo equipo = new Equipo();
        equipo.setNombre(dto.nombre().trim());
        equipo.setJugador(jugador);
        return EquipoResponse.desde(equipoRepository.save(equipo));
    }

    @Transactional(readOnly = true)
    public List<EquipoResponse> listar(UUID jugadorId) {
        if (!jugadorRepository.existsById(jugadorId)) {
            throw new RecursoNoEncontradoException("Jugador no encontrado");
        }
        return equipoRepository.findByJugadorId(jugadorId).stream()
                .map(EquipoResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipoResponse obtener(UUID id) {
        return EquipoResponse.desde(buscarEquipo(id));
    }

    public PokemonEquipoResponse agregar(
            UUID equipoId, GuardarPokemonRequest dto) {
        Equipo equipo = buscarEquipo(equipoId);
        if (pokemonRepository.countByEquipoId(equipoId) >= 3) {
            throw new ConflictoException("El equipo ya contiene tres Pokémon");
        }
        PokemonEquipo nuevo = construirPokemon(equipo, dto);
        return PokemonEquipoResponse.desde(pokemonRepository.save(nuevo));
    }

    public PokemonEquipoResponse cambiar(
            UUID equipoId,
            UUID pokemonEquipoId,
            GuardarPokemonRequest dto) {
        PokemonEquipo actual = pokemonRepository.findById(pokemonEquipoId)
                .filter(pokemon -> pokemon.getEquipo().getId().equals(equipoId))
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Pokémon no encontrado"));

        PokemonEquipo datos = construirPokemon(actual.getEquipo(), dto);
        actual.setPokemonApiId(datos.getPokemonApiId());
        actual.setNombre(datos.getNombre());
        actual.setSprite(datos.getSprite());
        actual.setHpBase(datos.getHpBase());
        actual.setAtaqueBase(datos.getAtaqueBase());
        actual.setDefensaBase(datos.getDefensaBase());
        actual.setVelocidadBase(datos.getVelocidadBase());
        actual.getMovimientos().clear();
        datos.getMovimientos().forEach(movimiento -> {
            movimiento.setPokemon(actual);
            actual.getMovimientos().add(movimiento);
        });
        return PokemonEquipoResponse.desde(pokemonRepository.save(actual));
    }

    private PokemonEquipo construirPokemon(
            Equipo equipo, GuardarPokemonRequest dto) {
        Set<Integer> idsSolicitados = new HashSet<>(dto.movimientosApiId());
        if (idsSolicitados.size() != dto.movimientosApiId().size()) {
            throw new ReglaNegocioException("No se permiten movimientos repetidos");
        }

        var externo = pokeApi.pokemon(dto.pokemonApiId().toString());
        Set<Integer> permitidos = externo.moves().stream()
                .map(item -> extraerId(item.move().url()))
                .collect(Collectors.toSet());
        if (!permitidos.containsAll(idsSolicitados)) {
            throw new ReglaNegocioException(
                    "Uno o más movimientos no están disponibles para el Pokémon");
        }

        Map<String, Integer> estadisticas = externo.stats().stream()
                .collect(Collectors.toMap(
                        estadistica -> estadistica.stat().name(),
                        PokeApiClient.Estadistica::baseStat));

        PokemonEquipo pokemon = new PokemonEquipo();
        pokemon.setEquipo(equipo);
        pokemon.setPokemonApiId(externo.id());
        pokemon.setNombre(externo.name());
        pokemon.setSprite(externo.sprites().frontDefault());
        pokemon.setHpBase(estadisticas.get("hp"));
        pokemon.setAtaqueBase(estadisticas.get("attack"));
        pokemon.setDefensaBase(estadisticas.get("defense"));
        pokemon.setVelocidadBase(estadisticas.get("speed"));

        idsSolicitados.forEach(id -> {
            var externoMovimiento = pokeApi.movimiento(id);
            MovimientoSeleccionado movimiento = new MovimientoSeleccionado();
            movimiento.setPokemon(pokemon);
            movimiento.setMovimientoApiId(externoMovimiento.id());
            movimiento.setNombre(externoMovimiento.name());
            movimiento.setPoder(externoMovimiento.power());
            movimiento.setPrecisionMovimiento(externoMovimiento.accuracy());
            movimiento.setTipo(externoMovimiento.type().name());
            pokemon.getMovimientos().add(movimiento);
        });
        return pokemon;
    }

    private Equipo buscarEquipo(UUID id) {
        return equipoRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Equipo no encontrado"));
    }

    private int extraerId(String url) {
        String[] partes = url.replaceAll("/$", "").split("/");
        return Integer.parseInt(partes[partes.length - 1]);
    }
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/service/PartidaService.java`:

```java
package sv.edu.udb.pokebattle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.pokebattle.dto.CrearPartidaRequest;
import sv.edu.udb.pokebattle.dto.ParticipanteResponse;
import sv.edu.udb.pokebattle.dto.PartidaResponse;
import sv.edu.udb.pokebattle.dto.UnirsePartidaRequest;
import sv.edu.udb.pokebattle.exception.ConflictoException;
import sv.edu.udb.pokebattle.exception.RecursoNoEncontradoException;
import sv.edu.udb.pokebattle.exception.ReglaNegocioException;
import sv.edu.udb.pokebattle.model.Equipo;
import sv.edu.udb.pokebattle.model.EstadoPartida;
import sv.edu.udb.pokebattle.model.Jugador;
import sv.edu.udb.pokebattle.model.ParticipantePartida;
import sv.edu.udb.pokebattle.model.Partida;
import sv.edu.udb.pokebattle.model.PokemonBatalla;
import sv.edu.udb.pokebattle.model.PokemonEquipo;
import sv.edu.udb.pokebattle.repository.EquipoRepository;
import sv.edu.udb.pokebattle.repository.JugadorRepository;
import sv.edu.udb.pokebattle.repository.ParticipantePartidaRepository;
import sv.edu.udb.pokebattle.repository.PartidaRepository;
import sv.edu.udb.pokebattle.repository.PokemonBatallaRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PartidaService {
    private final PartidaRepository partidaRepository;
    private final ParticipantePartidaRepository participanteRepository;
    private final JugadorRepository jugadorRepository;
    private final EquipoRepository equipoRepository;
    private final PokemonBatallaRepository pokemonBatallaRepository;

    public PartidaResponse crear(CrearPartidaRequest dto) {
        Jugador jugador = buscarJugador(dto.jugadorId());
        Equipo equipo = validarEquipo(dto.equipoId(), jugador.getId());
        validarEquipoCompleto(equipo);

        Partida partida = new Partida();
        partida.setCodigoSala(generarCodigo());
        partidaRepository.save(partida);
        participanteRepository.save(participante(partida, jugador, equipo));
        return respuesta(partida);
    }

    public PartidaResponse unirse(
            UUID partidaId, UnirsePartidaRequest dto) {
        Partida partida = buscarPartida(partidaId);
        if (partida.getEstado() != EstadoPartida.ESPERANDO_JUGADOR) {
            throw new ConflictoException("La sala no admite más jugadores");
        }

        List<ParticipantePartida> participantes =
                participanteRepository.findByPartidaId(partidaId);
        if (participantes.size() >= 2) {
            throw new ConflictoException("La sala está llena");
        }

        Jugador jugador = buscarJugador(dto.jugadorId());
        if (participanteRepository.existsByPartidaIdAndJugadorId(
                partidaId, jugador.getId())) {
            throw new ConflictoException("El jugador ya participa en la sala");
        }

        Equipo equipo = validarEquipo(dto.equipoId(), jugador.getId());
        validarEquipoCompleto(equipo);
        participanteRepository.save(participante(partida, jugador, equipo));
        partida.setEstado(EstadoPartida.ESPERANDO_CONFIRMACION);
        return respuesta(partidaRepository.save(partida));
    }

    public PartidaResponse confirmar(UUID partidaId, UUID jugadorId) {
        Partida partida = buscarPartida(partidaId);
        if (partida.getEstado() != EstadoPartida.ESPERANDO_CONFIRMACION) {
            throw new ConflictoException(
                    "La partida no se encuentra esperando confirmaciones");
        }
        ParticipantePartida participante = participanteRepository
                .findByPartidaIdAndJugadorId(partidaId, jugadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Participante no encontrado"));
        participante.setConfirmado(true);
        participanteRepository.save(participante);
        return respuesta(partida);
    }

    public PartidaResponse iniciar(UUID partidaId) {
        Partida partida = buscarPartida(partidaId);
        if (partida.getEstado() != EstadoPartida.ESPERANDO_CONFIRMACION) {
            throw new ConflictoException("La partida no puede iniciar en su estado actual");
        }

        List<ParticipantePartida> participantes =
                participanteRepository.findByPartidaId(partidaId);
        if (participantes.size() != 2
                || participantes.stream().anyMatch(p -> !p.isConfirmado())) {
            throw new ConflictoException("Ambos jugadores deben confirmar");
        }

        participantes.forEach(this::crearEstadoInicial);
        Jugador primerTurno = participantes.stream()
                .max(Comparator.comparingInt(this::velocidadMaxima))
                .orElseThrow()
                .getJugador();

        partida.setTurnoDe(primerTurno);
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setIniciadaEn(LocalDateTime.now());
        return respuesta(partidaRepository.save(partida));
    }

    @Transactional(readOnly = true)
    public PartidaResponse obtener(UUID partidaId) {
        return respuesta(buscarPartida(partidaId));
    }

    @Transactional(readOnly = true)
    public PartidaResponse buscarPorCodigo(String codigo) {
        Partida partida = partidaRepository.findByCodigoSalaIgnoreCase(codigo)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Partida no encontrada"));
        return respuesta(partida);
    }

    private void crearEstadoInicial(ParticipantePartida participante) {
        List<PokemonEquipo> equipo = participante.getEquipo().getPokemon();
        for (int indice = 0; indice < equipo.size(); indice++) {
            PokemonEquipo origen = equipo.get(indice);
            PokemonBatalla estado = new PokemonBatalla();
            estado.setParticipante(participante);
            estado.setPokemonEquipo(origen);
            estado.setVidaActual(origen.getHpBase());
            estado.setActivo(indice == 0);
            pokemonBatallaRepository.save(estado);
        }
    }

    private int velocidadMaxima(ParticipantePartida participante) {
        return participante.getEquipo().getPokemon().stream()
                .mapToInt(PokemonEquipo::getVelocidadBase)
                .max()
                .orElse(0);
    }

    private ParticipantePartida participante(
            Partida partida, Jugador jugador, Equipo equipo) {
        ParticipantePartida participante = new ParticipantePartida();
        participante.setPartida(partida);
        participante.setJugador(jugador);
        participante.setEquipo(equipo);
        return participante;
    }

    private Jugador buscarJugador(UUID id) {
        return jugadorRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Jugador no encontrado"));
    }

    private Partida buscarPartida(UUID id) {
        return partidaRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Partida no encontrada"));
    }

    private Equipo validarEquipo(UUID equipoId, UUID jugadorId) {
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Equipo no encontrado"));
        if (!equipo.getJugador().getId().equals(jugadorId)) {
            throw new ReglaNegocioException(
                    "El equipo no pertenece al jugador indicado");
        }
        return equipo;
    }

    private void validarEquipoCompleto(Equipo equipo) {
        if (equipo.getPokemon().size() != 3) {
            throw new ReglaNegocioException(
                    "El equipo debe contener exactamente tres Pokémon");
        }
        if (equipo.getPokemon().stream()
                .anyMatch(pokemon -> pokemon.getMovimientos().isEmpty())) {
            throw new ReglaNegocioException(
                    "Cada Pokémon debe tener al menos un movimiento");
        }
    }

    private String generarCodigo() {
        String codigo;
        do {
            codigo = UUID.randomUUID().toString()
                    .substring(0, 8)
                    .toUpperCase();
        } while (partidaRepository.existsByCodigoSalaIgnoreCase(codigo));
        return codigo;
    }

    private PartidaResponse respuesta(Partida partida) {
        List<ParticipanteResponse> participantes = participanteRepository
                .findByPartidaId(partida.getId())
                .stream()
                .map(item -> new ParticipanteResponse(
                        item.getJugador().getId(),
                        item.getEquipo().getId(),
                        item.isConfirmado()))
                .toList();
        UUID turno = partida.getTurnoDe() == null
                ? null
                : partida.getTurnoDe().getId();
        return new PartidaResponse(
                partida.getId(),
                partida.getCodigoSala(),
                partida.getEstado(),
                turno,
                participantes);
    }
}
```

## 10. Controladores

Crear `src/main/java/sv/edu/udb/pokebattle/controller/JugadorController.java`:

```java
package sv.edu.udb.pokebattle.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.pokebattle.dto.CrearJugadorRequest;
import sv.edu.udb.pokebattle.dto.JugadorResponse;
import sv.edu.udb.pokebattle.service.JugadorService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jugadores")
@RequiredArgsConstructor
public class JugadorController {
    private final JugadorService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JugadorResponse crear(
            @Valid @RequestBody CrearJugadorRequest dto) {
        return service.crear(dto);
    }

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

Crear `src/main/java/sv/edu/udb/pokebattle/controller/CatalogoController.java`:

```java
package sv.edu.udb.pokebattle.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.pokebattle.dto.MovimientoResumen;
import sv.edu.udb.pokebattle.dto.PaginaResponse;
import sv.edu.udb.pokebattle.dto.PokemonDetalleResponse;
import sv.edu.udb.pokebattle.dto.PokemonResumen;
import sv.edu.udb.pokebattle.service.CatalogoService;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
@Validated
public class CatalogoController {
    private final CatalogoService service;

    @GetMapping("/pokemon")
    public PaginaResponse<PokemonResumen> listar(
            @RequestParam(defaultValue = "0") @PositiveOrZero int page) {
        return service.listar(page);
    }

    @GetMapping("/pokemon/buscar")
    public List<PokemonResumen> buscar(@RequestParam String nombre) {
        return service.buscar(nombre);
    }

    @GetMapping("/pokemon/{idONombre}")
    public PokemonDetalleResponse detalle(
            @PathVariable String idONombre) {
        return service.detalle(idONombre);
    }

    @GetMapping("/pokemon/{idONombre}/movimientos")
    public PaginaResponse<MovimientoResumen> movimientos(
            @PathVariable String idONombre,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.movimientos(idONombre, page, size);
    }
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/controller/EquipoController.java`:

```java
package sv.edu.udb.pokebattle.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.pokebattle.dto.CrearEquipoRequest;
import sv.edu.udb.pokebattle.dto.EquipoResponse;
import sv.edu.udb.pokebattle.dto.GuardarPokemonRequest;
import sv.edu.udb.pokebattle.dto.PokemonEquipoResponse;
import sv.edu.udb.pokebattle.service.EquipoService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class EquipoController {
    private final EquipoService service;

    @PostMapping("/api/jugadores/{jugadorId}/equipos")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoResponse crear(
            @PathVariable UUID jugadorId,
            @Valid @RequestBody CrearEquipoRequest dto) {
        return service.crear(jugadorId, dto);
    }

    @GetMapping("/api/jugadores/{jugadorId}/equipos")
    public List<EquipoResponse> listar(@PathVariable UUID jugadorId) {
        return service.listar(jugadorId);
    }

    @GetMapping("/api/equipos/{equipoId}")
    public EquipoResponse obtener(@PathVariable UUID equipoId) {
        return service.obtener(equipoId);
    }

    @PostMapping("/api/equipos/{equipoId}/pokemon")
    @ResponseStatus(HttpStatus.CREATED)
    public PokemonEquipoResponse agregar(
            @PathVariable UUID equipoId,
            @Valid @RequestBody GuardarPokemonRequest dto) {
        return service.agregar(equipoId, dto);
    }

    @PutMapping("/api/equipos/{equipoId}/pokemon/{pokemonEquipoId}")
    public PokemonEquipoResponse cambiar(
            @PathVariable UUID equipoId,
            @PathVariable UUID pokemonEquipoId,
            @Valid @RequestBody GuardarPokemonRequest dto) {
        return service.cambiar(equipoId, pokemonEquipoId, dto);
    }
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/controller/PartidaController.java`:

```java
package sv.edu.udb.pokebattle.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.pokebattle.dto.CrearPartidaRequest;
import sv.edu.udb.pokebattle.dto.PartidaResponse;
import sv.edu.udb.pokebattle.dto.UnirsePartidaRequest;
import sv.edu.udb.pokebattle.service.PartidaService;

import java.util.UUID;

@RestController
@RequestMapping("/api/partidas")
@RequiredArgsConstructor
public class PartidaController {
    private final PartidaService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartidaResponse crear(
            @Valid @RequestBody CrearPartidaRequest dto) {
        return service.crear(dto);
    }

    @PostMapping("/{partidaId}/unirse")
    public PartidaResponse unirse(
            @PathVariable UUID partidaId,
            @Valid @RequestBody UnirsePartidaRequest dto) {
        return service.unirse(partidaId, dto);
    }

    @PostMapping("/{partidaId}/jugadores/{jugadorId}/confirmar")
    public PartidaResponse confirmar(
            @PathVariable UUID partidaId,
            @PathVariable UUID jugadorId) {
        return service.confirmar(partidaId, jugadorId);
    }

    @PostMapping("/{partidaId}/iniciar")
    public PartidaResponse iniciar(@PathVariable UUID partidaId) {
        return service.iniciar(partidaId);
    }

    @GetMapping("/{partidaId}")
    public PartidaResponse obtener(@PathVariable UUID partidaId) {
        return service.obtener(partidaId);
    }

    @GetMapping("/codigo/{codigo}")
    public PartidaResponse buscarPorCodigo(@PathVariable String codigo) {
        return service.buscarPorCodigo(codigo);
    }
}
```

## 11. Archivo `.gitignore`

Crear `.gitignore`:

```gitignore
target/
.idea/
*.iml
.vscode/
.DS_Store
```

## 12. Compilar e iniciar

Descargar dependencias y compilar:

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

La consola H2 estará disponible en:

```text
http://localhost:8080/h2-console
```

Usar estos datos en la consola H2:

```text
JDBC URL: jdbc:h2:mem:pokebattle
User Name: sa
Password: dejar vacío
```

## 13. Comprobaciones iniciales

Registrar el primer jugador:

```bash
curl -X POST http://localhost:8080/api/jugadores \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Ash"}'
```

Consultar jugadores:

```bash
curl http://localhost:8080/api/jugadores
```

Consultar la primera página del catálogo:

```bash
curl "http://localhost:8080/api/catalogo/pokemon?page=0"
```

Buscar un Pokémon:

```bash
curl "http://localhost:8080/api/catalogo/pokemon/buscar?nombre=pika"
```

Consultar detalles:

```bash
curl http://localhost:8080/api/catalogo/pokemon/pikachu
```

Consultar movimientos:

```bash
curl "http://localhost:8080/api/catalogo/pokemon/pikachu/movimientos?page=0&size=20"
```

## 14. Inicializar Git

```bash
git init
git add .
git commit -m "feat: inicializar API PokéBattle con Spring Boot"
```
