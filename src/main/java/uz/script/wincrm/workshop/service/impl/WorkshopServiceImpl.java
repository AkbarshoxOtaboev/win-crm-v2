package uz.script.wincrm.workshop.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.exceptions.AlreadyExistsException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.users.User;
import uz.script.wincrm.users.repository.UserRepository;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.workshop.Workshop;
import uz.script.wincrm.workshop.dto.WorkshopDTO;
import uz.script.wincrm.workshop.mapper.WorkshopMapper;
import uz.script.wincrm.workshop.repository.WorkshopRepository;
import uz.script.wincrm.workshop.response.WorkshopResponse;
import uz.script.wincrm.workshop.service.WorkshopService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WorkshopServiceImpl implements WorkshopService {

    private final WorkshopRepository repository;
    private final WorkshopMapper mapper;
    private final UserRepository userRepository;

    @Override
    @Auditable(action = AuditAction.CREATE, entity = "Workshop")
    public WorkshopResponse create(WorkshopDTO dto) {
        if (repository.existsByName(dto.getName())) {
            throw new AlreadyExistsException("error.workshop.name.exists", dto.getName());
        }
        Workshop workshop = mapper.toEntity(dto);
        applyManager(workshop, dto.getManagerId());
        return mapper.toResponse(repository.save(workshop));
    }

    @Override
    public WorkshopResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    @Override
    public List<WorkshopResponse> fetchAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    public List<WorkshopResponse> fetchActive() {
        return repository.findAll().stream()
                .filter(w -> w.getStatus() == Status.ACTIVE)
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "Workshop")
    public WorkshopResponse update(Long id, WorkshopDTO dto) {
        Workshop workshop = getOrThrow(id);
        if (!workshop.getName().equals(dto.getName()) && repository.existsByName(dto.getName())) {
            throw new AlreadyExistsException("error.workshop.name.exists", dto.getName());
        }
        mapper.updateEntity(workshop, dto);
        applyManager(workshop, dto.getManagerId());
        return mapper.toResponse(repository.save(workshop));
    }

    @Override
    @Auditable(action = AuditAction.DELETE, entity = "Workshop")
    public void delete(Long id) {
        Workshop workshop = getOrThrow(id);
        workshop.setStatus(Status.DELETED);
        repository.save(workshop);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "Workshop")
    public WorkshopResponse changeStatus(Long id) {
        Workshop workshop = getOrThrow(id);
        workshop.setStatus(workshop.getStatus() == Status.ACTIVE ? Status.DISABLED : Status.ACTIVE);
        return mapper.toResponse(repository.save(workshop));
    }

    private Workshop getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found with id: " + id));
    }

    private void applyManager(Workshop workshop, Long managerId) {
        if (managerId == null) {
            workshop.setManager(null);
            return;
        }
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + managerId));
        workshop.setManager(manager);
    }
}
