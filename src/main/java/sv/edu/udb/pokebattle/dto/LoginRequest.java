package sv.edu.udb.pokebattle.dto;
import jakarta.validation.constraints.NotBlank;
public record LoginRequest(@NotBlank String usuario,@NotBlank String password) { }
