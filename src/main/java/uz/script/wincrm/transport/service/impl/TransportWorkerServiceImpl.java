package uz.script.wincrm.transport.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.transport.TransportWorker;
import uz.script.wincrm.transport.dto.TransportWorkerDTO;
import uz.script.wincrm.transport.mapper.TransportMapper;
import uz.script.wincrm.transport.repository.TransportWorkerRepository;
import uz.script.wincrm.transport.response.TransportWorkerResponse;
import uz.script.wincrm.transport.service.TransportWorkerService;
import uz.script.wincrm.utils.Status;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransportWorkerServiceImpl implements TransportWorkerService {

    private final TransportWorkerRepository repository;
    private final TransportMapper mapper;

    @Override
    @Auditable(action = AuditAction.CREATE, entity = "TransportWorker")
    public TransportWorkerResponse create(TransportWorkerDTO dto) {
        TransportWorker worker = TransportWorker.builder().status(Status.ACTIVE).build();
        apply(worker, dto);
        return mapper.toResponse(repository.save(worker));
    }

    @Override
    public List<TransportWorkerResponse> fetchAll() {
        return repository.findAllByOrderByIdAsc().stream().map(mapper::toResponse).toList();
    }

    @Override
    public List<TransportWorkerResponse> fetchActive() {
        return repository.findAllByOrderByIdAsc().stream()
                .filter(w -> w.getStatus() == Status.ACTIVE)
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "TransportWorker")
    public TransportWorkerResponse update(Long id, TransportWorkerDTO dto) {
        TransportWorker worker = getOrThrow(id);
        apply(worker, dto);
        return mapper.toResponse(repository.save(worker));
    }

    @Override
    @Auditable(action = AuditAction.DELETE, entity = "TransportWorker")
    public void delete(Long id) {
        TransportWorker worker = getOrThrow(id);
        worker.setStatus(Status.DELETED);
        repository.save(worker);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "TransportWorker")
    public TransportWorkerResponse changeStatus(Long id) {
        TransportWorker worker = getOrThrow(id);
        worker.setStatus(worker.getStatus() == Status.ACTIVE ? Status.DISABLED : Status.ACTIVE);
        return mapper.toResponse(repository.save(worker));
    }

    private void apply(TransportWorker worker, TransportWorkerDTO dto) {
        worker.setFullName(dto.getFullName().trim());
        worker.setPhone(dto.getPhone());
        worker.setNote(dto.getNote());
    }

    private TransportWorker getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transport worker not found with id: " + id));
    }
}
