package uz.script.wincrm.transport.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@Schema(name = "Transport Setting Response")
public class TransportSettingResponse {
    private BigDecimal workerSalaryPercent;
}
