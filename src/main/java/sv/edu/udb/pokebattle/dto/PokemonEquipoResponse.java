package sv.edu.udb.pokebattle.dto;

import java.util.*;
import sv.edu.udb.pokebattle.model.PokemonEquipo;

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
  public static PokemonEquipoResponse desde(PokemonEquipo p) {
    return new PokemonEquipoResponse(
        p.getId(),
        p.getPokemonApiId(),
        p.getNombre(),
        p.getSprite(),
        p.getHpBase(),
        p.getAtaqueBase(),
        p.getDefensaBase(),
        p.getVelocidadBase(),
        p.getMovimientos().stream().map(MovimientoResponse::desde).toList());
  }
}
