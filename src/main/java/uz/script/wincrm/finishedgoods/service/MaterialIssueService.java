package uz.script.wincrm.finishedgoods.service;

import uz.script.wincrm.finishedgoods.dto.MaterialIssueDTO;
import uz.script.wincrm.finishedgoods.response.MaterialIssueResponse;

import java.util.List;

public interface MaterialIssueService {
    MaterialIssueResponse create(MaterialIssueDTO dto);

    MaterialIssueResponse update(Long id, MaterialIssueDTO dto);

    MaterialIssueResponse findById(Long id);

    List<MaterialIssueResponse> findAll();

    MaterialIssueResponse post(Long id);

    void delete(Long id);
}
