package uz.script.wincrm.transport.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(name = "Transport Driver Response")
public class TransportDriverResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String carModel;
    private String carNumber;
    private BigDecimal maxWeightKg;
    private String note;
    private Status status;
    private LocalDateTime createdAt;
}
