package uz.script.wincrm.currency.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.response.CbuRateResponse;
import uz.script.wincrm.exceptions.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** O'zbekiston Markaziy banki ochiq API'si: https://cbu.uz/uz/arkhiv-kursov-valyut/json/{CCY}/{yyyy-MM-dd}/ */
@Component
@Slf4j
public class CbuRateClient {

    private static final DateTimeFormatter CBU_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final RestClient client;

    public CbuRateClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder()
                .baseUrl("https://cbu.uz/uz/arkhiv-kursov-valyut/json")
                .requestFactory(factory)
                .build();
    }

    public CbuRateResponse fetch(Currency currency, LocalDate date) {
        List<Map<String, Object>> body;
        try {
            body = client.get()
                    .uri("/{ccy}/{date}/", currency.name(), date.toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            log.warn("CBU rate request failed for {} {}: {}", currency, date, e.getMessage());
            throw new BadRequestException("Markaziy bank kursini olib bo'lmadi. Internet aloqasini tekshiring.");
        }
        if (body == null || body.isEmpty()) {
            throw new BadRequestException("Markaziy bank " + currency + " kursini qaytarmadi");
        }
        Map<String, Object> row = body.getFirst();
        BigDecimal nominal = decimal(row.get("Nominal"), BigDecimal.ONE);
        BigDecimal rate = decimal(row.get("Rate"), null);
        if (rate == null) {
            throw new BadRequestException("Markaziy bank javobida kurs topilmadi");
        }
        if (nominal.signum() > 0 && nominal.compareTo(BigDecimal.ONE) != 0) {
            rate = rate.divide(nominal, 4, RoundingMode.HALF_UP);
        }
        LocalDate rateDate = date;
        Object rawDate = row.get("Date");
        if (rawDate != null) {
            try {
                rateDate = LocalDate.parse(rawDate.toString(), CBU_DATE);
            } catch (RuntimeException ignored) {
                // CBU sana formatini o'zgartirsa, so'ralgan sana qoladi
            }
        }
        return CbuRateResponse.builder()
                .currency(currency)
                .rateDate(rateDate)
                .rate(rate)
                .diff(decimal(row.get("Diff"), null))
                .build();
    }

    private BigDecimal decimal(Object value, BigDecimal fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return new BigDecimal(value.toString().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
