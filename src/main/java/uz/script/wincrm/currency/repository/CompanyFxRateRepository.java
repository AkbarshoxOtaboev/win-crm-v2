package uz.script.wincrm.currency.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.script.wincrm.currency.CompanyFxRate;
import uz.script.wincrm.currency.Currency;

import java.util.Optional;

public interface CompanyFxRateRepository extends JpaRepository<CompanyFxRate, Long> {

    Optional<CompanyFxRate> findByCurrency(Currency currency);
}
