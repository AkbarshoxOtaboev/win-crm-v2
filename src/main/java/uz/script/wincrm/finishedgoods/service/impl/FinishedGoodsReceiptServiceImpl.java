package uz.script.wincrm.finishedgoods.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.finishedgoods.FinishedGoodsReceipt;
import uz.script.wincrm.finishedgoods.FinishedGoodsReceiptItem;
import uz.script.wincrm.finishedgoods.dto.FinishedGoodsReceiptDTO;
import uz.script.wincrm.finishedgoods.dto.FinishedGoodsReceiptItemDTO;
import uz.script.wincrm.finishedgoods.enums.FinishedGoodsDocStatus;
import uz.script.wincrm.finishedgoods.repository.FinishedGoodsReceiptRepository;
import uz.script.wincrm.finishedgoods.response.FinishedGoodsReceiptItemResponse;
import uz.script.wincrm.finishedgoods.response.FinishedGoodsReceiptResponse;
import uz.script.wincrm.finishedgoods.service.FinishedGoodsReceiptService;
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
public class FinishedGoodsReceiptServiceImpl implements FinishedGoodsReceiptService {

    private final FinishedGoodsReceiptRepository repository;
    private final WarehouseRepository warehouseRepository;
    private final WorkshopRepository workshopRepository;
    private final GoodsRepository goodsRepository;
    private final StockService stockService;

    @Override
    public FinishedGoodsReceiptResponse create(FinishedGoodsReceiptDTO dto) {
        FinishedGoodsReceipt receipt = FinishedGoodsReceipt.builder()
                .warehouse(requireWarehouse(dto.getWarehouseId()))
                .workshop(resolveWorkshop(dto.getWorkshopId()))
                .comment(dto.getComment())
                .documentDate(dto.getDocumentDate())
                .docStatus(FinishedGoodsDocStatus.DRAFT)
                .items(new ArrayList<>())
                .build();
        applyItems(receipt, dto.getItems());
        return toResponse(repository.save(receipt));
    }

    @Override
    public FinishedGoodsReceiptResponse update(Long id, FinishedGoodsReceiptDTO dto) {
        FinishedGoodsReceipt receipt = requireDraft(id);
        receipt.setWarehouse(requireWarehouse(dto.getWarehouseId()));
        receipt.setWorkshop(resolveWorkshop(dto.getWorkshopId()));
        receipt.setComment(dto.getComment());
        receipt.setDocumentDate(dto.getDocumentDate());
        receipt.getItems().clear();
        applyItems(receipt, dto.getItems());
        return toResponse(repository.save(receipt));
    }

    @Override
    public FinishedGoodsReceiptResponse findById(Long id) {
        return toResponse(require(id));
    }

    @Override
    public List<FinishedGoodsReceiptResponse> findAll() {
        return repository.findAll().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public FinishedGoodsReceiptResponse post(Long id) {
        FinishedGoodsReceipt receipt = requireDraft(id);
        if (receipt.getItems() == null || receipt.getItems().isEmpty()) {
            throw new BadRequestException("Tayyor mahsulot prixodida hech qanday pozitsiya yo‘q");
        }
        Long warehouseId = receipt.getWarehouse().getId();
        for (FinishedGoodsReceiptItem item : receipt.getItems()) {
            stockService.increaseStock(
                    item.getGoods().getId(),
                    warehouseId,
                    item.getCount(),
                    item.getCount(),
                    item.getUnitCost()
            );
        }
        receipt.setDocStatus(FinishedGoodsDocStatus.POSTED);
        log.info("Finished goods receipt {} posted, {} items", id, receipt.getItems().size());
        return toResponse(repository.save(receipt));
    }

    @Override
    public void delete(Long id) {
        FinishedGoodsReceipt receipt = requireDraft(id);
        receipt.setStatus(Status.DELETED);
        repository.save(receipt);
    }

    private void applyItems(FinishedGoodsReceipt receipt, List<FinishedGoodsReceiptItemDTO> items) {
        for (FinishedGoodsReceiptItemDTO dto : items) {
            Goods goods = goodsRepository.findById(dto.getGoodsId())
                    .orElseThrow(() -> new ResourceNotFoundException("Goods not found: " + dto.getGoodsId()));
            if (goods.getType() != Type.FINISHED_PRODUCT) {
                throw new BadRequestException("Prixodga faqat tayyor mahsulot qo‘shiladi: " + goods.getName());
            }
            FinishedGoodsReceiptItem item = FinishedGoodsReceiptItem.builder()
                    .receipt(receipt)
                    .goods(goods)
                    .count(dto.getCount())
                    .unitCost(dto.getUnitCost())
                    .build();
            receipt.getItems().add(item);
        }
    }

    private FinishedGoodsReceipt require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Finished goods receipt not found: " + id));
    }

    private FinishedGoodsReceipt requireDraft(Long id) {
        FinishedGoodsReceipt receipt = require(id);
        if (receipt.getDocStatus() == FinishedGoodsDocStatus.POSTED) {
            throw new BadRequestException("Tasdiqlangan prixodni o‘zgartirib / o‘chirib bo‘lmaydi");
        }
        return receipt;
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

    private FinishedGoodsReceiptResponse toResponse(FinishedGoodsReceipt receipt) {
        List<FinishedGoodsReceiptItemResponse> items = receipt.getItems() == null ? List.of() : receipt.getItems().stream()
                .map(i -> FinishedGoodsReceiptItemResponse.builder()
                        .id(i.getId())
                        .goodsId(i.getGoods().getId())
                        .goodsName(i.getGoods().getName())
                        .count(i.getCount())
                        .unitCost(i.getUnitCost())
                        .build())
                .toList();
        return FinishedGoodsReceiptResponse.builder()
                .id(receipt.getId())
                .warehouseId(receipt.getWarehouse().getId())
                .warehouseName(receipt.getWarehouse().getName())
                .workshopId(receipt.getWorkshop() != null ? receipt.getWorkshop().getId() : null)
                .workshopName(receipt.getWorkshop() != null ? receipt.getWorkshop().getName() : null)
                .comment(receipt.getComment())
                .documentDate(receipt.getDocumentDate())
                .docStatus(receipt.getDocStatus())
                .createdAt(receipt.getCreatedAt())
                .createdUsername(receipt.getCreatedUsername())
                .items(items)
                .build();
    }
}
