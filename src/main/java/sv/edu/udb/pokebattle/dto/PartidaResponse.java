package sv.edu.udb.pokebattle.dto;

import java.util.List;
import java.util.UUID;
import sv.edu.udb.pokebattle.model.EstadoPartida;

public record PartidaResponse(UUID id, String codigoSala, EstadoPartida estado,
                             UUID turnoDe, List<ParticipanteResponse> participantes) { }
