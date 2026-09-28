package uz.script.wincrm.transport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Schema(name = "Delivery Crew DTO", description = "Accept delivery / change driver and workers")
public class DeliveryCrewDTO {

    @NotNull(message = "Driver is required")
    private Long driverId;

    private List<Long> workerIds = new ArrayList<>();

    private String address;

    private String note;
}
