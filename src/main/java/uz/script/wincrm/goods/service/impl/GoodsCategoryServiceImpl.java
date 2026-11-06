package uz.script.wincrm.goods.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.goods.GoodsCategory;
import uz.script.wincrm.goods.dto.GoodsCategoryDTO;
import uz.script.wincrm.goods.mapper.GoodsCategoryMapper;
import uz.script.wincrm.goods.repository.GoodsCategoryRepository;
import uz.script.wincrm.goods.response.GoodsCategoryResponse;
import uz.script.wincrm.goods.service.GoodsCategoryService;
import uz.script.wincrm.utils.Status;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GoodsCategoryServiceImpl implements GoodsCategoryService {

    private final GoodsCategoryRepository repository;
    private final GoodsCategoryMapper mapper;

    @Override
    public GoodsCategoryResponse create(GoodsCategoryDTO dto) {
        if (repository.existsByNameIgnoreCase(dto.getName().trim())) {
            throw new BadRequestException("Bu kategoriya allaqachon bor");
        }
        return mapper.toResponse(repository.save(mapper.toEntity(dto)));
    }

    @Override
    public GoodsCategoryResponse update(Long id, GoodsCategoryDTO dto) {
        GoodsCategory entity = find(id);
        if (!entity.getName().equalsIgnoreCase(dto.getName().trim())
                && repository.existsByNameIgnoreCase(dto.getName().trim())) {
            throw new BadRequestException("Bu kategoriya allaqachon bor");
        }
        mapper.updateEntity(entity, dto);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        GoodsCategory entity = find(id);
        entity.setStatus(Status.DELETED);
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoodsCategoryResponse> getAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    public GoodsCategoryResponse changeStatus(Long id) {
        GoodsCategory entity = find(id);
        entity.setStatus(entity.getStatus() == Status.ACTIVE ? Status.DISABLED : Status.ACTIVE);
        return mapper.toResponse(repository.save(entity));
    }

    private GoodsCategory find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategoriya topilmadi: " + id));
    }
}
