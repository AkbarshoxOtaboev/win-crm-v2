package uz.script.wincrm.suppliers.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.service.ExchangeRateService;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.expense.service.SupplierPaymentExpenseSync;
import uz.script.wincrm.payment.PaymentType;
import uz.script.wincrm.payment.repository.PaymentTypeRepository;
import uz.script.wincrm.suppliers.Supplier;
import uz.script.wincrm.suppliers.SupplierPayment;
import uz.script.wincrm.suppliers.dto.SupplierPaymentDTO;
import uz.script.wincrm.suppliers.dto.SupplierPaymentFilterDTO;
import uz.script.wincrm.suppliers.mapper.SupplierPaymentMapper;
import uz.script.wincrm.suppliers.repository.SupplierPaymentRepository;
import uz.script.wincrm.suppliers.repository.SupplierRepository;
import uz.script.wincrm.suppliers.response.SupplierPaymentResponse;
import uz.script.wincrm.suppliers.service.SupplierBalanceService;
import uz.script.wincrm.suppliers.service.SupplierPaymentService;
import uz.script.wincrm.suppliers.specification.SupplierPaymentSpecification;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierPaymentServiceImpl implements SupplierPaymentService {

    private final SupplierPaymentRepository repository;
    private final SupplierRepository supplierRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final SupplierBalanceService supplierBalanceService;
    private final SupplierPaymentExpenseSync expenseSync;
    private final ExchangeRateService exchangeRateService;

    @Override
    @Transactional
    @Auditable(action = AuditAction.CREATE, entity = "SupplierPayment")
    public SupplierPaymentResponse create(SupplierPaymentDTO dto) {

        log.info("Creating supplier payment. SupplierId: {}, Sum: {}",
                dto.getSupplierId(), dto.getPaidSumm());

        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier not found with id: " + dto.getSupplierId()));

        PaymentType paymentType = paymentTypeRepository.findById(dto.getPaymentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment type not found with id: " + dto.getPaymentTypeId()));

        SupplierPayment payment = SupplierPaymentMapper.toEntity(dto, supplier, paymentType);
        payment.setStatus(Status.ACTIVE);
        applyCurrency(payment, dto, paymentType);

        payment = repository.save(payment);

        supplierBalanceService.increasePayment(supplier.getId(), payment.getDebtCurrency(), payment.getAppliedAmount());
        expenseSync.upsert(payment);

        log.info("Supplier payment created successfully. ID: {}", payment.getId());

        return SupplierPaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.UPDATE, entity = "SupplierPayment")
    public SupplierPaymentResponse update(Long id, SupplierPaymentDTO dto) {

        log.info("Updating supplier payment with id: {}", id);

        SupplierPayment payment = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier payment not found with id: " + id));

        PaymentType paymentType = paymentTypeRepository.findById(dto.getPaymentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment type not found with id: " + dto.getPaymentTypeId()));

        // Eslatma: to'lovni boshqa supplierga o'tkazish qo'llab-quvvatlanmaydi,
        // faqat summa/sana/izoh/turi/valyuta o'zgartiriladi.
        Long supplierId = payment.getSupplier().getId();
        Currency oldDebtCurrency = payment.getDebtCurrency();
        BigDecimal oldApplied = payment.appliedOrPaid();

        SupplierPaymentMapper.updateEntity(payment, dto, paymentType);
        applyCurrency(payment, dto, paymentType);

        payment = repository.save(payment);

        supplierBalanceService.decreasePayment(supplierId, oldDebtCurrency, oldApplied);
        supplierBalanceService.increasePayment(supplierId, payment.getDebtCurrency(), payment.getAppliedAmount());
        expenseSync.upsert(payment);

        log.info("Supplier payment updated successfully. ID: {}", payment.getId());

        return SupplierPaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.DELETE, entity = "SupplierPayment")
    public void delete(Long id) {

        log.info("Deleting supplier payment with id: {}", id);

        SupplierPayment payment = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier payment not found with id: " + id));

        payment.setStatus(Status.DELETED);

        repository.save(payment);

        supplierBalanceService.decreasePayment(payment.getSupplier().getId(), payment.getDebtCurrency(), payment.appliedOrPaid());
        expenseSync.remove(payment.getId());

        log.info("Supplier payment deleted successfully. ID: {}", id);
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.READ, entity = "SupplierPayment")
    public SupplierPaymentResponse findById(Long id) {

        log.info("Fetching supplier payment with id: {}", id);

        SupplierPayment payment = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier payment not found with id: " + id));

        return SupplierPaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.READ, entity = "SupplierPayment")
    public Page<SupplierPaymentResponse> findAll(Pageable pageable) {

        log.info("Fetching all supplier payments. Page: {}, Size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<SupplierPayment> payments = repository.findAll(pageable);

        log.info("Fetched {} supplier payments.", payments.getTotalElements());

        return payments.map(SupplierPaymentMapper::toResponse);
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.READ, entity = "SupplierPayment")
    public Page<SupplierPaymentResponse> filter(SupplierPaymentFilterDTO filter, Pageable pageable) {

        log.info("Filtering supplier payments. Filter: {}, Page: {}, Size: {}",
                filter, pageable.getPageNumber(), pageable.getPageSize());

        Page<SupplierPayment> payments = repository.findAll(
                SupplierPaymentSpecification.filter(filter),
                pageable
        );

        log.info("Filter completed. Total payments found: {}", payments.getTotalElements());

        return payments.map(SupplierPaymentMapper::toResponse);
    }

    /**
     * To'lov valyutasi kassadan olinadi; boshqa valyutadagi qarz yopilsa, to'lov kunidagi kurs bilan o'giriladi.
     * Masalan: 25 700 000 so'm, kurs 12 850, qarz USD bo'lsa - $2 000 yopiladi.
     */
    private void applyCurrency(SupplierPayment payment, SupplierPaymentDTO dto, PaymentType paymentType) {
        Currency currency = paymentType.getCurrency() != null ? paymentType.getCurrency() : Currency.BASE;
        Currency debtCurrency = dto.getDebtCurrency() != null ? dto.getDebtCurrency() : currency;

        Currency foreign = !currency.isBase() ? currency : (!debtCurrency.isBase() ? debtCurrency : null);
        if (!currency.isBase() && !debtCurrency.isBase() && currency != debtCurrency) {
            throw new BadRequestException("Ikki xorijiy valyuta orasida to'g'ridan-to'g'ri konvertatsiya qo'llab-quvvatlanmaydi");
        }

        BigDecimal rate = BigDecimal.ONE;
        if (foreign != null) {
            LocalDate date = dto.getPaidDate() != null ? dto.getPaidDate().toLocalDate() : LocalDate.now();
            rate = exchangeRateService.rateOn(foreign, date);
        }

        BigDecimal paid = dto.getPaidSumm();
        BigDecimal applied;
        if (currency == debtCurrency) {
            applied = paid;
        } else if (currency.isBase()) {
            applied = paid.divide(rate, 2, RoundingMode.HALF_UP);
        } else {
            applied = paid.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        }

        payment.setCurrency(currency);
        payment.setDebtCurrency(debtCurrency);
        payment.setExchangeRate(rate);
        payment.setAppliedAmount(applied);
    }
}