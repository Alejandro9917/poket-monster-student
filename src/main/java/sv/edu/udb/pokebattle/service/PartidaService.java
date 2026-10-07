package sv.edu.udb.pokebattle.service;

import java.util.List;
import java.util.UUID;
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
import sv.edu.udb.pokebattle.repository.EquipoRepository;
import sv.edu.udb.pokebattle.repository.JugadorRepository;
import sv.edu.udb.pokebattle.repository.ParticipantePartidaRepository;
import sv.edu.udb.pokebattle.repository.PartidaRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class PartidaService {
    private final PartidaRepository partidaRepository;
    private final ParticipantePartidaRepository participanteRepository;
    private final JugadorRepository jugadorRepository;
    private final EquipoRepository equipoRepository;

    public PartidaResponse crear(CrearPartidaRequest dto) {
        Jugador jugador = buscarJugador(dto.jugadorId());
        Equipo equipo = validarEquipo(dto.equipoId(), jugador.getId());
        Partida partida = new Partida();
        partida.setCodigoSala(generarCodigo());
        partida = partidaRepository.save(partida);
        participanteRepository.save(nuevoParticipante(partida, jugador, equipo));
        return respuesta(partida);
    }

    public PartidaResponse unirse(UUID partidaId, UnirsePartidaRequest dto) {
        Partida partida = buscarPartida(partidaId);
        if (partida.getEstado() != EstadoPartida.ESPERANDO_JUGADOR) {
            throw new ConflictoException("La sala no admite más jugadores");
        }
        if (participanteRepository.findByPartidaId(partidaId).size() >= 2) {
            throw new ConflictoException("La sala está llena");
        }
        Jugador jugador = buscarJugador(dto.jugadorId());
        if (participanteRepository.existsByPartidaIdAndJugadorId(partidaId, jugador.getId())) {
            throw new ConflictoException("El jugador ya participa en la sala");
        }
        Equipo equipo = validarEquipo(dto.equipoId(), jugador.getId());
        participanteRepository.save(nuevoParticipante(partida, jugador, equipo));
        partida.setEstado(EstadoPartida.ESPERANDO_CONFIRMACION);
        return respuesta(partidaRepository.save(partida));
    }

    @Transactional(readOnly = true)
    public PartidaResponse obtener(UUID partidaId) { return respuesta(buscarPartida(partidaId)); }

    private ParticipantePartida nuevoParticipante(Partida partida, Jugador jugador, Equipo equipo) {
        ParticipantePartida participante = new ParticipantePartida();
        participante.setPartida(partida);
        participante.setJugador(jugador);
        participante.setEquipo(equipo);
        return participante;
    }

    private Equipo validarEquipo(UUID equipoId, UUID jugadorId) {
        Equipo equipo = equipoRepository.findById(equipoId).orElseThrow(
                () -> new RecursoNoEncontradoException("Equipo no encontrado"));
        if (!equipo.getJugador().getId().equals(jugadorId)) {
            throw new ReglaNegocioException("El equipo no pertenece al jugador indicado");
        }
        return equipo;
    }

    private Jugador buscarJugador(UUID id) { return jugadorRepository.findById(id).orElseThrow(
            () -> new RecursoNoEncontradoException("Jugador no encontrado")); }
    private Partida buscarPartida(UUID id) { return partidaRepository.findById(id).orElseThrow(
            () -> new RecursoNoEncontradoException("Partida no encontrada")); }

    private String generarCodigo() {
        String codigo;
        do { codigo = UUID.randomUUID().toString().substring(0, 8).toUpperCase(); }
        while (partidaRepository.existsByCodigoSalaIgnoreCase(codigo));
        return codigo;
    }

    private PartidaResponse respuesta(Partida partida) {
        List<ParticipanteResponse> participantes = participanteRepository.findByPartidaId(partida.getId())
                .stream().map(p -> new ParticipanteResponse(p.getJugador().getId(),
                        p.getEquipo().getId(), p.isConfirmado())).toList();
        return new PartidaResponse(partida.getId(), partida.getCodigoSala(), partida.getEstado(),
                partida.getTurnoDe() == null ? null : partida.getTurnoDe().getId(), participantes);
    }
}
