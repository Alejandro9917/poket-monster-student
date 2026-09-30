package sv.edu.udb.pokebattle.repository;
import org.springframework.data.jpa.repository.JpaRepository; import sv.edu.udb.pokebattle.model.Equipo; import java.util.*;
public interface EquipoRepository extends JpaRepository<Equipo, UUID> { List<Equipo> findByJugadorId(UUID jugadorId); }
