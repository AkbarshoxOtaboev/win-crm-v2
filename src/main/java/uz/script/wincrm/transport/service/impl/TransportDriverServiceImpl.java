package uz.script.wincrm.transport.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.transport.TransportDriver;
import uz.script.wincrm.transport.dto.TransportDriverDTO;
import uz.script.wincrm.transport.mapper.TransportMapper;
import uz.script.wincrm.transport.repository.TransportDriverRepository;
import uz.script.wincrm.transport.response.TransportDriverResponse;
import uz.script.wincrm.transport.service.TransportDriverService;
import uz.script.wincrm.utils.Status;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransportDriverServiceImpl implements TransportDriverService {

    private final TransportDriverRepository repository;
    private final TransportMapper mapper;

    @Override
    @Auditable(action = AuditAction.CREATE, entity = "TransportDriver")
    public TransportDriverResponse create(TransportDriverDTO dto) {
        TransportDriver driver = TransportDriver.builder().status(Status.ACTIVE).build();
        apply(driver, dto);
        return mapper.toResponse(repository.save(driver));
    }

    @Override
    public List<TransportDriverResponse> fetchAll() {
        return repository.findAllByOrderByIdAsc().stream().map(mapper::toResponse).toList();
    }

    @Override
    public List<TransportDriverResponse> fetchActive() {
        return repository.findAllByOrderByIdAsc().stream()
                .filter(d -> d.getStatus() == Status.ACTIVE)
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "TransportDriver")
    public TransportDriverResponse update(Long id, TransportDriverDTO dto) {
        TransportDriver driver = getOrThrow(id);
        apply(driver, dto);
        return mapper.toResponse(repository.save(driver));
    }

    @Override
    @Auditable(action = AuditAction.DELETE, entity = "TransportDriver")
    public void delete(Long id) {
        TransportDriver driver = getOrThrow(id);
        driver.setStatus(Status.DELETED);
        repository.save(driver);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "TransportDriver")
    public TransportDriverResponse changeStatus(Long id) {
        TransportDriver driver = getOrThrow(id);
        driver.setStatus(driver.getStatus() == Status.ACTIVE ? Status.DISABLED : Status.ACTIVE);
        return mapper.toResponse(repository.save(driver));
    }

    private void apply(TransportDriver driver, TransportDriverDTO dto) {
        driver.setFullName(dto.getFullName().trim());
        driver.setPhone(dto.getPhone());
        driver.setCarModel(dto.getCarModel());
        driver.setCarNumber(dto.getCarNumber());
        driver.setNote(dto.getNote());
    }

    private TransportDriver getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transport driver not found with id: " + id));
    }
}
