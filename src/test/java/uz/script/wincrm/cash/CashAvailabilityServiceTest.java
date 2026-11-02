package uz.script.wincrm.cash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.script.wincrm.cash.repository.CashHandoverRepository;
import uz.script.wincrm.cash.service.CashAvailabilityService;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.expense.repository.ExpenseRepository;
import uz.script.wincrm.payment.PaymentType;
import uz.script.wincrm.payment.repository.PaymentRepository;
import uz.script.wincrm.payment.repository.PaymentTypeRepository;
import uz.script.wincrm.suppliers.repository.SupplierPaymentRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Operatsion vs Rahbar (VAULT) qoldiq formulalari.
 * Filial scoping Hibernate filter orqali — bu yerda xulosa hisobi tekshiriladi.
 */
@ExtendWith(MockitoExtension.class)
class CashAvailabilityServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock SupplierPaymentRepository supplierPaymentRepository;
    @Mock CashHandoverRepository cashHandoverRepository;
    @Mock ExpenseRepository expenseRepository;
    @Mock PaymentTypeRepository paymentTypeRepository;

    @InjectMocks CashAvailabilityService service;

    @Test
    void operationalSubtractsHandoversAndOperationalSpend() {
        when(paymentRepository.sumCashByType(any(), any()))
                .thenReturn(rows(1L, Currency.UZS, "1000"));
        when(supplierPaymentRepository.sumCashByType(any(), any(), eq(CashSource.OPERATIONAL)))
                .thenReturn(rows(1L, Currency.UZS, "100"));
        when(cashHandoverRepository.sumByType(any(), any(), any()))
                .thenReturn(rows(1L, Currency.UZS, "300"));
        when(expenseRepository.sumCashByType(any(), any(), eq(CashSource.OPERATIONAL)))
                .thenReturn(rows(1L, Currency.UZS, "50"));

        BigDecimal available = service.available(1L, Currency.UZS, CashSource.OPERATIONAL);
        // 1000 - 100 - 300 - 50 = 550
        assertEquals(0, new BigDecimal("550").compareTo(available));
    }

    @Test
    void vaultIsAcceptedMinusVaultSpend() {
        when(cashHandoverRepository.sumAcceptedByType(any(), any()))
                .thenReturn(rows(1L, Currency.UZS, "400"));
        when(supplierPaymentRepository.sumCashByType(any(), any(), eq(CashSource.VAULT)))
                .thenReturn(rows(1L, Currency.UZS, "50"));
        when(expenseRepository.sumCashByType(any(), any(), eq(CashSource.VAULT)))
                .thenReturn(rows(1L, Currency.UZS, "25"));

        BigDecimal vault = service.available(1L, Currency.UZS, CashSource.VAULT);
        // 400 - 50 - 25 = 325
        assertEquals(0, new BigDecimal("325").compareTo(vault));
    }

    @Test
    void balancesExposeBothPoolsPerPaymentType() {
        PaymentType type = PaymentType.builder().id(1L).name("Naqd").currency(Currency.UZS).build();
        when(paymentTypeRepository.findAll()).thenReturn(List.of(type));

        when(paymentRepository.sumCashByType(any(), any()))
                .thenReturn(rows(1L, Currency.UZS, "1000"));
        when(supplierPaymentRepository.sumCashByType(any(), any(), eq(CashSource.OPERATIONAL)))
                .thenReturn(List.of());
        when(cashHandoverRepository.sumByType(any(), any(), any()))
                .thenReturn(rows(1L, Currency.UZS, "200"));
        when(expenseRepository.sumCashByType(any(), any(), eq(CashSource.OPERATIONAL)))
                .thenReturn(List.of());

        when(cashHandoverRepository.sumAcceptedByType(any(), any()))
                .thenReturn(rows(1L, Currency.UZS, "200"));
        when(supplierPaymentRepository.sumCashByType(any(), any(), eq(CashSource.VAULT)))
                .thenReturn(List.of());
        when(expenseRepository.sumCashByType(any(), any(), eq(CashSource.VAULT)))
                .thenReturn(List.of());

        var list = service.balances();
        assertEquals(1, list.size());
        assertEquals(0, new BigDecimal("800").compareTo(list.getFirst().getOperationalAvailable()));
        assertEquals(0, new BigDecimal("200").compareTo(list.getFirst().getVaultAvailable()));
        assertEquals(list.getFirst().getOperationalAvailable(), list.getFirst().getAvailable());
    }

    private static List<Object[]> rows(Long typeId, Currency currency, String amount) {
        return List.<Object[]>of(new Object[]{typeId, currency, new BigDecimal(amount)});
    }
}
