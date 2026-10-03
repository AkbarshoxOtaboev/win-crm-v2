package uz.script.wincrm.currency.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.ExchangeRate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyAndRateDate(Currency currency, LocalDate rateDate);

    Optional<ExchangeRate> findFirstByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(Currency currency, LocalDate date);

    List<ExchangeRate> findAllByCurrencyAndRateDateBetweenOrderByRateDateDesc(Currency currency, LocalDate from, LocalDate to);
}
