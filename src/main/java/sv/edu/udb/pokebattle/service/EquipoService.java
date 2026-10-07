package sv.edu.udb.pokebattle.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.pokebattle.dto.CrearEquipoRequest;
import sv.edu.udb.pokebattle.dto.EquipoResponse;
import sv.edu.udb.pokebattle.exception.RecursoNoEncontradoException;
import sv.edu.udb.pokebattle.model.Equipo;
import sv.edu.udb.pokebattle.repository.EquipoRepository;
import sv.edu.udb.pokebattle.repository.JugadorRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class EquipoService {
    private final EquipoRepository equipoRepository;
    private final JugadorRepository jugadorRepository;

    public EquipoResponse crear(UUID jugadorId, CrearEquipoRequest dto) {
        var jugador = jugadorRepository.findById(jugadorId).orElseThrow(
                () -> new RecursoNoEncontradoException("Jugador no encontrado"));
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
                .map(EquipoResponse::desde).toList();
    }
}
