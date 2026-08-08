package uz.script.wincrm.salary.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.script.wincrm.salary.dto.SalaryAdjustmentDTO;
import uz.script.wincrm.salary.response.SalaryTransactionResponse;

public interface SalaryTransactionService {

    /** Qo'lda tuzatish (BONUS/DEDUCTION/ADVANCE) qo'shadi. COMMISSION turlarini rad etadi. */
    SalaryTransactionResponse addAdjustment(SalaryAdjustmentDTO dto);

    Page<SalaryTransactionResponse> fetchByUserId(Long userId, Pageable pageable);

    Page<SalaryTransactionResponse> fetchByUserAndPeriod(
            Long userId, Integer year, Integer month, Pageable pageable);

    void delete(Long id);
}
