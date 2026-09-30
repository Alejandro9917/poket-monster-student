package sv.edu.udb.pokebattle.repository;
import org.springframework.data.jpa.repository.JpaRepository; import sv.edu.udb.pokebattle.model.PokemonBatalla; import java.util.UUID;
public interface PokemonBatallaRepository extends JpaRepository<PokemonBatalla, UUID> { }
