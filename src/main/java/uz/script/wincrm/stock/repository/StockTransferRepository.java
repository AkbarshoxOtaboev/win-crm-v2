package uz.script.wincrm.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.stock.StockTransfer;

import java.util.List;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    List<StockTransfer> findAllByGoodsId(Long goodsId);

    // Ombor yoki manba, yoki maqsad sifatida ishtirok etgan barcha transferlarni qaytaradi
    List<StockTransfer> findAllByFromWarehouseIdOrToWarehouseId(Long fromWarehouseId, Long toWarehouseId);

    @Query("SELECT t FROM StockTransfer t WHERE t.filial.id = :filialId OR t.toFilial.id = :filialId " +
            "OR t.toWarehouse.filial.id = :filialId ORDER BY t.id DESC")
    List<StockTransfer> findAllVisibleForFilial(@Param("filialId") Long filialId);
}
