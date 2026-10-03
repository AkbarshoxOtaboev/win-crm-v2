package uz.script.wincrm.currency.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.ExchangeRate;
import uz.script.wincrm.currency.ExchangeRateSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyAndRateDate(Currency currency, LocalDate rateDate);

    Optional<ExchangeRate> findFirstByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(Currency currency, LocalDate date);

    Optional<ExchangeRate> findFirstByCurrencyAndRateDateLessThanOrderByRateDateDesc(Currency currency, LocalDate date);

    Optional<ExchangeRate> findFirstByCurrencyAndSourceAndRateDateGreaterThanEqualOrderByRateDateAsc(
            Currency currency, ExchangeRateSource source, LocalDate from);

    List<ExchangeRate> findAllByCurrencyAndRateDateBetweenOrderByRateDateDesc(Currency currency, LocalDate from, LocalDate to);

    List<ExchangeRate> findAllByCurrencyAndSourceAndRateDateBetween(
            Currency currency, ExchangeRateSource source, LocalDate from, LocalDate to);
}
