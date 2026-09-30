package sv.edu.udb.pokebattle.dto; import java.util.*; public record PokemonDetalleResponse(int id,String nombre,String sprite,Map<String,Integer> estadisticas,List<String> tipos) { }
