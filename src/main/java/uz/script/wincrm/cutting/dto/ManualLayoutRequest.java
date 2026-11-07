package uz.script.wincrm.cutting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualLayoutRequest {

    @NotBlank(message = "Kesish kartasi bo'sh")
    @Size(max = 2_000_000, message = "Kesish kartasi juda katta")
    private String layout;
}
