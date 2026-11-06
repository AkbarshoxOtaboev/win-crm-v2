package uz.script.wincrm.finishedgoods.service;

import uz.script.wincrm.finishedgoods.dto.FinishedGoodsReceiptDTO;
import uz.script.wincrm.finishedgoods.response.FinishedGoodsReceiptResponse;

import java.util.List;

public interface FinishedGoodsReceiptService {
    FinishedGoodsReceiptResponse create(FinishedGoodsReceiptDTO dto);

    FinishedGoodsReceiptResponse update(Long id, FinishedGoodsReceiptDTO dto);

    FinishedGoodsReceiptResponse findById(Long id);

    List<FinishedGoodsReceiptResponse> findAll();

    FinishedGoodsReceiptResponse post(Long id);

    void delete(Long id);
}
