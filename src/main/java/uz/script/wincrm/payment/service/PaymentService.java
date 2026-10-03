package uz.script.wincrm.payment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.script.wincrm.payment.dto.PaymentAllocationRequest;
import uz.script.wincrm.payment.dto.PaymentDTO;
import uz.script.wincrm.payment.response.PaymentResponse;

import java.time.LocalDate;
import java.util.List;

public interface PaymentService {

    PaymentResponse create(PaymentDTO dto);

    PaymentResponse findById(Long id);

    Page<PaymentResponse> fetchAll(Pageable pageable);

    Page<PaymentResponse> search(Long clientId, Long paymentTypeId, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    Page<PaymentResponse> fetchByClientId(Long clientId, Pageable pageable);

    Page<PaymentResponse> fetchBySaleOrderId(Long saleOrderId, Pageable pageable);

    Page<PaymentResponse> fetchByPaymentTypeId(Long paymentTypeId, Pageable pageable);

    PaymentResponse update(Long id, PaymentDTO dto);

    void delete(Long id);

    /**
     * Belgilangan buyurtmasiz to'lovlarni kassir ko'rsatgan buyurtmalarga taqsimlaydi.
     * To'lov qisman ishlatilsa, u ikkiga bo'linadi: ishlatilgan qism buyurtmaga yoziladi,
     * qolgani taqsimlanmagan holda qoladi.
     */
    List<PaymentResponse> allocate(PaymentAllocationRequest request);

    /** To'lovni buyurtmadan ajratib, yana taqsimlanmagan holatga qaytaradi. */
    PaymentResponse unallocate(Long id);
}