package sv.edu.udb.pokebattle.dto;

import java.util.UUID;

public record ParticipanteResponse(UUID jugadorId, UUID equipoId, boolean confirmado) { }
