package uz.script.wincrm.expense.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.cash.service.CashAvailabilityService;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.currency.service.ExchangeRateService;
import uz.script.wincrm.dashboard.responses.DailyExpenseReportResponse;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.expense.Expense;
import uz.script.wincrm.expense.ExpenseCategory;
import uz.script.wincrm.expense.dto.ExpenseDTO;
import uz.script.wincrm.expense.mapper.ExpenseMapper;
import uz.script.wincrm.expense.repository.ExpenseCategoryRepository;
import uz.script.wincrm.expense.repository.ExpenseRepository;
import uz.script.wincrm.expense.response.ExpenseResponse;
import uz.script.wincrm.expense.service.ExpenseService;
import uz.script.wincrm.payment.PaymentType;
import uz.script.wincrm.payment.repository.PaymentTypeRepository;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository repository;
    private final ExpenseMapper mapper;
    private final ExpenseCategoryRepository categoryRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final CashAvailabilityService cashAvailabilityService;
    private final ExchangeRateService exchangeRateService;

    @Override
    @Auditable(
            action = AuditAction.CREATE,
            entity = "Expense"
    )
    public ExpenseResponse create(ExpenseDTO dto) {
        log.info("Create expense for category {}", dto.getCategoryId());

        ExpenseCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with id: " + dto.getCategoryId()));
        PaymentType type = checkCash(dto.getPaymentTypeId(), dto.getAmount(), null);

        Expense entity = mapper.toEntity(dto);
        entity.setCategory(category);
        entity.setStatus(Status.ACTIVE);
        applyCashSource(entity, type);

        entity = repository.save(entity);

        return mapper.toResponse(entity);
    }

    @Override
    public ExpenseResponse findById(Long id) {
        log.info("Fetch expense by id {}", id);

        Expense entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        return mapper.toResponse(entity);
    }

    @Override
    public Page<ExpenseResponse> fetchAll(Pageable pageable) {
        log.info("Fetch all expenses");

        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<ExpenseResponse> fetchByCategoryId(Long categoryId, Pageable pageable) {
        log.info("Fetch expenses by category id {}", categoryId);

        return repository.findByCategoryId(categoryId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public List<ExpenseResponse> fetchByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Fetch expenses by date range {} - {}", startDate, endDate);

        return repository.findByExpenseDateBetween(startDate, endDate)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public Page<ExpenseResponse> fetchByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("Fetch expenses (paginated) by date range {} - {}", startDate, endDate);

        return repository.findByExpenseDateBetween(startDate, endDate, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Auditable(
            action = AuditAction.UPDATE,
            entity = "Expense"
    )
    public ExpenseResponse update(Long id, ExpenseDTO dto) {
        log.info("Update expense with id {}", id);

        Expense entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        ensureNotSupplierPayment(entity);

        CashSpend previous = CashSpend.of(entity);
        PaymentType type = checkCash(dto.getPaymentTypeId() != null ? dto.getPaymentTypeId() : previous.paymentTypeId(),
                dto.getAmount() != null ? dto.getAmount() : entity.getAmount(), previous);

        if (dto.getCategoryId() != null
                && (entity.getCategory() == null || !dto.getCategoryId().equals(entity.getCategory().getId()))) {
            ExpenseCategory category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Expense category not found with id: " + dto.getCategoryId()));
            entity.setCategory(category);
        }

        mapper.updateEntity(entity, dto);
        applyCashSource(entity, type);

        entity = repository.save(entity);

        return mapper.toResponse(entity);
    }

    @Override
    @Auditable(
            action = AuditAction.DELETE,
            entity = "Expense"
    )
    public void delete(Long id) {
        log.info("Delete expense with id {}", id);

        Expense entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        ensureNotSupplierPayment(entity);

        entity.setStatus(Status.DELETED);
        repository.save(entity);
    }

    /** Xarajat oldin qaysi kassadan qancha olgani: tahrirda shu summa kassaga qaytgan deb hisoblanadi. */
    private record CashSpend(Long paymentTypeId, Currency currency, BigDecimal amount) {
        static CashSpend of(Expense e) {
            return new CashSpend(e.getPaymentType() != null ? e.getPaymentType().getId() : null,
                    CurrencyMath.orBase(e.getCurrency()), e.getAmount());
        }
    }

    /**
     * Kassada shu valyutada yetarli qoldiq borligini tekshiradi. Entity o'zgartirilishidan oldin chaqiriladi:
     * aks holda so'rovdan oldingi auto-flush yangi summani qoldiqdan allaqachon ayirib qo'yadi.
     */
    private PaymentType checkCash(Long paymentTypeId, BigDecimal amount, CashSpend previous) {
        if (paymentTypeId == null) {
            throw new BadRequestException("Xarajat qaysi kassadan qilinishini tanlang");
        }
        PaymentType type = paymentTypeRepository.findById(paymentTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment type not found with id: " + paymentTypeId));
        Currency currency = CurrencyMath.orBase(type.getCurrency());

        BigDecimal available = cashAvailabilityService.available(type.getId(), currency);
        if (previous != null && type.getId().equals(previous.paymentTypeId()) && currency == previous.currency()) {
            available = available.add(previous.amount());
        }
        if (amount.compareTo(available) > 0) {
            throw new BadRequestException("Kassada yetarli mablag' yo'q: \"" + type.getName() + "\" kassasida "
                    + plain(available.max(BigDecimal.ZERO)) + " " + currency
                    + " bor, xarajat " + plain(amount) + " " + currency);
        }
        return type;
    }

    /** Xarajat valyutasi kassa valyutasidan olinadi; xorijiy valyutada xarajat kunidagi kurs qotiriladi. */
    private void applyCashSource(Expense entity, PaymentType type) {
        Currency currency = CurrencyMath.orBase(type.getCurrency());
        entity.setPaymentType(type);
        entity.setCurrency(currency);
        entity.setExchangeRate(currency.isBase() ? null : rateOrNull(currency, entity.getExpenseDate()));
    }

    private BigDecimal rateOrNull(Currency currency, LocalDate date) {
        try {
            return exchangeRateService.rateOn(currency, date);
        } catch (BadRequestException e) {
            return null;
        }
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private void ensureNotSupplierPayment(Expense entity) {
        if (entity.getSupplierPaymentId() != null) {
            throw new BadRequestException(
                    "Bu xarajat yetkazib beruvchiga to'lovdan yaratilgan. Uni Yetkazib beruvchilar bo'limida o'zgartiring.");
        }
    }

    @Override
    public List<DailyExpenseReportResponse> findExpenseReportByExpenesCategory(
            LocalDate startDate,
            LocalDate endDate
    ) {

        List<Expense> expenses = repository.findExpenseReportByExpenesCategory(startDate, endDate);

        return expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .values()
                .stream()
                .map(list -> {

                    ExpenseCategory category = list.getFirst().getCategory();

                    return DailyExpenseReportResponse.builder()
                            .categoryId(category.getId())
                            .categoryName(category.getName())
                            .totalAmount(
                                    list.stream()
                                            .map(Expense::getAmount)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            )
                            .build();
                })
                .toList();
    }
}