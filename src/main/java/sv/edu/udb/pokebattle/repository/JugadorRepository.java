package sv.edu.udb.pokebattle.repository;
import org.springframework.data.jpa.repository.JpaRepository; import sv.edu.udb.pokebattle.model.Jugador; import java.util.*;
public interface JugadorRepository extends JpaRepository<Jugador, UUID> { boolean existsByNombreIgnoreCase(String nombre); boolean existsByUsuarioIgnoreCase(String usuario); Optional<Jugador> findByUsuarioIgnoreCase(String usuario); }
