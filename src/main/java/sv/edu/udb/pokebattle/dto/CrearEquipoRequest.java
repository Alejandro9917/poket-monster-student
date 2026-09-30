package sv.edu.udb.pokebattle.dto; import jakarta.validation.constraints.*; public record CrearEquipoRequest(@NotBlank @Size(max=50) String nombre) { }
