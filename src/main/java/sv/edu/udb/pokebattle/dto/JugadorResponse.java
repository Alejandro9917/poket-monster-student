package sv.edu.udb.pokebattle.dto;

import java.util.UUID;
import sv.edu.udb.pokebattle.model.Jugador;

public record JugadorResponse(UUID id, String nombre) {
  public static JugadorResponse desde(Jugador j) {
    return new JugadorResponse(j.getId(), j.getNombre());
  }
}
