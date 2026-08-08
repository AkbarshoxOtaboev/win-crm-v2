package uz.script.wincrm.salary.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.script.wincrm.salary.dto.SalaryConfigDTO;
import uz.script.wincrm.salary.response.SalaryConfigResponse;

public interface SalaryConfigService {

    SalaryConfigResponse create(SalaryConfigDTO dto);

    SalaryConfigResponse findById(Long id);

    Page<SalaryConfigResponse> fetchAll(Pageable pageable);

    Page<SalaryConfigResponse> fetchByUserId(Long userId, Pageable pageable);

    SalaryConfigResponse getCurrentByUserId(Long userId);

    void delete(Long id);
}
