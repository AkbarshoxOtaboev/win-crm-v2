package uz.script.wincrm.transport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "Worker Salary Decision DTO")
public class SalaryDecisionDTO {

    @NotEmpty(message = "Salary ids are required")
    private List<Long> ids;

    private String comment;
}
