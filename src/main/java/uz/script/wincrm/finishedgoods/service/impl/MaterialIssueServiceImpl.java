package uz.script.wincrm.finishedgoods.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.finishedgoods.MaterialIssue;
import uz.script.wincrm.finishedgoods.MaterialIssueItem;
import uz.script.wincrm.finishedgoods.dto.MaterialIssueDTO;
import uz.script.wincrm.finishedgoods.dto.MaterialIssueItemDTO;
import uz.script.wincrm.finishedgoods.enums.FinishedGoodsDocStatus;
import uz.script.wincrm.finishedgoods.repository.MaterialIssueRepository;
import uz.script.wincrm.finishedgoods.response.MaterialIssueItemResponse;
import uz.script.wincrm.finishedgoods.response.MaterialIssueResponse;
import uz.script.wincrm.finishedgoods.service.MaterialIssueService;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.goods.repository.GoodsRepository;
import uz.script.wincrm.stock.service.StockService;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.warehouse.Warehouse;
import uz.script.wincrm.warehouse.repository.WarehouseRepository;
import uz.script.wincrm.workshop.Workshop;
import uz.script.wincrm.workshop.repository.WorkshopRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MaterialIssueServiceImpl implements MaterialIssueService {

    private final MaterialIssueRepository repository;
    private final WarehouseRepository warehouseRepository;
    private final WorkshopRepository workshopRepository;
    private final GoodsRepository goodsRepository;
    private final StockService stockService;

    @Override
    public MaterialIssueResponse create(MaterialIssueDTO dto) {
        MaterialIssue issue = MaterialIssue.builder()
                .warehouse(requireWarehouse(dto.getWarehouseId()))
                .workshop(resolveWorkshop(dto.getWorkshopId()))
                .comment(dto.getComment())
                .documentDate(dto.getDocumentDate())
                .docStatus(FinishedGoodsDocStatus.DRAFT)
                .items(new ArrayList<>())
                .build();
        applyItems(issue, dto.getItems());
        return toResponse(repository.save(issue));
    }

    @Override
    public MaterialIssueResponse update(Long id, MaterialIssueDTO dto) {
        MaterialIssue issue = requireDraft(id);
        issue.setWarehouse(requireWarehouse(dto.getWarehouseId()));
        issue.setWorkshop(resolveWorkshop(dto.getWorkshopId()));
        issue.setComment(dto.getComment());
        issue.setDocumentDate(dto.getDocumentDate());
        issue.getItems().clear();
        applyItems(issue, dto.getItems());
        return toResponse(repository.save(issue));
    }

    @Override
    public MaterialIssueResponse findById(Long id) {
        return toResponse(require(id));
    }

    @Override
    public List<MaterialIssueResponse> findAll() {
        return repository.findAll().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public MaterialIssueResponse post(Long id) {
        MaterialIssue issue = requireDraft(id);
        if (issue.getItems() == null || issue.getItems().isEmpty()) {
            throw new BadRequestException("Material chiqimda hech qanday pozitsiya yo‘q");
        }
        Long warehouseId = issue.getWarehouse().getId();
        for (MaterialIssueItem item : issue.getItems()) {
            stockService.decreaseStock(
                    item.getGoods().getId(),
                    warehouseId,
                    item.getCount(),
                    item.getPieceCount()
            );
        }
        issue.setDocStatus(FinishedGoodsDocStatus.POSTED);
        log.info("Material issue {} posted, {} items", id, issue.getItems().size());
        return toResponse(repository.save(issue));
    }

    @Override
    public void delete(Long id) {
        MaterialIssue issue = requireDraft(id);
        issue.setStatus(Status.DELETED);
        repository.save(issue);
    }

    private void applyItems(MaterialIssue issue, List<MaterialIssueItemDTO> items) {
        for (MaterialIssueItemDTO dto : items) {
            Goods goods = goodsRepository.findById(dto.getGoodsId())
                    .orElseThrow(() -> new ResourceNotFoundException("Goods not found: " + dto.getGoodsId()));
            if (goods.getType() != Type.PRODUCT && goods.getType() != Type.WINDOW) {
                throw new BadRequestException("Material chiqimga faqat tovar yoki oyna qo‘shiladi: " + goods.getName());
            }
            MaterialIssueItem item = MaterialIssueItem.builder()
                    .materialIssue(issue)
                    .goods(goods)
                    .count(dto.getCount())
                    .pieceCount(dto.getPieceCount())
                    .build();
            issue.getItems().add(item);
        }
    }

    private MaterialIssue require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material issue not found: " + id));
    }

    private MaterialIssue requireDraft(Long id) {
        MaterialIssue issue = require(id);
        if (issue.getDocStatus() == FinishedGoodsDocStatus.POSTED) {
            throw new BadRequestException("Tasdiqlangan material chiqimni o‘zgartirib / o‘chirib bo‘lmaydi");
        }
        return issue;
    }

    private Warehouse requireWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));
    }

    private Workshop resolveWorkshop(Long id) {
        if (id == null) return null;
        return workshopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found: " + id));
    }

    private MaterialIssueResponse toResponse(MaterialIssue issue) {
        List<MaterialIssueItemResponse> items = issue.getItems() == null ? List.of() : issue.getItems().stream()
                .map(i -> MaterialIssueItemResponse.builder()
                        .id(i.getId())
                        .goodsId(i.getGoods().getId())
                        .goodsName(i.getGoods().getName())
                        .goodsType(i.getGoods().getType() != null ? i.getGoods().getType().name() : null)
                        .count(i.getCount())
                        .pieceCount(i.getPieceCount())
                        .build())
                .toList();
        return MaterialIssueResponse.builder()
                .id(issue.getId())
                .warehouseId(issue.getWarehouse().getId())
                .warehouseName(issue.getWarehouse().getName())
                .workshopId(issue.getWorkshop() != null ? issue.getWorkshop().getId() : null)
                .workshopName(issue.getWorkshop() != null ? issue.getWorkshop().getName() : null)
                .comment(issue.getComment())
                .documentDate(issue.getDocumentDate())
                .docStatus(issue.getDocStatus())
                .createdAt(issue.getCreatedAt())
                .createdUsername(issue.getCreatedUsername())
                .items(items)
                .build();
    }
}
