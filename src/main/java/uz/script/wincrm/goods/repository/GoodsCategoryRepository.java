package uz.script.wincrm.goods.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.goods.GoodsCategory;

@Repository
public interface GoodsCategoryRepository extends JpaRepository<GoodsCategory, Long> {
    boolean existsByNameIgnoreCase(String name);
}
