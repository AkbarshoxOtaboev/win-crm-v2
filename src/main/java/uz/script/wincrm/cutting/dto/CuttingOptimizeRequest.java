package uz.script.wincrm.cutting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CuttingOptimizeRequest {

    @Valid
    @NotNull
    private List<Line> lines = new ArrayList<>();

    @Getter
    @Setter
    public static class Line {
        @NotNull
        private Long saleOrderItemId;
        @NotNull
        @Min(0)
        private Integer sheetQuantity;
        @Valid
        private List<RemnantPick> remnants = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class RemnantPick {
        @NotNull
        private Long remnantId;
        @NotNull
        @Min(1)
        private Integer quantity;
    }
}
