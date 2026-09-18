package uz.script.wincrm.filial;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.exceptions.AlreadyExistsException;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.roles.Role;
import uz.script.wincrm.roles.RoleRepository;
import uz.script.wincrm.users.User;
import uz.script.wincrm.users.repository.UserRepository;
import uz.script.wincrm.utils.Status;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class FilialServiceImpl implements FilialService {

    private final FilialRepository repository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FilialAccess filialAccess;

    @Override
    @Auditable(action = AuditAction.CREATE, entity = "Filial")
    public FilialResponse create(FilialDTO dto) {
        filialAccess.requireSuperAdmin();
        String name = dto.getName().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new AlreadyExistsException("error.filial.name.exists");
        }
        Filial filial = Filial.builder()
                .name(name)
                .address(blankToNull(dto.getAddress()))
                .phone(blankToNull(dto.getPhone()))
                .status(Status.ACTIVE)
                .build();
        filial = repository.save(filial);
        if (dto.getDirectorId() != null) {
            assignDirectorInternal(filial, dto.getDirectorId());
        }
        return toResponse(filial);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "Filial")
    public FilialResponse update(Long id, FilialDTO dto) {
        filialAccess.requireSuperAdmin();
        Filial filial = getActive(id);
        String name = dto.getName().trim();
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new AlreadyExistsException("error.filial.name.exists");
        }
        filial.setName(name);
        filial.setAddress(blankToNull(dto.getAddress()));
        filial.setPhone(blankToNull(dto.getPhone()));
        if (dto.getDirectorId() != null) {
            assignDirectorInternal(filial, dto.getDirectorId());
        } else if (filial.getDirector() != null) {
            User previous = filial.getDirector();
            if (previous.getFilial() != null && previous.getFilial().getId().equals(filial.getId())) {
                previous.setFilial(null);
            }
            filial.setDirector(null);
        }
        return toResponse(filial);
    }

    @Override
    @Auditable(action = AuditAction.DELETE, entity = "Filial")
    public void delete(Long id) {
        filialAccess.requireSuperAdmin();
        Filial filial = getActive(id);
        if (filial.getDirector() != null) {
            User director = filial.getDirector();
            if (director.getFilial() != null && director.getFilial().getId().equals(filial.getId())) {
                director.setFilial(null);
            }
            filial.setDirector(null);
        }
        filial.setStatus(Status.DELETED);
    }

    @Override
    public FilialResponse findById(Long id) {
        if (!filialAccess.isSuperAdmin()) {
            Long current = filialAccess.currentFilialId();
            if (current == null || !current.equals(id)) {
                throw new ResourceNotFoundException("Filial not found with id: " + id);
            }
        }
        return toResponse(getActive(id));
    }

    @Override
    public List<FilialResponse> fetchAll() {
        if (!filialAccess.isSuperAdmin()) {
            Long current = filialAccess.currentFilialId();
            if (current == null || current <= 0) {
                return List.of();
            }
            return List.of(toResponse(getActive(current)));
        }
        return repository.findAllByStatusNotOrderByNameAsc(Status.DELETED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "Filial")
    public FilialResponse assignDirector(Long filialId, Long directorId) {
        filialAccess.requireSuperAdmin();
        Filial filial = getActive(filialId);
        assignDirectorInternal(filial, directorId);
        return toResponse(filial);
    }

    private void assignDirectorInternal(Filial filial, Long directorId) {
        User director = userRepository.findByIdAndStatusNot(directorId, Status.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + directorId));
        if (director.getRoles() != null && director.getRoles().stream()
                .anyMatch(role -> "SUPER_ADMIN".equals(role.getName()))) {
            throw new BadRequestException("Super adminni filial direktori qilib tayinlab bo'lmaydi");
        }

        User previous = filial.getDirector();
        if (previous != null && !previous.getId().equals(director.getId())
                && previous.getFilial() != null
                && previous.getFilial().getId().equals(filial.getId())) {
            previous.setFilial(null);
        }

        director.setFilial(filial);
        ensureDirectorRole(director);
        filial.setDirector(director);
    }

    private void ensureDirectorRole(User user) {
        Role directorRole = roleRepository.findByName("DIRECTOR")
                .orElseThrow(() -> new ResourceNotFoundException("DIRECTOR role not found"));
        Set<Role> roles = user.getRoles() == null ? new HashSet<>() : new HashSet<>(user.getRoles());
        boolean hasDirector = roles.stream().anyMatch(role -> "DIRECTOR".equals(role.getName()));
        if (!hasDirector) {
            roles.add(directorRole);
            user.setRoles(roles);
        }
    }

    private Filial getActive(Long id) {
        return repository.findByIdAndStatusNot(id, Status.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Filial not found with id: " + id));
    }

    private FilialResponse toResponse(Filial filial) {
        User director = filial.getDirector();
        return FilialResponse.builder()
                .id(filial.getId())
                .name(filial.getName())
                .address(filial.getAddress())
                .phone(filial.getPhone())
                .directorId(director != null ? director.getId() : null)
                .directorFullName(director != null ? director.getFullName() : null)
                .directorUsername(director != null ? director.getUsername() : null)
                .status(filial.getStatus())
                .createdAt(filial.getCreatedAt())
                .updatedAt(filial.getUpdatedAt())
                .build();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
