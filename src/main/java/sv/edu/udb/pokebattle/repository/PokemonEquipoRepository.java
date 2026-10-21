package sv.edu.udb.pokebattle.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.pokebattle.model.PokemonEquipo;

public interface PokemonEquipoRepository extends JpaRepository<PokemonEquipo, UUID> {
  long countByEquipoId(UUID equipoId);
}
