package uz.script.wincrm.currency.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.ExchangeRate;
import uz.script.wincrm.currency.ExchangeRateSource;
import uz.script.wincrm.currency.repository.ExchangeRateRepository;
import uz.script.wincrm.currency.response.CbuRateResponse;
import uz.script.wincrm.currency.response.ExchangeRateResponse;
import uz.script.wincrm.currency.service.CbuRateClient;
import uz.script.wincrm.currency.service.ExchangeRateService;
import uz.script.wincrm.exceptions.BadRequestException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        List<ExchangeRate> rows = repository.findAllByCurrencyAndRateDateBetweenOrderByRateDateDesc(currency, start, end);
        BigDecimal beforeStart = repository.findFirstByCurrencyAndRateDateLessThanOrderByRateDateDesc(currency, start)
                .map(ExchangeRate::getRate)
                .orElse(null);
        List<ExchangeRateResponse> result = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            BigDecimal previous = i + 1 < rows.size() ? rows.get(i + 1).getRate() : beforeStart;
            result.add(toResponse(rows.get(i), previous));
        }
        return result;
    }

    @Override
    public ExchangeRateResponse current(Currency currency) {
        return on(currency, LocalDate.now());
    }

    @Override
    public ExchangeRateResponse on(Currency currency, LocalDate date) {
        requireForeign(currency);
        LocalDate day = date != null ? date : LocalDate.now();
        Optional<ExchangeRate> row = repository.findFirstByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(currency, day);
        if (row.isEmpty()) {
            try {
                rateOn(currency, day);
            } catch (BadRequestException ignored) {
                return null;
            }
            row = repository.findFirstByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(currency, day);
        }
        return row.map(this::withPrevious).orElse(null);
    }

    @Override
    public ExchangeRateResponse storeCbu(CbuRateResponse cbu) {
        requireForeign(cbu.getCurrency());
        ExchangeRate entity = repository.findByCurrencyAndRateDate(cbu.getCurrency(), cbu.getRateDate())
                .orElseGet(() -> ExchangeRate.builder()
                        .currency(cbu.getCurrency())
                        .rateDate(cbu.getRateDate())
                        .build());
        boolean changed = entity.getId() == null
                || entity.getSource() != ExchangeRateSource.CBU
                || entity.getRate() == null
                || entity.getRate().compareTo(cbu.getRate()) != 0;
        if (changed) {
            entity.setRate(cbu.getRate());
            entity.setSource(ExchangeRateSource.CBU);
            entity = repository.save(entity);
            log.info("CBU rate stored: {} {} = {}", entity.getCurrency(), entity.getRateDate(), entity.getRate());
        }
        return withPrevious(entity);
    }

    @Override
    public ExchangeRateResponse syncLatest(Currency currency) {
        requireForeign(currency);
        // Ertangi sana so'ralsa CBU oxirgi e'lon qilingan kursni (oldindan e'lon qilingan bo'lsa ertangisini) qaytaradi
        storeCbu(cbuRateClient.fetch(currency, LocalDate.now().plusDays(1)));
        return current(currency);
    }

    @Override
    public BigDecimal rateOn(Currency currency, LocalDate date) {
        if (currency == null || currency.isBase()) {
            return BigDecimal.ONE;
        }
        Optional<ExchangeRate> stored = repository.findFirstByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(currency, date);
        if (stored.isPresent()) {
            return stored.get().getRate();
        }
        try {
            CbuRateResponse cbu = cbuRateClient.fetch(currency, date);
            if (!cbu.getRateDate().isAfter(date)) {
                return storeCbu(cbu).getRate();
            }
        } catch (BadRequestException e) {
            log.warn("CBU rate for {} {} unavailable: {}", currency, date, e.getMessage());
        }
        throw new BadRequestException(currency + " kursi hali Markaziy bankdan olinmagan. "
                + "Internet aloqasini tekshirib, «Valyuta» sahifasida «Yangilash» tugmasini bosing.");
    }

    private ExchangeRateResponse withPrevious(ExchangeRate e) {
        BigDecimal previous = repository
                .findFirstByCurrencyAndRateDateLessThanOrderByRateDateDesc(e.getCurrency(), e.getRateDate())
                .map(ExchangeRate::getRate)
                .orElse(null);
        return toResponse(e, previous);
    }

    private void requireForeign(Currency currency) {
        if (currency == null || currency.isBase()) {
            throw new BadRequestException("Kurs faqat xorijiy valyuta uchun yuritiladi (" + Currency.BASE + " asosiy valyuta)");
        }
    }

    private ExchangeRateResponse toResponse(ExchangeRate e, BigDecimal previous) {
        return ExchangeRateResponse.builder()
                .id(e.getId())
                .currency(e.getCurrency())
                .rateDate(e.getRateDate())
                .rate(e.getRate())
                .change(previous != null ? e.getRate().subtract(previous) : null)
                .source(e.getSource())
                .createdUsername(e.getCreatedUsername())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
