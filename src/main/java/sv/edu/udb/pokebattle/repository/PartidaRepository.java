package sv.edu.udb.pokebattle.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import sv.edu.udb.pokebattle.model.Partida;

public interface PartidaRepository extends JpaRepository<Partida, UUID> {
  Optional<Partida> findByCodigoSalaIgnoreCase(String codigoSala);

  boolean existsByCodigoSalaIgnoreCase(String codigoSala);
}
