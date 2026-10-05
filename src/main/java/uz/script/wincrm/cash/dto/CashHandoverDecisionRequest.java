package uz.script.wincrm.cash.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CashHandoverDecisionRequest {

    @Size(max = 255)
    private String comment;
}
