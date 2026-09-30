package sv.edu.udb.pokebattle.dto; import jakarta.validation.constraints.*; public record CrearJugadorRequest(@NotBlank @Size(max=40) String nombre) { }
