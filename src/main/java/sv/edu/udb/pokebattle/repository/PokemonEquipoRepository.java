package sv.edu.udb.pokebattle.repository;
import org.springframework.data.jpa.repository.JpaRepository; import sv.edu.udb.pokebattle.model.PokemonEquipo; import java.util.UUID;
public interface PokemonEquipoRepository extends JpaRepository<PokemonEquipo, UUID> { long countByEquipoId(UUID equipoId); }
