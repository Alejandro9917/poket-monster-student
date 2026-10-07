package sv.edu.udb.pokebattle.dto; import java.util.*; public record EstadoParticipanteResponse(UUID jugadorId,String nombreJugador,boolean confirmado,List<PokemonBatallaResponse> pokemon) { }
