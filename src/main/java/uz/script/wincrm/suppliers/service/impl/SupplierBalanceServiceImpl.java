package uz.script.wincrm.suppliers.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.suppliers.Supplier;
import uz.script.wincrm.suppliers.SupplierBalance;
import uz.script.wincrm.suppliers.dto.SupplierBalanceFilterDTO;
import uz.script.wincrm.suppliers.mapper.SupplierBalanceMapper;
import uz.script.wincrm.suppliers.repository.SupplierBalanceRepository;
import uz.script.wincrm.suppliers.repository.SupplierRepository;
import uz.script.wincrm.suppliers.response.SupplierBalanceResponse;
import uz.script.wincrm.suppliers.service.SupplierBalanceService;
import uz.script.wincrm.suppliers.specification.SupplierBalanceSpecification;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierBalanceServiceImpl implements SupplierBalanceService {

    private final SupplierBalanceRepository balanceRepository;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    @Auditable(action = AuditAction.UPDATE, entity = "SupplierBalance")
    public void increasePurchase(Long supplierId, Currency currency, BigDecimal amount) {

        SupplierBalance balance = getOrCreateBalance(supplierId, currency);

        balance.setTotalPurchase(balance.getTotalPurchase().add(amount));
        balance.setTotalDebt(balance.getTotalDebt().add(amount));
        balance.setLastUpdated(LocalDateTime.now());

        balanceRepository.save(balance);

        log.info("Supplier balance updated: purchase +{} {} (supplierId={})", amount, balance.getCurrency(), supplierId);
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.UPDATE, entity = "SupplierBalance")
    public void decreasePurchase(Long supplierId, Currency currency, BigDecimal amount) {

        SupplierBalance balance = getOrCreateBalance(supplierId, currency);

        balance.setTotalPurchase(balance.getTotalPurchase().subtract(amount));
        balance.setTotalDebt(balance.getTotalDebt().subtract(amount));
        balance.setLastUpdated(LocalDateTime.now());

        balanceRepository.save(balance);

        log.info("Supplier balance updated: purchase -{} {} (supplierId={})", amount, balance.getCurrency(), supplierId);
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.UPDATE, entity = "SupplierBalance")
    public void increasePayment(Long supplierId, Currency currency, BigDecimal amount) {

        SupplierBalance balance = getOrCreateBalance(supplierId, currency);

        balance.setTotalPaid(balance.getTotalPaid().add(amount));
        balance.setTotalDebt(balance.getTotalDebt().subtract(amount));
        balance.setLastUpdated(LocalDateTime.now());

        balanceRepository.save(balance);

        log.info("Supplier balance updated: payment +{} {} (supplierId={})", amount, balance.getCurrency(), supplierId);
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.UPDATE, entity = "SupplierBalance")
    public void decreasePayment(Long supplierId, Currency currency, BigDecimal amount) {

        SupplierBalance balance = getOrCreateBalance(supplierId, currency);

        balance.setTotalPaid(balance.getTotalPaid().subtract(amount));
        balance.setTotalDebt(balance.getTotalDebt().add(amount));
        balance.setLastUpdated(LocalDateTime.now());

        balanceRepository.save(balance);

        log.info("Supplier balance updated: payment -{} {} (supplierId={})", amount, balance.getCurrency(), supplierId);
    }

    // MUHIM: readOnly = true OLIB TASHLANDI.
    // @Auditable aspekti shu transaction ichida audit_logs jadvaliga INSERT qiladi;
    // readOnly=true bo'lsa, PostgreSQL "in a read-only transaction" xatosini beradi.
    @Override
    @Transactional
    @Auditable(action = AuditAction.READ, entity = "SupplierBalance")
    public List<SupplierBalanceResponse> findBySupplierId(Long supplierId) {

        log.info("Fetching balances for supplierId: {}", supplierId);

        if (!supplierRepository.existsById(supplierId)) {
            throw new ResourceNotFoundException("Supplier not found: " + supplierId);
        }
        return balanceRepository.findAllBySupplierIdOrderByCurrencyAsc(supplierId).stream()
                .map(SupplierBalanceMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.READ, entity = "SupplierBalance")
    public Page<SupplierBalanceResponse> findAll(Pageable pageable) {

        log.info("Fetching all supplier balances. Page: {}, Size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<SupplierBalance> balances = balanceRepository.findAll(pageable);

        log.info("Fetched {} supplier balances.", balances.getTotalElements());

        return balances.map(SupplierBalanceMapper::toResponse);
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.READ, entity = "SupplierBalance")
    public Page<SupplierBalanceResponse> filter(SupplierBalanceFilterDTO filter, Pageable pageable) {

        log.info("Filtering supplier balances. Filter: {}, Page: {}, Size: {}",
                filter, pageable.getPageNumber(), pageable.getPageSize());

        Page<SupplierBalance> balances = balanceRepository.findAll(
                SupplierBalanceSpecification.filter(filter),
                pageable
        );

        log.info("Filter completed. Total balances found: {}", balances.getTotalElements());

        return balances.map(SupplierBalanceMapper::toResponse);
    }

    /**
     * Balansni pessimistic lock bilan olib keladi, mavjud bo'lmasa yaratadi.
     * Parallel so'rovlarda ikkita balans yaratilib qolmasligi uchun
     * unique constraint (supplier_id, currency) buzilsa, qayta o'qib olinadi.
     */
    private SupplierBalance getOrCreateBalance(Long supplierId, Currency currency) {
        Currency resolved = currency != null ? currency : Currency.BASE;
        return balanceRepository.findBySupplierIdAndCurrencyForUpdate(supplierId, resolved)
                .orElseGet(() -> createBalance(supplierId, resolved));
    }

    private SupplierBalance createBalance(Long supplierId, Currency currency) {
        try {
            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Supplier not found: " + supplierId));

            SupplierBalance newBalance = SupplierBalance.builder()
                    .supplier(supplier)
                    .currency(currency)
                    .totalPurchase(BigDecimal.ZERO)
                    .totalPaid(BigDecimal.ZERO)
                    .totalDebt(BigDecimal.ZERO)
                    .lastUpdated(LocalDateTime.now())
                    .build();
            newBalance.setStatus(Status.ACTIVE);

            return balanceRepository.saveAndFlush(newBalance);
        } catch (DataIntegrityViolationException e) {
            return balanceRepository.findBySupplierIdAndCurrencyForUpdate(supplierId, currency)
                    .orElseThrow(() -> e);
        }
    }
}
