package uz.script.wincrm.goods.service;

import uz.script.wincrm.goods.dto.GoodsCategoryDTO;
import uz.script.wincrm.goods.response.GoodsCategoryResponse;

import java.util.List;

public interface GoodsCategoryService {

    GoodsCategoryResponse create(GoodsCategoryDTO dto);

    GoodsCategoryResponse update(Long id, GoodsCategoryDTO dto);

    void delete(Long id);

    List<GoodsCategoryResponse> getAll();

    GoodsCategoryResponse changeStatus(Long id);
}
