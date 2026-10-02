package uz.script.wincrm.workshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.workshop.Workshop;

import java.util.List;

@Repository
public interface WorkshopRepository extends JpaRepository<Workshop, Long> {
    boolean existsByName(String name);

    @Query("select w.id from Workshop w where w.manager.id = :userId and w.status = uz.script.wincrm.utils.Status.ACTIVE order by w.id")
    List<Long> findActiveIdsByManagerId(@Param("userId") Long userId);
}
