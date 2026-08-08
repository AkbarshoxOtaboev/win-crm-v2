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
import uz.script.wincrm.salary.SalaryTransaction;
import uz.script.wincrm.salary.dto.SalaryAdjustmentDTO;
import uz.script.wincrm.salary.enums.SalaryEntryType;
import uz.script.wincrm.salary.mapper.SalaryTransactionMapper;
import uz.script.wincrm.salary.repository.SalaryTransactionRepository;
import uz.script.wincrm.salary.response.SalaryTransactionResponse;
import uz.script.wincrm.salary.service.SalaryTransactionService;
import uz.script.wincrm.users.repository.UserRepository;
import uz.script.wincrm.utils.Status;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SalaryTransactionServiceImpl implements SalaryTransactionService {

    private final SalaryTransactionRepository repository;
    private final SalaryTransactionMapper mapper;
    private final UserRepository userRepository;

    @Override
    @Auditable(action = AuditAction.CREATE, entity = "SalaryTransaction")
    public SalaryTransactionResponse addAdjustment(SalaryAdjustmentDTO dto) {
        // Faqat qo'lda tuzatishlar. COMMISSION/COMMISSION_REVERSAL faqat tizim (to'lov hodisasi) yozadi.
        if (dto.getEntryType() == SalaryEntryType.COMMISSION
                || dto.getEntryType() == SalaryEntryType.COMMISSION_REVERSAL) {
            throw new BadRequestException(
                    "COMMISSION turlari qo'lda kiritilmaydi - ular to'lov hodisasida avtomatik yoziladi");
        }

        if (!userRepository.existsById(dto.getUserId())) {
            throw new ResourceNotFoundException("User not found with id: " + dto.getUserId());
        }

        LocalDateTime now = LocalDateTime.now();
        int year = dto.getPeriodYear() != null ? dto.getPeriodYear() : now.getYear();
        int month = dto.getPeriodMonth() != null ? dto.getPeriodMonth() : now.getMonthValue();
        if (month < 1 || month > 12) {
            throw new BadRequestException("periodMonth must be between 1 and 12");
        }

        SalaryTransaction entity = SalaryTransaction.builder()
                .userId(dto.getUserId())
                .saleOrderId(null)
                .entryType(dto.getEntryType())
                .amount(dto.getAmount())
                .earnedAt(now)
                .periodYear(year)
                .periodMonth(month)
                .comment(dto.getComment())
                .status(Status.ACTIVE)
                .build();

        entity = repository.save(entity);
        return mapper.toResponse(entity);
    }

    @Override
    public Page<SalaryTransactionResponse> fetchByUserId(Long userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable).map(mapper::toResponse);
    }

    @Override
    public Page<SalaryTransactionResponse> fetchByUserAndPeriod(
            Long userId, Integer year, Integer month, Pageable pageable) {
        return repository.findByUserIdAndPeriodYearAndPeriodMonth(userId, year, month, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Auditable(action = AuditAction.DELETE, entity = "SalaryTransaction")
    public void delete(Long id) {
        SalaryTransaction entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salary transaction not found with id: " + id));
        // Avtomatik komissiya yozuvlarini qo'lda o'chirishga yo'l qo'ymaymiz - ular ledger yaxlitligini saqlaydi.
        if (entity.getEntryType() == SalaryEntryType.COMMISSION
                || entity.getEntryType() == SalaryEntryType.COMMISSION_REVERSAL) {
            throw new BadRequestException(
                    "Avtomatik komissiya yozuvini o'chirib bo'lmaydi - u to'lov o'zgarganda avtomatik to'g'rilanadi");
        }
        entity.setStatus(Status.DELETED);
        repository.save(entity);
    }
}
