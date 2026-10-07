package sv.edu.udb.pokebattle.dto; import jakarta.validation.constraints.NotNull; import java.util.UUID; public record AtacarRequest(@NotNull UUID jugadorId, @NotNull UUID movimientoId) { }
