package uz.script.wincrm.suppliers.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.suppliers.dto.SupplierBalanceFilterDTO;
import uz.script.wincrm.suppliers.response.SupplierBalanceResponse;

import java.math.BigDecimal;
import java.util.List;

public interface SupplierBalanceService {

    /**
     * WarehouseOrder yaratilganda chaqiriladi (ichki metod); summa hujjat valyutasida
     */
    void increasePurchase(Long supplierId, Currency currency, BigDecimal amount);

    /**
     * WarehouseOrder o'chirilganda/kamaytirilganda chaqiriladi (ichki metod)
     */
    void decreasePurchase(Long supplierId, Currency currency, BigDecimal amount);

    /**
     * SupplierPayment yaratilganda chaqiriladi (ichki metod); summa yopilgan qarz valyutasida
     */
    void increasePayment(Long supplierId, Currency currency, BigDecimal amount);

    /**
     * SupplierPayment o'chirilganda/kamaytirilganda chaqiriladi (ichki metod)
     */
    void decreasePayment(Long supplierId, Currency currency, BigDecimal amount);

    /**
     * Bitta supplierning barcha valyutalardagi balanslari
     */
    List<SupplierBalanceResponse> findBySupplierId(Long supplierId);

    /**
     * Barcha balanslarni sahifalab olish
     */
    Page<SupplierBalanceResponse> findAll(Pageable pageable);

    /**
     * Balanslarni filtrlash
     */
    Page<SupplierBalanceResponse> filter(SupplierBalanceFilterDTO filter, Pageable pageable);
}
