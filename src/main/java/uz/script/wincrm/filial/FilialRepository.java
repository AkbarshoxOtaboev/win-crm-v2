package uz.script.wincrm.filial;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.script.wincrm.utils.Status;

import java.util.List;
import java.util.Optional;

public interface FilialRepository extends JpaRepository<Filial, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @EntityGraph(attributePaths = "director")
    Optional<Filial> findByIdAndStatusNot(Long id, Status status);

    @EntityGraph(attributePaths = "director")
    List<Filial> findAllByStatusNotOrderByNameAsc(Status status);
}
