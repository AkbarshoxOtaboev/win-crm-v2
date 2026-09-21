package uz.script.wincrm.production.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.production.enums.ProductionEventType;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ProductionEventResponse {
    private Long id;
    private ProductionEventType eventType;
    private Long fromWorkshopId;
    private String fromWorkshopName;
    private Long toWorkshopId;
    private String toWorkshopName;
    private Long actorId;
    private String actorName;
    private LocalDateTime occurredAt;
    private String comment;
}
