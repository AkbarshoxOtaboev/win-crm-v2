package uz.script.wincrm.workshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.workshop.Workshop;

@Repository
public interface WorkshopRepository extends JpaRepository<Workshop, Long> {
    boolean existsByName(String name);
}
