package sv.edu.udb.pokebattle.repository;
import org.springframework.data.jpa.repository.JpaRepository; import sv.edu.udb.pokebattle.model.Jugador; import java.util.UUID;
public interface JugadorRepository extends JpaRepository<Jugador, UUID> { boolean existsByNombreIgnoreCase(String nombre); }
