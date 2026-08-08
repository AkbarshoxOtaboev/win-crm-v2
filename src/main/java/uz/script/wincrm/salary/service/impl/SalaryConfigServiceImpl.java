package uz.script.wincrm.salary.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.salary.SalaryConfig;
import uz.script.wincrm.salary.dto.SalaryConfigDTO;
import uz.script.wincrm.salary.mapper.SalaryConfigMapper;
import uz.script.wincrm.salary.repository.SalaryConfigRepository;
import uz.script.wincrm.salary.response.SalaryConfigResponse;
import uz.script.wincrm.salary.service.SalaryConfigService;
import uz.script.wincrm.users.User;
import uz.script.wincrm.users.repository.UserRepository;
import uz.script.wincrm.utils.Status;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SalaryConfigServiceImpl implements SalaryConfigService {

    private final SalaryConfigRepository repository;
    private final SalaryConfigMapper mapper;
    private final UserRepository userRepository;

    /**
     * Yangi konfiguratsiya yaratadi. Mavjud ochiq konfiguratsiya bo'lsa - uni yangi
     * konfiguratsiya kuchga kirishidan bir kun oldin avtomatik yopadi (effective-dating).
     * Shu tarzda tarixiy oylar buzilmaydi.
     */
    @Override
    @Auditable(action = AuditAction.CREATE, entity = "SalaryConfig")
    public SalaryConfigResponse create(SalaryConfigDTO dto) {
        log.info("Create salary config for user {}", dto.getUserId());

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getUserId()));

        LocalDate effectiveFrom = dto.getEffectiveFrom();

        Optional<SalaryConfig> openConfigOpt =
                repository.findFirstByUserIdAndEffectiveToIsNullOrderByEffectiveFromDesc(user.getId());

        if (openConfigOpt.isPresent()) {
            SalaryConfig openConfig = openConfigOpt.get();
            if (!effectiveFrom.isAfter(openConfig.getEffectiveFrom())) {
                throw new BadRequestException(
                        "effectiveFrom must be after the current config's effectiveFrom ("
                                + openConfig.getEffectiveFrom() + ")");
            }
            openConfig.setEffectiveTo(effectiveFrom.minusDays(1));
            repository.save(openConfig);
        }

        SalaryConfig entity = SalaryConfig.builder()
                .user(user)
                .baseSalary(dto.getBaseSalary())
                .commissionType(dto.getCommissionType())
                .commissionValue(dto.getCommissionValue())
                .effectiveFrom(effectiveFrom)
                .effectiveTo(null)
                .status(Status.ACTIVE)
                .build();

        entity = repository.save(entity);
        return mapper.toResponse(entity);
    }

    @Override
    public SalaryConfigResponse findById(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    @Override
    public Page<SalaryConfigResponse> fetchAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    public Page<SalaryConfigResponse> fetchByUserId(Long userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable).map(mapper::toResponse);
    }

    @Override
    public SalaryConfigResponse getCurrentByUserId(Long userId) {
        SalaryConfig config = repository
                .findFirstByUserIdAndEffectiveToIsNullOrderByEffectiveFromDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active salary config for user id: " + userId));
        return mapper.toResponse(config);
    }

    @Override
    @Auditable(action = AuditAction.DELETE, entity = "SalaryConfig")
    public void delete(Long id) {
        SalaryConfig entity = getEntity(id);
        entity.setStatus(Status.DELETED);
        repository.save(entity);
    }

    private SalaryConfig getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salary config not found with id: " + id));
    }
}
