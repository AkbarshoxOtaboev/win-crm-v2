package uz.script.wincrm.cutting.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RemnantDeleteRequest {

    @NotEmpty(message = "O'chirish uchun qoldiq tanlanmagan")
    private List<Long> ids;
}
