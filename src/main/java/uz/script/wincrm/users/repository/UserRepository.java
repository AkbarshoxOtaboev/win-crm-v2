package uz.script.wincrm.users.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.Status;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = {"roles.permissions", "filial"})
    Optional<User> findByUsername(String username);
    @EntityGraph(attributePaths = {"roles", "filial"})
    Optional<User> findByIdAndStatusNot(Long id, Status status);
    @EntityGraph(attributePaths = {"roles", "filial"})
    List<User> findAllByStatusNot(Status status);
    @EntityGraph(attributePaths = {"roles", "filial"})
    List<User> findAllByStatusNotAndFilial_Id(Status status, Long filialId);
    boolean existsByUsername(String username);
}
