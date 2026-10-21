package sv.edu.udb.pokebattle.dto;
import jakarta.validation.constraints.*;
public record RegistroRequest(@NotBlank @Size(min=4,max=40) @Pattern(regexp="^[a-zA-Z0-9._-]+$",message="solo puede contener letras, números, punto, guion y guion bajo") String usuario,@NotBlank @Size(min=8,max=72) String password) { }
