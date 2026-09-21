package uz.script.wincrm.workshop.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.utils.Status;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(name = "Workshop Response")
public class WorkshopResponse {

    private Long id;
    private String name;
    private String description;
    private Long managerId;
    private String managerFullName;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdUsername;
}
