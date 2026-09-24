package uz.script.wincrm.users.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.exceptions.AlreadyExistsException;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ForbiddenException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.filial.Filial;
import uz.script.wincrm.filial.FilialAccess;
import uz.script.wincrm.filial.FilialRepository;
import uz.script.wincrm.payment.repository.PaymentRepository;
import uz.script.wincrm.roles.Role;
import uz.script.wincrm.roles.RoleRepository;
import uz.script.wincrm.roles.RoleResponse;
import uz.script.wincrm.sale.repository.SaleOrderRepository;
import uz.script.wincrm.storage.StorageService;
import uz.script.wincrm.users.*;
import uz.script.wincrm.users.dto.UserDTO;
import uz.script.wincrm.users.repository.UserRepository;
import uz.script.wincrm.users.response.UserResponse;
import uz.script.wincrm.users.response.UserStatResponse;
import uz.script.wincrm.users.service.UserService;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImplement implements UserService {

    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final SaleOrderRepository saleOrderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final FilialRepository filialRepository;
    private final FilialAccess filialAccess;
    @Override
//    @Caching(evict = {
//            @CacheEvict(value = "users", allEntries = true)
//    })
    @Auditable(
            action = AuditAction.CREATE,
            entity = "User"
    )
    public UserResponse create(UserDTO dto) {

        log.info("Create user {}", dto.getUsername());

        if (repository.existsByUsername(dto.getUsername())) {
            throw new AlreadyExistsException("error.user.username.exists", dto.getUsername());
        }

        Set<Role> roles = new HashSet<>(roleRepository.findAllById(dto.getRoleIds()));

        if (roles.size() != dto.getRoleIds().size()) {
            throw new BadRequestException("One or more roles not found");
        }
        assertAssignableRoles(roles);

        String photoLink = null;

        if (dto.getPhoto() != null && !dto.getPhoto().isEmpty()) {
            photoLink = "/api/files/" + storageService.uploadFile(dto.getPhoto());
        }

        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .roles(roles)
                .photoLink(photoLink)
                .filial(resolveFilial(dto.getFilialId()))
                .status(Status.ACTIVE)
                .build();

        repository.save(user);

        log.info("User {} successfully created", user.getUsername());

        return mapUserToUserResponse(user);
    }

    @Override
//    @Cacheable(value = "user", key = "#id")
    public UserResponse findById(Long id) {

        log.info("Find user by id {}", id);

        User user = repository.findByIdAndStatusNot(id, Status.DELETED)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id " + id));

        return mapUserToUserResponse(user);
    }

    @Override
//    @Cacheable(value = "users")
    public List<UserResponse> fetchAllUsers() {

        log.info("Fetch all users");

        List<User> users;
        if (!filialAccess.isSuperAdmin()) {
            Long filialId = filialAccess.currentFilialId();
            if (filialId == null || filialId <= 0) {
                return List.of();
            }
            users = repository.findAllByStatusNotAndFilial_Id(Status.DELETED, filialId);
        } else {
            users = repository.findAllByStatusNot(Status.DELETED);
        }

        return users.stream()
                .map(this::mapUserToUserResponse)
                .toList();
    }

    @Override
//    @Caching(evict = {
//            @CacheEvict(value = "user", key = "#id"),
//            @CacheEvict(value = "users", allEntries = true)
//    })
    @Auditable(
            action = AuditAction.UPDATE,
            entity = "User"
    )
    public UserResponse update(Long id, UserDTO dto) {

        log.info("Update user {}", id);

        User user = repository.findByIdAndStatusNot(id, Status.DELETED)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id " + id));

        Set<Role> roles = new HashSet<>(roleRepository.findAllById(dto.getRoleIds()));

        if (roles.size() != dto.getRoleIds().size()) {
            throw new BadRequestException("One or more roles not found");
        }
        assertAssignableRoles(roles);
        assertCanManageUser(user);

        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setRoles(roles);
        user.setFilial(resolveFilial(dto.getFilialId()));

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getPhoto() != null && !dto.getPhoto().isEmpty()) {

            if (user.getPhotoLink() != null) {
                storageService.delete(user.getPhotoLink().replace("/api/files/", ""));
            }

            user.setPhotoLink(
                    "/api/files/" + storageService.uploadFile(dto.getPhoto())
            );
        }

        log.info("User {} successfully updated", user.getUsername());

        return mapUserToUserResponse(user);
    }

    @Override
//    @Caching(evict = {
//            @CacheEvict(value = "user", key = "#id"),
//            @CacheEvict(value = "users", allEntries = true)
//    })
    @Auditable(
            action = AuditAction.DELETE,
            entity = "User"
    )
    public void delete(Long id) {

        log.info("Delete user {}", id);

        User user = repository.findByIdAndStatusNot(id, Status.DELETED)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id " + id));

        assertCanManageUser(user);

        user.setStatus(Status.DELETED);

        log.info("User {} marked as deleted", user.getUsername());
    }

    @Override
//    @Caching(evict = {
//            @CacheEvict(value = "user", key = "#id"),
//            @CacheEvict(value = "users", allEntries = true)
//    })
    @Auditable(
            action = AuditAction.UPDATE,
            entity = "User"
    )
    public void activeOrDisabledUser(Long id) {

        log.info("Change status for user {}", id);

        User user = repository.findByIdAndStatusNot(id, Status.DELETED)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id " + id));

        assertCanManageUser(user);

        switch (user.getStatus()) {

            case ACTIVE -> {
                user.setStatus(Status.DISABLED);
                log.info("User {} disabled", user.getUsername());
            }

            case DISABLED -> {
                user.setStatus(Status.ACTIVE);
                log.info("User {} activated", user.getUsername());
            }

            default -> throw new BadRequestException(
                    "Status cannot be changed"
            );
        }
    }

    @Override
    @Transactional
    public UserStatResponse getUserStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Object[]> result = saleOrderRepository.countAndSumByUserId(userId);
        Object[] row = result.get(0);
        Long totalOrdersCount = (Long) row[0];
        BigDecimal totalOrdersSum = (BigDecimal) row[1];

        BigDecimal totalPaidSum = paymentRepository.sumPaymentAmountByUserId(userId);

        return UserStatResponse.builder()
                .userId(user.getId())
                .userFullName(user.getFullName()) // DIQQAT: pastga qarang
                .totalOrdersCount(totalOrdersCount)
                .totalOrdersSum(totalOrdersSum)
                .totalPaidSum(totalPaidSum)
                .totalDebt(totalOrdersSum.subtract(totalPaidSum))
                .build();
    }

    private UserResponse mapUserToUserResponse(User user) {

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .photoLink(user.getPhotoLink())
                .role(
                        user.getRoles()
                                .stream()
                                .map(this::mapRoleToRoleResponse)
                                .collect(Collectors.toSet())
                )
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updateAt(user.getUpdatedAt())
                .filialId(user.getFilial() != null ? user.getFilial().getId() : null)
                .filialName(user.getFilial() != null ? user.getFilial().getName() : null)
                .build();
    }

    private Filial resolveFilial(Long filialId) {
        if (!filialAccess.isSuperAdmin()) {
            return filialAccess.requireCurrentFilial();
        }
        if (filialId == null || filialId <= 0) {
            return null;
        }
        return filialRepository.findByIdAndStatusNot(filialId, Status.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Filial not found with id: " + filialId));
    }

    private boolean isElevatedAdmin() {
        var user = filialAccess.currentUser();
        if (user == null) return false;
        if (user.isSuperAdmin()) return true;
        if (user.getUser().getRoles() == null) return false;
        return user.getUser().getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName()));
    }

    private boolean isDirectorOnly() {
        var user = filialAccess.currentUser();
        if (user == null || isElevatedAdmin()) return false;
        if (user.getUser().getRoles() == null) return false;
        return user.getUser().getRoles().stream()
                .anyMatch(role -> "DIRECTOR".equals(role.getName()));
    }

    private void assertAssignableRoles(Set<Role> roles) {
        if (filialAccess.isSuperAdmin()) {
            return;
        }
        Set<String> blocked;
        if (isElevatedAdmin()) {
            blocked = Set.of("SUPER_ADMIN");
        } else if (isDirectorOnly()) {
            blocked = Set.of("SUPER_ADMIN", "ADMIN", "DIRECTOR");
        } else {
            blocked = Set.of("SUPER_ADMIN", "ADMIN", "DIRECTOR");
        }
        boolean forbidden = roles.stream().anyMatch(r -> blocked.contains(r.getName()));
        if (forbidden) {
            throw new ForbiddenException("Bu rollarni biriktirishga ruxsat yo‘q");
        }
    }

    private void assertCanManageUser(User target) {
        if (filialAccess.isSuperAdmin()) {
            return;
        }
        if (isDirectorOnly()) {
            Long myFilial = filialAccess.currentFilialId();
            Long targetFilial = target.getFilial() != null ? target.getFilial().getId() : null;
            if (myFilial == null || targetFilial == null || !myFilial.equals(targetFilial)) {
                throw new ForbiddenException("Faqat o‘z filialingiz xodimlarini boshqarishingiz mumkin");
            }
            boolean elevatedTarget = target.getRoles().stream()
                    .anyMatch(r -> "SUPER_ADMIN".equals(r.getName())
                            || "ADMIN".equals(r.getName())
                            || "DIRECTOR".equals(r.getName()));
            if (elevatedTarget) {
                throw new ForbiddenException("Bu foydalanuvchini o‘zgartirishga ruxsat yo‘q");
            }
        }
    }

    private RoleResponse mapRoleToRoleResponse(Role role) {

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .status(role.getStatus())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .createdUsername(role.getCreatedUsername())
                .build();
    }
}