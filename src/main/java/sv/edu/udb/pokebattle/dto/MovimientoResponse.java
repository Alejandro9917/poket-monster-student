package sv.edu.udb.pokebattle.dto;

import java.util.UUID;
import sv.edu.udb.pokebattle.model.MovimientoSeleccionado;

public record MovimientoResponse(
    UUID id,
    Integer movimientoApiId,
    String nombre,
    Integer poder,
    Integer precision,
    String tipo) {
  public static MovimientoResponse desde(MovimientoSeleccionado m) {
    return new MovimientoResponse(
        m.getId(),
        m.getMovimientoApiId(),
        m.getNombre(),
        m.getPoder(),
        m.getPrecisionMovimiento(),
        m.getTipo());
  }
}
