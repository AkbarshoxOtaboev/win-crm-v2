package uz.script.wincrm.goods;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.cutting.CuttingPlanSolver;
import uz.script.wincrm.filial.Filial;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.goods.repository.GoodsCategoryRepository;
import uz.script.wincrm.goods.repository.GoodsRepository;
import uz.script.wincrm.utils.Status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kategoriyasi yo'q oynalarni nomidagi oila bo'yicha bir marta biriktiradi,
 * shunda kesishda kategoriya tanlash darhol ishlaydi.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoodsCategoryBootstrap implements ApplicationRunner {

    private final GoodsRepository goodsRepository;
    private final GoodsCategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, List<Goods>> groups = new LinkedHashMap<>();
        for (Goods goods : goodsRepository.findAll()) {
            if (goods.getType() != Type.WINDOW || goods.getCategory() != null) {
                continue;
            }
            if (goods.getWidth() == null || goods.getHeight() == null) {
                continue;
            }
            long filialId = goods.getFilial() == null || goods.getFilial().getId() == null ? 0L : goods.getFilial().getId();
            String key = filialId + "|" + CuttingPlanSolver.familyKey(goods.getName(), goods.getThickness());
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(goods);
        }
        if (groups.isEmpty()) {
            return;
        }
        int assigned = 0;
        for (List<Goods> list : groups.values()) {
            Goods sample = list.getFirst();
            GoodsCategory category = findOrCreate(label(sample), sample.getFilial());
            for (Goods goods : list) {
                goods.setCategory(category);
                assigned++;
            }
            goodsRepository.saveAll(list);
        }
        log.info("Oyna kategoriyasi biriktirildi: {} ta mahsulot", assigned);
    }

    private GoodsCategory findOrCreate(String name, Filial filial) {
        Long filialId = filial == null ? null : filial.getId();
        return categoryRepository.findAll().stream()
                .filter(category -> category.getName().equalsIgnoreCase(name))
                .filter(category -> sameFilial(category.getFilial(), filialId))
                .findFirst()
                .orElseGet(() -> {
                    GoodsCategory created = GoodsCategory.builder()
                            .name(name)
                            .status(Status.ACTIVE)
                            .filial(filial)
                            .build();
                    return categoryRepository.save(created);
                });
    }

    private boolean sameFilial(Filial filial, Long filialId) {
        Long current = filial == null ? null : filial.getId();
        return current == null ? filialId == null : current.equals(filialId);
    }

    /** "Ok 3 mm 260*180" → "Ok 3 mm". */
    static String label(Goods goods) {
        String name = goods.getName() == null ? "" : goods.getName();
        name = name.replaceAll("\\d+(?:[.,]\\d+)?\\s*[x×х*]\\s*\\d+(?:[.,]\\d+)?", " ");
        name = name.replaceAll("\\s+", " ").trim();
        return name.isBlank() ? "Oyna" : name;
    }
}
