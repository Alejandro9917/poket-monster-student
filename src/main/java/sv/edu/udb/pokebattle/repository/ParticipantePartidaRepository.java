package sv.edu.udb.pokebattle.repository;
import org.springframework.data.jpa.repository.JpaRepository; import sv.edu.udb.pokebattle.model.ParticipantePartida; import java.util.*;
public interface ParticipantePartidaRepository extends JpaRepository<ParticipantePartida, UUID> { List<ParticipantePartida> findByPartidaId(UUID partidaId); Optional<ParticipantePartida> findByPartidaIdAndJugadorId(UUID partidaId, UUID jugadorId); boolean existsByPartidaIdAndJugadorId(UUID partidaId, UUID jugadorId); }
