package sv.edu.udb.pokebattle.dto;

import java.util.*;
import sv.edu.udb.pokebattle.model.Equipo;

public record EquipoResponse(
    UUID id, String nombre, UUID jugadorId, List<PokemonEquipoResponse> pokemon) {
  public static EquipoResponse desde(Equipo e) {
    return new EquipoResponse(
        e.getId(),
        e.getNombre(),
        e.getJugador().getId(),
        e.getPokemon().stream().map(PokemonEquipoResponse::desde).toList());
  }
}
