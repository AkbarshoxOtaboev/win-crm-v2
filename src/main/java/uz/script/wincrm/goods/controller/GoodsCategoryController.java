package uz.script.wincrm.goods.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.script.wincrm.goods.dto.GoodsCategoryDTO;
import uz.script.wincrm.goods.response.GoodsCategoryResponse;
import uz.script.wincrm.goods.service.GoodsCategoryService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/goods-categories")
public class GoodsCategoryController {

    private final GoodsCategoryService service;

    @PostMapping
    @PreAuthorize("hasAuthority('GOODS_CREATE')")
    public ResponseEntity<GoodsCategoryResponse> create(@Valid @RequestBody GoodsCategoryDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GOODS_EDIT')")
    public ResponseEntity<GoodsCategoryResponse> update(@PathVariable Long id, @Valid @RequestBody GoodsCategoryDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('GOODS_EDIT')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GOODS_VIEW', 'SALE_ORDER_VIEW', 'SALE_ORDER_CREATE', 'SALE_ORDER_EDIT')")
    public ResponseEntity<List<GoodsCategoryResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{id}/change-status")
    @PreAuthorize("hasAuthority('GOODS_EDIT')")
    public ResponseEntity<GoodsCategoryResponse> changeStatus(@PathVariable Long id) {
        return ResponseEntity.ok(service.changeStatus(id));
    }
}
