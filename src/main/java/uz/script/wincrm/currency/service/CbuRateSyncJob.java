package uz.script.wincrm.currency.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.ExchangeRate;
import uz.script.wincrm.currency.ExchangeRateSource;
import uz.script.wincrm.currency.repository.ExchangeRateRepository;
import uz.script.wincrm.currency.response.CbuRateResponse;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Markaziy bank kurslarini avtomatik yig'adi: har soatda oxirgi e'lon qilingan kurs, ishga tushganda va har tunda
 * tarix (birinchi marta - bir yillik, keyin oxirgi ikki hafta bo'shliqlari).
 * CBU har kuni kurs belgilamaydi: belgilanmagan kun so'ralsa oxirgi belgilangan kurs qaytadi.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CbuRateSyncJob {

    private static final int HISTORY_DAYS = 365;
    private static final int CATCH_UP_DAYS = 14;
    private static final long REQUEST_PAUSE_MS = 150;

    private final CbuRateClient client;
    private final ExchangeRateService rates;
    private final ExchangeRateRepository repository;

    private final AtomicBoolean backfillRunning = new AtomicBoolean();
    private volatile Thread backfillThread;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        startBackfill();
    }

    @PreDestroy
    void stop() {
        Thread thread = backfillThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Scheduled(cron = "0 5 * * * *")
    public void syncLatest() {
        for (Currency currency : foreignCurrencies()) {
            try {
                rates.syncLatest(currency);
            } catch (RuntimeException e) {
                log.warn("CBU hourly sync failed for {}: {}", currency, e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 30 0 * * *")
    public void nightlyBackfill() {
        startBackfill();
    }

    private void startBackfill() {
        if (!backfillRunning.compareAndSet(false, true)) {
            return;
        }
        backfillThread = Thread.ofVirtual().name("cbu-rate-backfill").start(() -> {
            try {
                for (Currency currency : foreignCurrencies()) {
                    backfill(currency);
                }
            } finally {
                backfillRunning.set(false);
            }
        });
    }

    private void backfill(Currency currency) {
        LocalDate today = LocalDate.now();
        LocalDate historyStart = today.minusDays(HISTORY_DAYS);
        boolean historyLoaded = repository
                .findFirstByCurrencyAndSourceAndRateDateGreaterThanEqualOrderByRateDateAsc(currency, ExchangeRateSource.CBU, historyStart)
                .map(oldest -> !oldest.getRateDate().isAfter(historyStart.plusDays(7)))
                .orElse(false);
        LocalDate limit = historyLoaded ? today.minusDays(CATCH_UP_DAYS) : historyStart;

        Set<LocalDate> known = new HashSet<>();
        repository.findAllByCurrencyAndSourceAndRateDateBetween(currency, ExchangeRateSource.CBU, limit, today.plusDays(1))
                .stream().map(ExchangeRate::getRateDate).forEach(known::add);

        int fetched = 0;
        LocalDate cursor = today.plusDays(1);
        while (!cursor.isBefore(limit)) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            if (known.contains(cursor)) {
                cursor = cursor.minusDays(1);
                continue;
            }
            CbuRateResponse cbu;
            try {
                cbu = client.fetch(currency, cursor);
                rates.storeCbu(cbu);
            } catch (RuntimeException e) {
                log.warn("CBU backfill stopped for {} at {}: {}", currency, cursor, e.getMessage());
                return;
            }
            fetched++;
            known.add(cbu.getRateDate());
            cursor = cbu.getRateDate().isBefore(cursor) ? cbu.getRateDate().minusDays(1) : cursor.minusDays(1);
            try {
                Thread.sleep(REQUEST_PAUSE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (fetched > 0) {
            log.info("CBU backfill for {} done: {} requests, from {}", currency, fetched, limit);
        }
    }

    private List<Currency> foreignCurrencies() {
        return Arrays.stream(Currency.values()).filter(c -> !c.isBase()).toList();
    }
}
