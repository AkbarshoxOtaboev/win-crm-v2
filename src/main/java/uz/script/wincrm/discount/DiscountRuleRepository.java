package uz.script.wincrm.discount;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRuleRepository extends JpaRepository<DiscountRule, Long> {
    Optional<DiscountRule> findByRole_Id(Long roleId);

    List<DiscountRule> findAllByRole_IdIn(Collection<Long> roleIds);
}
