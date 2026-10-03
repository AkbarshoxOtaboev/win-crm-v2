package uz.script.wincrm.currency.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.ExchangeRate;
import uz.script.wincrm.currency.ExchangeRateSource;
import uz.script.wincrm.currency.dto.ExchangeRateDTO;
import uz.script.wincrm.currency.repository.ExchangeRateRepository;
import uz.script.wincrm.currency.response.CbuRateResponse;
import uz.script.wincrm.currency.response.ExchangeRateResponse;
import uz.script.wincrm.currency.service.CbuRateClient;
import uz.script.wincrm.currency.service.ExchangeRateService;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private static final int DEFAULT_HISTORY_DAYS = 60;

    private final ExchangeRateRepository repository;
    private final CbuRateClient cbuRateClient;

    @Override
    public List<ExchangeRateResponse> history(Currency currency, LocalDate from, LocalDate to) {
        requireForeign(currency);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_HISTORY_DAYS);
        if (start.isAfter(end)) {
            throw new BadRequestException("Boshlanish sanasi tugash sanasidan keyin bo'lishi mumkin emas");
        }
        return repository.findAllByCurrencyAndRateDateBetweenOrderByRateDateDesc(currency, start, end)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public ExchangeRateResponse current(Currency currency) {
        requireForeign(currency);
        return repository.findFirstByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(currency, LocalDate.now())
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    public ExchangeRateResponse save(ExchangeRateDTO dto) {
        requireForeign(dto.getCurrency());
        if (dto.getRateDate().isAfter(LocalDate.now().plusDays(1))) {
            throw new BadRequestException("Kursni faqat bugun va ertangi kun uchun oldindan kiritish mumkin");
        }
        ExchangeRate entity = repository.findByCurrencyAndRateDate(dto.getCurrency(), dto.getRateDate())
                .orElseGet(() -> ExchangeRate.builder()
                        .currency(dto.getCurrency())
                        .rateDate(dto.getRateDate())
                        .build());
        entity.setRate(dto.getRate());
        entity.setSource(dto.getSource() != null ? dto.getSource() : ExchangeRateSource.MANUAL);
        entity = repository.save(entity);
        log.info("Exchange rate {} {} = {} ({})", entity.getCurrency(), entity.getRateDate(), entity.getRate(), entity.getSource());
        return toResponse(entity);
    }

    @Override
    public void delete(Long id) {
        ExchangeRate entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exchange rate not found with id: " + id));
        repository.delete(entity);
        log.info("Exchange rate {} {} deleted", entity.getCurrency(), entity.getRateDate());
    }

    @Override
    public CbuRateResponse cbu(Currency currency, LocalDate date) {
        requireForeign(currency);
        return cbuRateClient.fetch(currency, date != null ? date : LocalDate.now());
    }

    @Override
    public BigDecimal rateOn(Currency currency, LocalDate date) {
        if (currency == null || currency.isBase()) {
            return BigDecimal.ONE;
        }
        return repository.findFirstByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(currency, date)
                .map(ExchangeRate::getRate)
                .orElseThrow(() -> new BadRequestException(
                        currency + " kursi kiritilmagan. Sozlamalar → Valyuta kurslari bo'limida kursni kiriting."));
    }

    private void requireForeign(Currency currency) {
        if (currency == null || currency.isBase()) {
            throw new BadRequestException("Kurs faqat xorijiy valyuta uchun yuritiladi (" + Currency.BASE + " asosiy valyuta)");
        }
    }

    private ExchangeRateResponse toResponse(ExchangeRate e) {
        return ExchangeRateResponse.builder()
                .id(e.getId())
                .currency(e.getCurrency())
                .rateDate(e.getRateDate())
                .rate(e.getRate())
                .source(e.getSource())
                .createdUsername(e.getCreatedUsername())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
