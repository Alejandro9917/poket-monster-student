package sv.edu.udb.pokebattle.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sv.edu.udb.pokebattle.dto.CrearEquipoRequest;
import sv.edu.udb.pokebattle.dto.EquipoResponse;
import sv.edu.udb.pokebattle.dto.GuardarPokemonRequest;
import sv.edu.udb.pokebattle.dto.PokemonEquipoResponse;
import sv.edu.udb.pokebattle.service.EquipoService;

@RestController
@RequiredArgsConstructor
public class EquipoController {
    private final EquipoService service;

    @PostMapping("/api/jugadores/{jugadorId}/equipos")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoResponse crear(@PathVariable UUID jugadorId,
                                @Valid @RequestBody CrearEquipoRequest dto) {
        return service.crear(jugadorId, dto);
    }

    @GetMapping("/api/jugadores/{jugadorId}/equipos")
    public List<EquipoResponse> listar(@PathVariable UUID jugadorId) {
        return service.listar(jugadorId);
    }

    @GetMapping("/api/equipos/{equipoId}")
    public EquipoResponse obtener(@PathVariable UUID equipoId) { return service.obtener(equipoId); }

    @PostMapping("/api/equipos/{equipoId}/pokemon")
    @ResponseStatus(HttpStatus.CREATED)
    public PokemonEquipoResponse agregar(@PathVariable UUID equipoId, @Valid @RequestBody GuardarPokemonRequest dto) { return service.agregar(equipoId, dto); }

    @PutMapping("/api/equipos/{equipoId}/pokemon/{pokemonId}")
    public PokemonEquipoResponse cambiar(@PathVariable UUID equipoId, @PathVariable UUID pokemonId, @Valid @RequestBody GuardarPokemonRequest dto) { return service.cambiar(equipoId, pokemonId, dto); }
}
