package uz.script.wincrm.workshop.service;

import uz.script.wincrm.workshop.dto.WorkshopDTO;
import uz.script.wincrm.workshop.response.WorkshopResponse;

import java.util.List;

public interface WorkshopService {
    WorkshopResponse create(WorkshopDTO dto);

    WorkshopResponse findById(Long id);

    List<WorkshopResponse> fetchAll();

    List<WorkshopResponse> fetchActive();

    WorkshopResponse update(Long id, WorkshopDTO dto);

    void delete(Long id);

    WorkshopResponse changeStatus(Long id);
}
