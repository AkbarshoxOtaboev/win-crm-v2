package uz.script.wincrm.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.stock.Stock;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByGoodsIdAndWarehouseId(Long goodsId, Long warehouseId);
    List<Stock> findAllByWarehouseId(Long warehouseId);
    List<Stock> findAllByGoodsId(Long goodsId);

    @Query("SELECT s FROM Stock s JOIN FETCH s.goods g LEFT JOIN FETCH s.warehouse w ORDER BY g.id ASC, w.id ASC, s.id ASC")
    List<Stock> findAllOrderedByGoodsId();
}