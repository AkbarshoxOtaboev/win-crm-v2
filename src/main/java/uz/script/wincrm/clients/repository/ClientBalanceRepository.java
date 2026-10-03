package uz.script.wincrm.clients.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.clients.ClientBalance;
import uz.script.wincrm.currency.Currency;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientBalanceRepository extends JpaRepository<ClientBalance, Long> {

    List<ClientBalance> findAllByClient_IdOrderByCurrencyAsc(Long clientId);

    Optional<ClientBalance> findByClient_IdAndCurrency(Long clientId, Currency currency);

    boolean existsByClient_Id(Long clientId);
}
