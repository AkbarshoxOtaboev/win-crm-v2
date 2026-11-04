package uz.script.wincrm.cutting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.script.wincrm.cutting.GlassRemnant;
import uz.script.wincrm.utils.Status;

import java.util.List;

public interface GlassRemnantRepository extends JpaRepository<GlassRemnant, Long> {

    List<GlassRemnant> findByStatusAndPieceCountGreaterThan(Status status, int pieceCount);
}
