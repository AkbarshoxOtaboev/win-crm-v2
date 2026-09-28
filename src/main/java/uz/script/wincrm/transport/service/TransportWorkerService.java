package uz.script.wincrm.transport.service;

import uz.script.wincrm.transport.dto.TransportWorkerDTO;
import uz.script.wincrm.transport.response.TransportWorkerResponse;

import java.util.List;

public interface TransportWorkerService {
    TransportWorkerResponse create(TransportWorkerDTO dto);

    List<TransportWorkerResponse> fetchAll();

    List<TransportWorkerResponse> fetchActive();

    TransportWorkerResponse update(Long id, TransportWorkerDTO dto);

    void delete(Long id);

    TransportWorkerResponse changeStatus(Long id);
}
