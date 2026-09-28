package uz.script.wincrm.transport.service;

import uz.script.wincrm.transport.dto.TransportDriverDTO;
import uz.script.wincrm.transport.response.TransportDriverResponse;

import java.util.List;

public interface TransportDriverService {
    TransportDriverResponse create(TransportDriverDTO dto);

    List<TransportDriverResponse> fetchAll();

    List<TransportDriverResponse> fetchActive();

    TransportDriverResponse update(Long id, TransportDriverDTO dto);

    void delete(Long id);

    TransportDriverResponse changeStatus(Long id);
}
