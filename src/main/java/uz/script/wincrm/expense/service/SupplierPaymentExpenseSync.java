package uz.script.wincrm.expense.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.expense.Expense;
import uz.script.wincrm.expense.ExpenseCategory;
import uz.script.wincrm.expense.repository.ExpenseCategoryRepository;
import uz.script.wincrm.expense.repository.ExpenseRepository;
import uz.script.wincrm.filial.Filial;
import uz.script.wincrm.suppliers.SupplierPayment;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Yetkazib beruvchiga qilingan har bir to'lovni "Yetkazib beruvchilarga to'lov" kategoriyasidagi
 * xarajat sifatida aks ettiradi. Chaqiruvchi tranzaksiyasi ichida ishlaydi.
 */
@Component
@RequiredArgsConstructor
public class SupplierPaymentExpenseSync {

    public static final String CATEGORY_NAME = "Yetkazib beruvchilarga to'lov";
    private static final int DESCRIPTION_MAX = 500;

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;

    public void upsert(SupplierPayment payment) {
        Expense expense = expenseRepository.findBySupplierPaymentId(payment.getId())
                .orElseGet(() -> Expense.builder()
                        .supplierPaymentId(payment.getId())
                        .category(resolveCategory(payment.getFilial()))
                        .filial(payment.getFilial())
                        .build());

        expense.setAmount(baseAmount(payment));
        expense.setExpenseDate(payment.getPaidDate() != null ? payment.getPaidDate().toLocalDate() : LocalDate.now());
        expense.setDescription(describe(payment));
        expense.setStatus(Status.ACTIVE);
        expenseRepository.save(expense);
    }

    public void remove(Long supplierPaymentId) {
        expenseRepository.findBySupplierPaymentId(supplierPaymentId).ifPresent(expense -> {
            expense.setStatus(Status.DELETED);
            expenseRepository.save(expense);
        });
    }

    private ExpenseCategory resolveCategory(Filial filial) {
        Long filialId = filial != null ? filial.getId() : null;
        return (filialId != null
                ? categoryRepository.findFirstByNameIgnoreCaseAndFilial_IdOrderByIdAsc(CATEGORY_NAME, filialId)
                : categoryRepository.findFirstByNameIgnoreCaseAndFilialIsNullOrderByIdAsc(CATEGORY_NAME))
                .orElseGet(() -> {
                    ExpenseCategory category = ExpenseCategory.builder()
                            .name(CATEGORY_NAME)
                            .description("Yetkazib beruvchilarga qilingan to'lovlar (avtomatik)")
                            .filial(filial)
                            .build();
                    category.setStatus(Status.ACTIVE);
                    return categoryRepository.save(category);
                });
    }

    /** Xarajatlar so'mda yuritiladi: xorijiy valyutadagi to'lov to'lov kunidagi kurs bilan o'giriladi. */
    private static BigDecimal baseAmount(SupplierPayment payment) {
        Currency currency = payment.getCurrency();
        if (currency == null || currency.isBase() || payment.getExchangeRate() == null) {
            return payment.getPaidSumm();
        }
        return payment.getPaidSumm().multiply(payment.getExchangeRate()).setScale(2, RoundingMode.HALF_UP);
    }

    private static String describe(SupplierPayment payment) {
        String supplier = payment.getSupplier() != null ? payment.getSupplier().getName() : "—";
        String text = "Yetkazib beruvchi: " + supplier;
        Currency currency = payment.getCurrency();
        if (currency != null && !currency.isBase()) {
            text += " · " + plain(payment.getPaidSumm()) + " " + currency + " × " + plain(payment.getExchangeRate());
        }
        if (payment.getComment() != null && !payment.getComment().isBlank()) {
            text += " · " + payment.getComment().trim();
        }
        return text.length() > DESCRIPTION_MAX ? text.substring(0, DESCRIPTION_MAX) : text;
    }

    private static String plain(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
    }
}
