package uz.script.wincrm.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.warehouse.WarehouseOrder;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface WarehouseOrderRepository extends JpaRepository<WarehouseOrder, Long> {
    @Query("""
            select o from WarehouseOrder o
            join fetch o.supplier
            order by o.id desc
            """)
    List<WarehouseOrder> findAllWithSupplier();

    List<WarehouseOrder> findAllByWarehouseId(Long warehouseId);
    List<WarehouseOrder> findAllBySupplierId(Long supplierId);

    @Query("""
            select coalesce(sum(coalesce(o.totalSum, 0)), 0)
            from WarehouseOrder o
            where o.supplier.id = :supplierId and o.currency = :currency
            """)
    BigDecimal sumTotalSumBySupplierAndCurrency(@Param("supplierId") Long supplierId,
                                                @Param("currency") Currency currency);

    @Query("""
            select distinct o.currency from WarehouseOrder o
            where o.supplier.id = :supplierId
            """)
    List<Currency> findCurrenciesBySupplierId(@Param("supplierId") Long supplierId);
}
