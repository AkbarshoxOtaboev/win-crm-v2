package uz.script.wincrm.currency.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uz.script.wincrm.currency.CompanyFxRate;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.dto.CompanyFxRateRequest;
import uz.script.wincrm.currency.repository.CompanyFxRateRepository;
import uz.script.wincrm.currency.response.CompanyFxRateResponse;
import uz.script.wincrm.currency.response.ExchangeRateResponse;
import uz.script.wincrm.currency.service.CompanyFxRateService;
import uz.script.wincrm.currency.service.ExchangeRateService;
import uz.script.wincrm.exceptions.BadRequestException;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyFxRateServiceImpl implements CompanyFxRateService {

    private final CompanyFxRateRepository repository;
    private final ExchangeRateService exchangeRateService;

    @Override
    public List<CompanyFxRateResponse> fetchAll() {
        return Arrays.stream(Currency.values())
                .filter(c -> !c.isBase())
                .map(c -> toResponse(c, repository.findByCurrency(c).orElse(null)))
                .toList();
    }

    @Override
    public CompanyFxRateResponse save(Currency currency, CompanyFxRateRequest request) {
        if (currency == null || currency.isBase()) {
            throw new BadRequestException("Kurs faqat xorijiy valyuta uchun belgilanadi");
        }
        if (request.getBuyRate().compareTo(request.getSellRate()) > 0) {
            throw new BadRequestException("Olish kursi sotish kursidan katta bo'lishi mumkin emas");
        }
        CompanyFxRate entity = repository.findByCurrency(currency)
                .orElseGet(() -> CompanyFxRate.builder().currency(currency).build());
        entity.setBuyRate(request.getBuyRate());
        entity.setSellRate(request.getSellRate());
        entity.setUpdatedUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        return toResponse(currency, repository.saveAndFlush(entity));
    }

    private CompanyFxRateResponse toResponse(Currency currency, CompanyFxRate entity) {
        ExchangeRateResponse cbu = exchangeRateService.current(currency);
        return CompanyFxRateResponse.builder()
                .currency(currency)
                .buyRate(entity != null ? entity.getBuyRate() : null)
                .sellRate(entity != null ? entity.getSellRate() : null)
                .updatedUsername(entity != null ? entity.getUpdatedUsername() : null)
                .updatedAt(entity != null ? entity.getUpdatedAt() : null)
                .cbuRate(cbu != null ? cbu.getRate() : null)
                .cbuChange(cbu != null ? cbu.getChange() : null)
                .cbuRateDate(cbu != null ? cbu.getRateDate() : null)
                .build();
    }
}
