package uz.script.wincrm.sale;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.sale.repository.SaleOrderRepository;

import java.util.List;

/** Eski buyurtmalarga yil bo'yicha 1/26 ko'rinishidagi raqam beradi. */
@Component
@Order(50)
public class SaleOrderNumberBackfill implements ApplicationRunner {

    private final SaleOrderRepository repository;

    public SaleOrderNumberBackfill(SaleOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<SaleOrder> missing = repository.findByOrderSeqIsNull();
        if (missing.isEmpty()) {
            return;
        }
        SaleOrderNumbers.assignMissing(missing, year -> {
            Number max = repository.maxOrderSeq(year);
            return max == null ? 0 : max.intValue();
        });
        repository.saveAll(missing);
    }
}
