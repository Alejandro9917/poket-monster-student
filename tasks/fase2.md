# Fase 2 — Combate completo de PokéBattle

## Objetivo

Actualizar el proyecto creado con `init.md` para que dos jugadores puedan completar una partida desde Postman.

Al finalizar esta fase, la API deberá permitir:

- consultar el estado completo de una partida;
- consultar a qué jugador le corresponde el turno;
- atacar con uno de los movimientos del Pokémon activo;
- descontar vida y marcar Pokémon debilitados;
- activar automáticamente el siguiente Pokémon disponible;
- cambiar voluntariamente el Pokémon activo;
- rendirse;
- finalizar la partida y registrar al ganador;
- consultar el historial de turnos, movimientos y resultado;
- rechazar acciones simultáneas o realizadas fuera de turno.

> Este archivo es la continuación de `init.md`. No se debe crear otro proyecto ni cambiar el paquete base `sv.edu.udb.pokebattle`.

## 1. Antes de modificar el proyecto

Abrir una terminal en la raíz del repositorio y comprobar que la fase anterior funciona:

```bash
mvn clean verify
mvn spring-boot:run
```

Detener la aplicación antes de continuar.

Crear una rama para esta fase:

```bash
git switch -c feature/fase-2-combate
```

## 2. Archivos que se agregarán o modificarán

### Archivos nuevos

```text
src/main/java/sv/edu/udb/pokebattle/
├── controller/
│   ├── CombateController.java
│   └── HistorialController.java
├── dto/
│   ├── AccionCombateResponse.java
│   ├── AtacarRequest.java
│   ├── CambiarPokemonRequest.java
│   ├── EstadoParticipanteResponse.java
│   ├── EstadoPartidaResponse.java
│   ├── MovimientoHistorialResponse.java
│   ├── PokemonBatallaResponse.java
│   ├── RendirseRequest.java
│   ├── ResultadoPartidaResponse.java
│   ├── TurnoActualResponse.java
│   └── TurnoResponse.java
├── model/
│   ├── HistorialMovimiento.java
│   ├── ResultadoMovimiento.java
│   ├── TipoAccion.java
│   └── Turno.java
├── repository/
│   ├── HistorialMovimientoRepository.java
│   ├── MovimientoSeleccionadoRepository.java
│   └── TurnoRepository.java
└── service/
    ├── CombateService.java
    └── HistorialService.java
```

### Archivos que se modificarán

```text
src/main/java/sv/edu/udb/pokebattle/
├── exception/ApiExceptionHandler.java
├── model/Partida.java
├── repository/PokemonBatallaRepository.java
└── service/PartidaService.java
```

## 3. Actualizar el modelo `Partida`

Abrir `src/main/java/sv/edu/udb/pokebattle/model/Partida.java`.

Agregar el siguiente import:

```java
import jakarta.persistence.Version;
```

Agregar estos campos dentro de la clase:

```java
@Column(nullable = false)
private Integer numeroTurno = 0;

private LocalDateTime finalizadaEn;

@Version
private Long version;
```

`numeroTurno` identifica cada acción. `finalizadaEn` registra el cierre de la partida y `@Version` habilita el bloqueo optimista para evitar que dos solicitudes actualicen el mismo turno simultáneamente.

## 4. Crear las enumeraciones del combate

Crear `src/main/java/sv/edu/udb/pokebattle/model/TipoAccion.java`:

```java
package sv.edu.udb.pokebattle.model;

public enum TipoAccion {
    ATAQUE,
    CAMBIO_POKEMON,
    RENDICION
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/model/ResultadoMovimiento.java`:

```java
package sv.edu.udb.pokebattle.model;

public enum ResultadoMovimiento {
    EXITOSO,
    FALLIDO,
    POKEMON_DEBILITADO,
    PARTIDA_FINALIZADA
}
```

## 5. Crear las entidades del historial

Crear `src/main/java/sv/edu/udb/pokebattle/model/Turno.java`:

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(
        columnNames = {"partida_id", "numero"}))
public class Turno {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "partida_id")
    private Partida partida;

    @Column(nullable = false)
    private Integer numero;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Jugador jugador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAccion tipoAccion;

    @Column(nullable = false, length = 250)
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
```

Crear `src/main/java/sv/edu/udb/pokebattle/model/HistorialMovimiento.java`:

```java
package sv.edu.udb.pokebattle.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class HistorialMovimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    private Turno turno;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MovimientoSeleccionado movimiento;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private PokemonBatalla atacante;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private PokemonBatalla defensor;

    private Integer dano;
    private Integer vidaAnterior;
    private Integer vidaRestante;

    @Enumerated(EnumType.STRING)
    private ResultadoMovimiento resultado;
}
```

## 6. Actualizar y crear repositorios

Reemplazar el contenido de `repository/PokemonBatallaRepository.java`:

```java
package sv.edu.udb.pokebattle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.pokebattle.model.PokemonBatalla;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PokemonBatallaRepository
        extends JpaRepository<PokemonBatalla, UUID> {

    List<PokemonBatalla> findByParticipantePartidaId(UUID partidaId);

    List<PokemonBatalla> findByParticipantePartidaIdAndParticipanteJugadorId(
            UUID partidaId,
            UUID jugadorId);

    Optional<PokemonBatalla>
    findByParticipantePartidaIdAndParticipanteJugadorIdAndActivoTrue(
            UUID partidaId,
            UUID jugadorId);

    List<PokemonBatalla>
    findByParticipantePartidaIdAndParticipanteJugadorIdAndDebilitadoFalse(
            UUID partidaId,
            UUID jugadorId);
}
```

Crear `repository/MovimientoSeleccionadoRepository.java`:

```java
package sv.edu.udb.pokebattle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.pokebattle.model.MovimientoSeleccionado;

import java.util.UUID;

public interface MovimientoSeleccionadoRepository
        extends JpaRepository<MovimientoSeleccionado, UUID> {
}
```

Crear `repository/TurnoRepository.java`:

```java
package sv.edu.udb.pokebattle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.pokebattle.model.Turno;

import java.util.List;
import java.util.UUID;

public interface TurnoRepository extends JpaRepository<Turno, UUID> {
    List<Turno> findByPartidaIdOrderByNumeroAsc(UUID partidaId);
}
```

Crear `repository/HistorialMovimientoRepository.java`:

```java
package sv.edu.udb.pokebattle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.pokebattle.model.HistorialMovimiento;

import java.util.List;
import java.util.UUID;

public interface HistorialMovimientoRepository
        extends JpaRepository<HistorialMovimiento, UUID> {
    List<HistorialMovimiento> findByTurnoPartidaIdOrderByTurnoNumeroAsc(
            UUID partidaId);
}
```

## 7. Crear los DTO de solicitudes

Crear `dto/AtacarRequest.java`:

```java
package sv.edu.udb.pokebattle.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AtacarRequest(
        @NotNull UUID jugadorId,
        @NotNull UUID movimientoId) {
}
```

Crear `dto/CambiarPokemonRequest.java`:

```java
package sv.edu.udb.pokebattle.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CambiarPokemonRequest(
        @NotNull UUID jugadorId,
        @NotNull UUID pokemonEquipoId) {
}
```

Crear `dto/RendirseRequest.java`:

```java
package sv.edu.udb.pokebattle.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RendirseRequest(@NotNull UUID jugadorId) {
}
```

## 8. Crear los DTO del estado del combate

Crear `dto/PokemonBatallaResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import sv.edu.udb.pokebattle.model.PokemonBatalla;
import java.util.UUID;

public record PokemonBatallaResponse(
        UUID pokemonBatallaId,
        UUID pokemonEquipoId,
        String nombre,
        String sprite,
        Integer vidaActual,
        Integer vidaMaxima,
        boolean activo,
        boolean debilitado) {

    public static PokemonBatallaResponse desde(PokemonBatalla pokemon) {
        return new PokemonBatallaResponse(
                pokemon.getId(),
                pokemon.getPokemonEquipo().getId(),
                pokemon.getPokemonEquipo().getNombre(),
                pokemon.getPokemonEquipo().getSprite(),
                pokemon.getVidaActual(),
                pokemon.getPokemonEquipo().getHpBase(),
                pokemon.isActivo(),
                pokemon.isDebilitado());
    }
}
```

Crear `dto/EstadoParticipanteResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import java.util.List;
import java.util.UUID;

public record EstadoParticipanteResponse(
        UUID jugadorId,
        String nombreJugador,
        boolean confirmado,
        List<PokemonBatallaResponse> pokemon) {
}
```

Crear `dto/EstadoPartidaResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import sv.edu.udb.pokebattle.model.EstadoPartida;
import java.util.List;
import java.util.UUID;

public record EstadoPartidaResponse(
        UUID partidaId,
        String codigoSala,
        EstadoPartida estado,
        Integer numeroTurno,
        UUID turnoDe,
        UUID ganadorId,
        List<EstadoParticipanteResponse> participantes) {
}
```

Crear `dto/TurnoActualResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import java.util.UUID;

public record TurnoActualResponse(
        UUID partidaId,
        Integer numeroTurno,
        UUID jugadorId,
        String nombreJugador) {
}
```

Crear `dto/AccionCombateResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import sv.edu.udb.pokebattle.model.EstadoPartida;
import sv.edu.udb.pokebattle.model.TipoAccion;
import java.util.UUID;

public record AccionCombateResponse(
        Integer numeroTurno,
        TipoAccion tipoAccion,
        UUID jugadorId,
        String mensaje,
        Integer dano,
        Integer vidaRestante,
        boolean pokemonDebilitado,
        EstadoPartida estadoPartida,
        UUID siguienteTurnoDe,
        UUID ganadorId) {
}
```

## 9. Crear los DTO del historial y resultado

Crear `dto/TurnoResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import sv.edu.udb.pokebattle.model.TipoAccion;
import sv.edu.udb.pokebattle.model.Turno;

import java.time.LocalDateTime;
import java.util.UUID;

public record TurnoResponse(
        Integer numero,
        UUID jugadorId,
        String jugador,
        TipoAccion tipoAccion,
        String descripcion,
        LocalDateTime creadoEn) {

    public static TurnoResponse desde(Turno turno) {
        return new TurnoResponse(
                turno.getNumero(),
                turno.getJugador().getId(),
                turno.getJugador().getNombre(),
                turno.getTipoAccion(),
                turno.getDescripcion(),
                turno.getCreadoEn());
    }
}
```

Crear `dto/MovimientoHistorialResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import sv.edu.udb.pokebattle.model.ResultadoMovimiento;

public record MovimientoHistorialResponse(
        Integer numeroTurno,
        String atacante,
        String defensor,
        String movimiento,
        Integer dano,
        Integer vidaRestante,
        ResultadoMovimiento resultado) {
}
```

Crear `dto/ResultadoPartidaResponse.java`:

```java
package sv.edu.udb.pokebattle.dto;

import sv.edu.udb.pokebattle.model.EstadoPartida;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResultadoPartidaResponse(
        UUID partidaId,
        EstadoPartida estado,
        UUID ganadorId,
        String ganador,
        LocalDateTime finalizadaEn) {
}
```

## 10. Crear `CombateService`

Crear `src/main/java/sv/edu/udb/pokebattle/service/CombateService.java`:

```java
package sv.edu.udb.pokebattle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.pokebattle.dto.AccionCombateResponse;
import sv.edu.udb.pokebattle.dto.AtacarRequest;
import sv.edu.udb.pokebattle.dto.CambiarPokemonRequest;
import sv.edu.udb.pokebattle.dto.EstadoParticipanteResponse;
import sv.edu.udb.pokebattle.dto.EstadoPartidaResponse;
import sv.edu.udb.pokebattle.dto.PokemonBatallaResponse;
import sv.edu.udb.pokebattle.dto.RendirseRequest;
import sv.edu.udb.pokebattle.dto.TurnoActualResponse;
import sv.edu.udb.pokebattle.exception.ConflictoException;
import sv.edu.udb.pokebattle.exception.RecursoNoEncontradoException;
import sv.edu.udb.pokebattle.exception.ReglaNegocioException;
import sv.edu.udb.pokebattle.model.EstadoPartida;
import sv.edu.udb.pokebattle.model.HistorialMovimiento;
import sv.edu.udb.pokebattle.model.Jugador;
import sv.edu.udb.pokebattle.model.MovimientoSeleccionado;
import sv.edu.udb.pokebattle.model.ParticipantePartida;
import sv.edu.udb.pokebattle.model.Partida;
import sv.edu.udb.pokebattle.model.PokemonBatalla;
import sv.edu.udb.pokebattle.model.ResultadoMovimiento;
import sv.edu.udb.pokebattle.model.TipoAccion;
import sv.edu.udb.pokebattle.model.Turno;
import sv.edu.udb.pokebattle.repository.HistorialMovimientoRepository;
import sv.edu.udb.pokebattle.repository.MovimientoSeleccionadoRepository;
import sv.edu.udb.pokebattle.repository.ParticipantePartidaRepository;
import sv.edu.udb.pokebattle.repository.PartidaRepository;
import sv.edu.udb.pokebattle.repository.PokemonBatallaRepository;
import sv.edu.udb.pokebattle.repository.TurnoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class CombateService {
    private final PartidaRepository partidaRepository;
    private final ParticipantePartidaRepository participanteRepository;
    private final PokemonBatallaRepository pokemonBatallaRepository;
    private final MovimientoSeleccionadoRepository movimientoRepository;
    private final TurnoRepository turnoRepository;
    private final HistorialMovimientoRepository historialRepository;

    @Transactional(readOnly = true)
    public EstadoPartidaResponse estado(UUID partidaId) {
        Partida partida = buscarPartida(partidaId);
        List<EstadoParticipanteResponse> participantes = participanteRepository
                .findByPartidaId(partidaId)
                .stream()
                .map(participante -> new EstadoParticipanteResponse(
                        participante.getJugador().getId(),
                        participante.getJugador().getNombre(),
                        participante.isConfirmado(),
                        pokemonBatallaRepository
                                .findByParticipantePartidaIdAndParticipanteJugadorId(
                                        partidaId,
                                        participante.getJugador().getId())
                                .stream()
                                .map(PokemonBatallaResponse::desde)
                                .toList()))
                .toList();

        return new EstadoPartidaResponse(
                partida.getId(),
                partida.getCodigoSala(),
                partida.getEstado(),
                partida.getNumeroTurno(),
                id(partida.getTurnoDe()),
                id(partida.getGanador()),
                participantes);
    }

    @Transactional(readOnly = true)
    public TurnoActualResponse turno(UUID partidaId) {
        Partida partida = buscarPartida(partidaId);
        validarEnCurso(partida);
        return new TurnoActualResponse(
                partida.getId(),
                partida.getNumeroTurno(),
                partida.getTurnoDe().getId(),
                partida.getTurnoDe().getNombre());
    }

    public AccionCombateResponse atacar(UUID partidaId, AtacarRequest dto) {
        Partida partida = buscarPartida(partidaId);
        validarTurno(partida, dto.jugadorId());

        PokemonBatalla atacante = pokemonActivo(partidaId, dto.jugadorId());
        MovimientoSeleccionado movimiento = movimientoRepository
                .findById(dto.movimientoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Movimiento no encontrado"));

        if (!movimiento.getPokemon().getId()
                .equals(atacante.getPokemonEquipo().getId())) {
            throw new ReglaNegocioException(
                    "El movimiento no pertenece al Pokémon activo");
        }

        ParticipantePartida oponente = oponente(partidaId, dto.jugadorId());
        PokemonBatalla defensor = pokemonActivo(
                partidaId,
                oponente.getJugador().getId());

        int vidaAnterior = defensor.getVidaActual();
        boolean acierta = evaluarPrecision(movimiento);
        int dano = acierta
                ? calcularDano(atacante, defensor, movimiento)
                : 0;
        defensor.setVidaActual(Math.max(0, vidaAnterior - dano));

        boolean debilitado = defensor.getVidaActual() == 0;
        ResultadoMovimiento resultado = acierta
                ? ResultadoMovimiento.EXITOSO
                : ResultadoMovimiento.FALLIDO;

        if (debilitado) {
            defensor.setDebilitado(true);
            defensor.setActivo(false);
            resultado = ResultadoMovimiento.POKEMON_DEBILITADO;
        }
        pokemonBatallaRepository.save(defensor);

        Turno turno = registrarTurno(
                partida,
                dto.jugadorId(),
                TipoAccion.ATAQUE,
                atacante.getPokemonEquipo().getNombre()
                        + " utilizó "
                        + movimiento.getNombre());

        boolean partidaFinalizada = false;
        if (debilitado && !activarSiguiente(
                partidaId,
                oponente.getJugador().getId())) {
            finalizar(partida, atacante.getParticipante().getJugador());
            resultado = ResultadoMovimiento.PARTIDA_FINALIZADA;
            partidaFinalizada = true;
        } else {
            avanzarTurno(partida, oponente.getJugador());
        }

        registrarMovimiento(
                turno,
                movimiento,
                atacante,
                defensor,
                dano,
                vidaAnterior,
                resultado);
        partidaRepository.save(partida);

        String mensaje = partidaFinalizada
                ? "Ataque procesado; la partida finalizó"
                : acierta ? "Ataque procesado" : "El ataque falló";

        return respuestaAccion(
                partida,
                turno,
                dano,
                defensor.getVidaActual(),
                debilitado,
                mensaje);
    }

    public AccionCombateResponse cambiar(
            UUID partidaId,
            CambiarPokemonRequest dto) {
        Partida partida = buscarPartida(partidaId);
        validarTurno(partida, dto.jugadorId());
        PokemonBatalla actual = pokemonActivo(partidaId, dto.jugadorId());

        PokemonBatalla destino = pokemonBatallaRepository
                .findByParticipantePartidaIdAndParticipanteJugadorId(
                        partidaId,
                        dto.jugadorId())
                .stream()
                .filter(pokemon -> pokemon.getPokemonEquipo().getId()
                        .equals(dto.pokemonEquipoId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Pokémon no encontrado en la partida"));

        if (destino.isDebilitado()) {
            throw new ReglaNegocioException("El Pokémon está debilitado");
        }
        if (destino.isActivo()) {
            throw new ConflictoException("El Pokémon ya está activo");
        }

        actual.setActivo(false);
        destino.setActivo(true);
        pokemonBatallaRepository.saveAll(List.of(actual, destino));

        Turno turno = registrarTurno(
                partida,
                dto.jugadorId(),
                TipoAccion.CAMBIO_POKEMON,
                "Cambio a " + destino.getPokemonEquipo().getNombre());
        Jugador siguiente = oponente(partidaId, dto.jugadorId()).getJugador();
        avanzarTurno(partida, siguiente);
        partidaRepository.save(partida);

        return respuestaAccion(
                partida,
                turno,
                0,
                destino.getVidaActual(),
                false,
                "Pokémon cambiado");
    }

    public AccionCombateResponse rendirse(
            UUID partidaId,
            RendirseRequest dto) {
        Partida partida = buscarPartida(partidaId);
        validarEnCurso(partida);
        participante(partidaId, dto.jugadorId());

        Jugador ganador = oponente(partidaId, dto.jugadorId()).getJugador();
        Turno turno = registrarTurno(
                partida,
                dto.jugadorId(),
                TipoAccion.RENDICION,
                "El jugador se rindió");
        finalizar(partida, ganador);
        partidaRepository.save(partida);

        return respuestaAccion(
                partida,
                turno,
                0,
                null,
                false,
                "Partida finalizada por rendición");
    }

    private boolean evaluarPrecision(MovimientoSeleccionado movimiento) {
        int precision = movimiento.getPrecisionMovimiento() == null
                ? 100
                : movimiento.getPrecisionMovimiento();
        return ThreadLocalRandom.current().nextInt(1, 101) <= precision;
    }

    private int calcularDano(
            PokemonBatalla atacante,
            PokemonBatalla defensor,
            MovimientoSeleccionado movimiento) {
        int poder = movimiento.getPoder() == null
                ? 10
                : movimiento.getPoder();
        return Math.max(
                1,
                ((atacante.getPokemonEquipo().getAtaqueBase() * poder) / 100)
                        - (defensor.getPokemonEquipo().getDefensaBase() / 10));
    }

    private boolean activarSiguiente(UUID partidaId, UUID jugadorId) {
        return pokemonBatallaRepository
                .findByParticipantePartidaIdAndParticipanteJugadorIdAndDebilitadoFalse(
                        partidaId,
                        jugadorId)
                .stream()
                .filter(pokemon -> !pokemon.isActivo())
                .findFirst()
                .map(pokemon -> {
                    pokemon.setActivo(true);
                    pokemonBatallaRepository.save(pokemon);
                    return true;
                })
                .orElse(false);
    }

    private Turno registrarTurno(
            Partida partida,
            UUID jugadorId,
            TipoAccion tipo,
            String descripcion) {
        Turno turno = new Turno();
        turno.setPartida(partida);
        turno.setNumero(partida.getNumeroTurno());
        turno.setJugador(
                participante(partida.getId(), jugadorId).getJugador());
        turno.setTipoAccion(tipo);
        turno.setDescripcion(descripcion);
        return turnoRepository.save(turno);
    }

    private void registrarMovimiento(
            Turno turno,
            MovimientoSeleccionado movimiento,
            PokemonBatalla atacante,
            PokemonBatalla defensor,
            int dano,
            int vidaAnterior,
            ResultadoMovimiento resultado) {
        HistorialMovimiento historial = new HistorialMovimiento();
        historial.setTurno(turno);
        historial.setMovimiento(movimiento);
        historial.setAtacante(atacante);
        historial.setDefensor(defensor);
        historial.setDano(dano);
        historial.setVidaAnterior(vidaAnterior);
        historial.setVidaRestante(defensor.getVidaActual());
        historial.setResultado(resultado);
        historialRepository.save(historial);
    }

    private void avanzarTurno(Partida partida, Jugador siguiente) {
        partida.setNumeroTurno(partida.getNumeroTurno() + 1);
        partida.setTurnoDe(siguiente);
    }

    private void finalizar(Partida partida, Jugador ganador) {
        partida.setEstado(EstadoPartida.FINALIZADA);
        partida.setGanador(ganador);
        partida.setTurnoDe(null);
        partida.setFinalizadaEn(LocalDateTime.now());
    }

    private void validarTurno(Partida partida, UUID jugadorId) {
        validarEnCurso(partida);
        if (!partida.getTurnoDe().getId().equals(jugadorId)) {
            throw new ConflictoException(
                    "No corresponde al turno del jugador");
        }
    }

    private void validarEnCurso(Partida partida) {
        if (partida.getEstado() != EstadoPartida.EN_CURSO) {
            throw new ConflictoException("La partida no está en curso");
        }
    }

    private PokemonBatalla pokemonActivo(UUID partidaId, UUID jugadorId) {
        return pokemonBatallaRepository
                .findByParticipantePartidaIdAndParticipanteJugadorIdAndActivoTrue(
                        partidaId,
                        jugadorId)
                .orElseThrow(() -> new ConflictoException(
                        "El jugador no posee un Pokémon activo"));
    }

    private ParticipantePartida participante(
            UUID partidaId,
            UUID jugadorId) {
        return participanteRepository
                .findByPartidaIdAndJugadorId(partidaId, jugadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Participante no encontrado"));
    }

    private ParticipantePartida oponente(
            UUID partidaId,
            UUID jugadorId) {
        return participanteRepository
                .findByPartidaId(partidaId)
                .stream()
                .filter(participante -> !participante.getJugador().getId()
                        .equals(jugadorId))
                .findFirst()
                .orElseThrow(() -> new ConflictoException(
                        "La partida no posee un oponente"));
    }

    private Partida buscarPartida(UUID partidaId) {
        return partidaRepository
                .findById(partidaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Partida no encontrada"));
    }

    private UUID id(Jugador jugador) {
        return jugador == null ? null : jugador.getId();
    }

    private AccionCombateResponse respuestaAccion(
            Partida partida,
            Turno turno,
            Integer dano,
            Integer vidaRestante,
            boolean debilitado,
            String mensaje) {
        return new AccionCombateResponse(
                turno.getNumero(),
                turno.getTipoAccion(),
                turno.getJugador().getId(),
                mensaje,
                dano,
                vidaRestante,
                debilitado,
                partida.getEstado(),
                id(partida.getTurnoDe()),
                id(partida.getGanador()));
    }
}
```

## 11. Ajustar el inicio de la partida

Abrir `service/PartidaService.java` y localizar el método `iniciar`.

Dentro del método ya se obtiene la variable `primerTurno`. Antes de guardar la partida, el bloque final debe quedar así:

```java
partida.setNumeroTurno(1);
partida.setTurnoDe(primerTurno);
partida.setEstado(EstadoPartida.EN_CURSO);
partida.setIniciadaEn(LocalDateTime.now());
return respuesta(partidaRepository.save(partida));
```

No se debe llamar nuevamente a `crearEstadoInicial` fuera del ciclo que ya existe en `iniciar`, porque se duplicarían los registros de `PokemonBatalla`.

## 12. Crear `HistorialService`

Crear `src/main/java/sv/edu/udb/pokebattle/service/HistorialService.java`:

```java
package sv.edu.udb.pokebattle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.pokebattle.dto.MovimientoHistorialResponse;
import sv.edu.udb.pokebattle.dto.ResultadoPartidaResponse;
import sv.edu.udb.pokebattle.dto.TurnoResponse;
import sv.edu.udb.pokebattle.exception.RecursoNoEncontradoException;
import sv.edu.udb.pokebattle.model.Partida;
import sv.edu.udb.pokebattle.repository.HistorialMovimientoRepository;
import sv.edu.udb.pokebattle.repository.PartidaRepository;
import sv.edu.udb.pokebattle.repository.TurnoRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistorialService {
    private final PartidaRepository partidaRepository;
    private final TurnoRepository turnoRepository;
    private final HistorialMovimientoRepository historialRepository;

    public List<TurnoResponse> turnos(UUID partidaId) {
        validarPartida(partidaId);
        return turnoRepository
                .findByPartidaIdOrderByNumeroAsc(partidaId)
                .stream()
                .map(TurnoResponse::desde)
                .toList();
    }

    public List<MovimientoHistorialResponse> movimientos(UUID partidaId) {
        validarPartida(partidaId);
        return historialRepository
                .findByTurnoPartidaIdOrderByTurnoNumeroAsc(partidaId)
                .stream()
                .map(historial -> new MovimientoHistorialResponse(
                        historial.getTurno().getNumero(),
                        historial.getAtacante().getPokemonEquipo().getNombre(),
                        historial.getDefensor().getPokemonEquipo().getNombre(),
                        historial.getMovimiento().getNombre(),
                        historial.getDano(),
                        historial.getVidaRestante(),
                        historial.getResultado()))
                .toList();
    }

    public ResultadoPartidaResponse resultado(UUID partidaId) {
        Partida partida = partidaRepository
                .findById(partidaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Partida no encontrada"));

        return new ResultadoPartidaResponse(
                partida.getId(),
                partida.getEstado(),
                partida.getGanador() == null
                        ? null
                        : partida.getGanador().getId(),
                partida.getGanador() == null
                        ? null
                        : partida.getGanador().getNombre(),
                partida.getFinalizadaEn());
    }

    private void validarPartida(UUID partidaId) {
        if (!partidaRepository.existsById(partidaId)) {
            throw new RecursoNoEncontradoException("Partida no encontrada");
        }
    }
}
```

## 13. Crear los controladores

Crear `controller/CombateController.java`:

```java
package sv.edu.udb.pokebattle.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.pokebattle.dto.AccionCombateResponse;
import sv.edu.udb.pokebattle.dto.AtacarRequest;
import sv.edu.udb.pokebattle.dto.CambiarPokemonRequest;
import sv.edu.udb.pokebattle.dto.EstadoPartidaResponse;
import sv.edu.udb.pokebattle.dto.RendirseRequest;
import sv.edu.udb.pokebattle.dto.TurnoActualResponse;
import sv.edu.udb.pokebattle.service.CombateService;

import java.util.UUID;

@RestController
@RequestMapping("/api/partidas/{partidaId}")
@RequiredArgsConstructor
public class CombateController {
    private final CombateService service;

    @GetMapping("/estado")
    public EstadoPartidaResponse estado(@PathVariable UUID partidaId) {
        return service.estado(partidaId);
    }

    @GetMapping("/turno")
    public TurnoActualResponse turno(@PathVariable UUID partidaId) {
        return service.turno(partidaId);
    }

    @PostMapping("/acciones/atacar")
    public AccionCombateResponse atacar(
            @PathVariable UUID partidaId,
            @Valid @RequestBody AtacarRequest dto) {
        return service.atacar(partidaId, dto);
    }

    @PostMapping("/acciones/cambiar-pokemon")
    public AccionCombateResponse cambiar(
            @PathVariable UUID partidaId,
            @Valid @RequestBody CambiarPokemonRequest dto) {
        return service.cambiar(partidaId, dto);
    }

    @PostMapping("/acciones/rendirse")
    public AccionCombateResponse rendirse(
            @PathVariable UUID partidaId,
            @Valid @RequestBody RendirseRequest dto) {
        return service.rendirse(partidaId, dto);
    }
}
```

Crear `controller/HistorialController.java`:

```java
package sv.edu.udb.pokebattle.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.edu.udb.pokebattle.dto.MovimientoHistorialResponse;
import sv.edu.udb.pokebattle.dto.ResultadoPartidaResponse;
import sv.edu.udb.pokebattle.dto.TurnoResponse;
import sv.edu.udb.pokebattle.service.HistorialService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partidas/{partidaId}")
@RequiredArgsConstructor
public class HistorialController {
    private final HistorialService service;

    @GetMapping("/turnos")
    public List<TurnoResponse> turnos(@PathVariable UUID partidaId) {
        return service.turnos(partidaId);
    }

    @GetMapping("/movimientos")
    public List<MovimientoHistorialResponse> movimientos(
            @PathVariable UUID partidaId) {
        return service.movimientos(partidaId);
    }

    @GetMapping("/resultado")
    public ResultadoPartidaResponse resultado(
            @PathVariable UUID partidaId) {
        return service.resultado(partidaId);
    }
}
```

## 14. Manejar actualizaciones simultáneas

Abrir `exception/ApiExceptionHandler.java` y agregar estos imports:

```java
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
```

Agregar este método dentro de la clase `ApiExceptionHandler`:

```java
@ExceptionHandler({
        ObjectOptimisticLockingFailureException.class,
        OptimisticLockException.class
})
@ResponseStatus(HttpStatus.CONFLICT)
ErrorResponse concurrencia(Exception ex) {
    return error(
            HttpStatus.CONFLICT,
            "La partida fue actualizada por otra solicitud; "
                    + "consulta el estado nuevamente");
}
```

## 15. Compilar y ejecutar

Compilar el proyecto:

```bash
mvn clean verify
```

Si la compilación termina correctamente, iniciar la API:

```bash
mvn spring-boot:run
```

La aplicación estará disponible en:

```text
http://localhost:8080
```

Debido a que se utiliza H2 en memoria con `create-drop`, los jugadores, equipos y partidas deben crearse nuevamente cada vez que se reinicie la aplicación.

## 16. Endpoints agregados en esta fase

| Método | Endpoint | Función |
|---|---|---|
| `GET` | `/api/partidas/{partidaId}/estado` | Consultar jugadores, vida, Pokémon activos, turno y ganador |
| `GET` | `/api/partidas/{partidaId}/turno` | Consultar el jugador que debe actuar |
| `POST` | `/api/partidas/{partidaId}/acciones/atacar` | Ejecutar un ataque |
| `POST` | `/api/partidas/{partidaId}/acciones/cambiar-pokemon` | Cambiar el Pokémon activo y consumir el turno |
| `POST` | `/api/partidas/{partidaId}/acciones/rendirse` | Finalizar por rendición |
| `GET` | `/api/partidas/{partidaId}/turnos` | Consultar acciones ordenadas por turno |
| `GET` | `/api/partidas/{partidaId}/movimientos` | Consultar ataques y daño causado |
| `GET` | `/api/partidas/{partidaId}/resultado` | Consultar estado final y ganador |

## 17. Preparar una partida para las pruebas

Antes de probar el combate se debe completar el flujo de `init.md`:

1. Registrar dos jugadores.
2. Crear un equipo para cada jugador.
3. Agregar exactamente tres Pokémon a cada equipo.
4. Asignar de uno a cuatro movimientos válidos a cada Pokémon.
5. Crear una partida con el primer jugador y su equipo.
6. Unir al segundo jugador con su equipo.
7. Confirmar a ambos jugadores.
8. Iniciar la partida.

Guardar en variables de Postman los valores siguientes:

```text
baseUrl=http://localhost:8080
partidaId=UUID-DE-LA-PARTIDA
jugador1Id=UUID-DEL-JUGADOR-1
jugador2Id=UUID-DEL-JUGADOR-2
```

## 18. Probar el estado y el turno

Consultar el estado:

```http
GET {{baseUrl}}/api/partidas/{{partidaId}}/estado
```

La respuesta debe mostrar:

- `estado` con valor `EN_CURSO`;
- `numeroTurno` con valor inicial `1`;
- `turnoDe` con el UUID del primer jugador;
- tres Pokémon por participante;
- exactamente un Pokémon activo por jugador;
- vida actual igual a la vida máxima de cada Pokémon.

Consultar el turno:

```http
GET {{baseUrl}}/api/partidas/{{partidaId}}/turno
```

## 19. Probar un ataque

Del resultado de `/estado`, identificar el jugador en `turnoDe` y su Pokémon activo. Luego consultar el equipo correspondiente con `GET /api/equipos/{equipoId}` y copiar el `id` de uno de los movimientos guardados para ese Pokémon. Este identificador pertenece a `MovimientoSeleccionado`; no se debe enviar el ID numérico de PokéAPI.

```http
POST {{baseUrl}}/api/partidas/{{partidaId}}/acciones/atacar
Content-Type: application/json

{
  "jugadorId": "UUID-JUGADOR-CON-TURNO",
  "movimientoId": "UUID-MOVIMIENTO-SELECCIONADO"
}
```

Después del ataque se debe comprobar que:

- `dano` es cero si el ataque falla o mayor que cero si acierta;
- `vidaRestante` nunca es negativa;
- `siguienteTurnoDe` corresponde al oponente;
- `numeroTurno` aumenta en la siguiente acción;
- la acción aparece en `/turnos`;
- el ataque aparece en `/movimientos`.

### Prueba negativa de turno

Repetir inmediatamente el ataque con el mismo jugador. La API debe responder con HTTP `409 Conflict`:

```json
{
  "estado": 409,
  "error": "Conflict",
  "mensaje": "No corresponde al turno del jugador",
  "fecha": "2026-10-06T20:25:00"
}
```

## 20. Probar el cambio de Pokémon

Usar el jugador que tenga el turno y seleccionar uno de sus Pokémon que no esté activo ni debilitado:

```http
POST {{baseUrl}}/api/partidas/{{partidaId}}/acciones/cambiar-pokemon
Content-Type: application/json

{
  "jugadorId": "UUID-JUGADOR-CON-TURNO",
  "pokemonEquipoId": "UUID-POKEMON-DISPONIBLE"
}
```

El cambio consume el turno. Al consultar `/estado`, el Pokémon anterior debe tener `activo: false`, el nuevo debe tener `activo: true` y `turnoDe` debe señalar al oponente.

## 21. Completar la partida

Continuar alternando ataques. Cuando la vida de un Pokémon llegue a cero:

- debe quedar con `vidaActual: 0`;
- debe quedar con `debilitado: true`;
- el siguiente Pokémon disponible debe activarse automáticamente.

Cuando los tres Pokémon de un participante estén debilitados:

- la partida debe cambiar a `FINALIZADA`;
- `turnoDe` debe quedar en `null`;
- `ganadorId` debe contener el jugador contrario;
- se debe registrar `finalizadaEn`.

Para finalizar más rápido también se puede probar la rendición:

```http
POST {{baseUrl}}/api/partidas/{{partidaId}}/acciones/rendirse
Content-Type: application/json

{
  "jugadorId": "UUID-JUGADOR"
}
```

## 22. Consultar historial y resultado

Consultar todos los turnos:

```http
GET {{baseUrl}}/api/partidas/{{partidaId}}/turnos
```

Consultar solamente los ataques:

```http
GET {{baseUrl}}/api/partidas/{{partidaId}}/movimientos
```

Consultar el resultado:

```http
GET {{baseUrl}}/api/partidas/{{partidaId}}/resultado
```

Ejemplo de resultado final:

```json
{
  "partidaId": "UUID-DE-LA-PARTIDA",
  "estado": "FINALIZADA",
  "ganadorId": "UUID-DEL-GANADOR",
  "ganador": "Ash",
  "finalizadaEn": "2026-10-06T20:30:00"
}
```

## 23. Casos que deben rechazarse

La implementación se considera correcta cuando rechaza estos casos:

| Caso | Respuesta esperada |
|---|---|
| Atacar cuando la partida no está en curso | `409 Conflict` |
| Atacar fuera de turno | `409 Conflict` |
| Usar un movimiento de otro Pokémon | `400 Bad Request` |
| Cambiar a un Pokémon debilitado | `400 Bad Request` |
| Cambiar al mismo Pokémon activo | `409 Conflict` |
| Enviar un jugador que no participa | `404 Not Found` |
| Realizar una acción después de finalizar | `409 Conflict` |
| Enviar dos actualizaciones simultáneas sobre la misma versión | una se procesa y la otra devuelve `409 Conflict` |

## 24. Criterio de finalización

La fase 2 está completa cuando se puede demostrar desde Postman una partida que:

1. inicia con dos jugadores confirmados;
2. alterna correctamente los turnos;
3. permite atacar y cambiar de Pokémon;
4. debilita y reemplaza Pokémon;
5. pasa de `EN_CURSO` a `FINALIZADA`;
6. devuelve un ganador;
7. conserva el historial de turnos y movimientos;
8. rechaza al menos una acción fuera de turno con HTTP `409`.

## 25. Guardar los cambios en Git

Comprobar el estado del repositorio:

```bash
git status
```

Compilar una última vez:

```bash
mvn clean verify
```

Guardar la fase:

```bash
git add .
git commit -m "feat: agregar combate por turnos e historial de partidas"
```

## Resultado esperado

El repositorio conserva todos los componentes construidos con `init.md` y agrega una segunda capa funcional de combate. No requiere frontend: toda la demostración puede ejecutarse desde Postman utilizando las rutas REST de las fases 1 y 2.
