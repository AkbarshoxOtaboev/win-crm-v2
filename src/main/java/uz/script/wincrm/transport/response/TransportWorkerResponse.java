package uz.script.wincrm.transport.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.utils.Status;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(name = "Transport Worker Response")
public class TransportWorkerResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String note;
    private Status status;
    private LocalDateTime createdAt;
}
