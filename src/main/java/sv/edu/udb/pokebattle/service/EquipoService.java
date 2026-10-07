package sv.edu.udb.pokebattle.service;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.pokebattle.dto.CrearEquipoRequest;
import sv.edu.udb.pokebattle.dto.EquipoResponse;
import sv.edu.udb.pokebattle.dto.GuardarPokemonRequest;
import sv.edu.udb.pokebattle.dto.PokemonEquipoResponse;
import sv.edu.udb.pokebattle.client.PokeApiClient;
import sv.edu.udb.pokebattle.exception.ConflictoException;
import sv.edu.udb.pokebattle.exception.ReglaNegocioException;
import sv.edu.udb.pokebattle.exception.RecursoNoEncontradoException;
import sv.edu.udb.pokebattle.model.Equipo;
import sv.edu.udb.pokebattle.model.PokemonEquipo;
import sv.edu.udb.pokebattle.model.MovimientoSeleccionado;
import sv.edu.udb.pokebattle.repository.EquipoRepository;
import sv.edu.udb.pokebattle.repository.JugadorRepository;
import sv.edu.udb.pokebattle.repository.PokemonEquipoRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class EquipoService {
    private final EquipoRepository equipoRepository;
    private final JugadorRepository jugadorRepository;
    private final PokemonEquipoRepository pokemonRepository;
    private final PokeApiClient pokeApi;

    public EquipoResponse crear(UUID jugadorId, CrearEquipoRequest dto) {
        var jugador = jugadorRepository.findById(jugadorId).orElseThrow(
                () -> new RecursoNoEncontradoException("Jugador no encontrado"));
        Equipo equipo = new Equipo();
        equipo.setNombre(dto.nombre().trim());
        equipo.setJugador(jugador);
        return EquipoResponse.desde(equipoRepository.save(equipo));
    }

    @Transactional(readOnly = true)
    public List<EquipoResponse> listar(UUID jugadorId) {
        if (!jugadorRepository.existsById(jugadorId)) {
            throw new RecursoNoEncontradoException("Jugador no encontrado");
        }
        return equipoRepository.findByJugadorId(jugadorId).stream()
                .map(EquipoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public EquipoResponse obtener(UUID equipoId) { return EquipoResponse.desde(equipo(equipoId)); }

    public PokemonEquipoResponse agregar(UUID equipoId, GuardarPokemonRequest dto) {
        Equipo equipo = equipo(equipoId);
        if (pokemonRepository.countByEquipoId(equipoId) >= 3) throw new ConflictoException("El equipo ya contiene tres Pokémon");
        return PokemonEquipoResponse.desde(pokemonRepository.save(construir(equipo, dto)));
    }

    public PokemonEquipoResponse cambiar(UUID equipoId, UUID pokemonId, GuardarPokemonRequest dto) {
        PokemonEquipo actual = pokemonRepository.findById(pokemonId).filter(p -> p.getEquipo().getId().equals(equipoId)).orElseThrow(() -> new RecursoNoEncontradoException("Pokémon no encontrado"));
        PokemonEquipo datos = construir(actual.getEquipo(), dto);
        actual.setPokemonApiId(datos.getPokemonApiId()); actual.setNombre(datos.getNombre()); actual.setSprite(datos.getSprite()); actual.setHpBase(datos.getHpBase()); actual.setAtaqueBase(datos.getAtaqueBase()); actual.setDefensaBase(datos.getDefensaBase()); actual.setVelocidadBase(datos.getVelocidadBase()); actual.getMovimientos().clear();
        datos.getMovimientos().forEach(m -> { m.setPokemon(actual); actual.getMovimientos().add(m); });
        return PokemonEquipoResponse.desde(pokemonRepository.save(actual));
    }

    private PokemonEquipo construir(Equipo equipo, GuardarPokemonRequest dto) {
        Set<Integer> solicitados = new HashSet<>(dto.movimientosApiId());
        if (solicitados.size() != dto.movimientosApiId().size()) throw new ReglaNegocioException("No se permiten movimientos repetidos");
        var externo = pokeApi.pokemon(dto.pokemonApiId().toString());
        Set<Integer> permitidos = externo.moves().stream().map(x -> id(x.move().url())).collect(java.util.stream.Collectors.toSet());
        if (!permitidos.containsAll(solicitados)) throw new ReglaNegocioException("Uno o más movimientos no están disponibles para el Pokémon");
        Map<String,Integer> stats = externo.stats().stream().collect(java.util.stream.Collectors.toMap(x -> x.stat().name(), PokeApiClient.Estadistica::baseStat));
        PokemonEquipo p = new PokemonEquipo(); p.setEquipo(equipo); p.setPokemonApiId(externo.id()); p.setNombre(externo.name()); p.setSprite(externo.sprites().frontDefault()); p.setHpBase(stats.get("hp")); p.setAtaqueBase(stats.get("attack")); p.setDefensaBase(stats.get("defense")); p.setVelocidadBase(stats.get("speed"));
        solicitados.forEach(movId -> { var ext = pokeApi.movimiento(movId); MovimientoSeleccionado m = new MovimientoSeleccionado(); m.setPokemon(p); m.setMovimientoApiId(ext.id()); m.setNombre(ext.name()); m.setPoder(ext.power()); m.setPrecisionMovimiento(ext.accuracy()); m.setTipo(ext.type().name()); p.getMovimientos().add(m); });
        return p;
    }
    private Equipo equipo(UUID id) { return equipoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado")); }
    private int id(String url) { String[] p=url.replaceAll("/$","").split("/"); return Integer.parseInt(p[p.length-1]); }
}
