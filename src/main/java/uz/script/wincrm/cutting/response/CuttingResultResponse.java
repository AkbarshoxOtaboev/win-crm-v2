package uz.script.wincrm.cutting.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Schema(name = "Cutting Result Response", description = "Listni bir xil bo'laklarga kesish hisobi (o'lchamlar sm, maydonlar m²)")
public class CuttingResultResponse {

    private BigDecimal sheetWidth;
    private BigDecimal sheetHeight;
    private BigDecimal pieceWidth;
    private BigDecimal pieceHeight;
    private Integer quantity;
    private BigDecimal kerfMm;
    private BigDecimal minRemnantCm;

    @Schema(description = "Bo'lak listga 90° burilgan holda joylashtirilgan")
    private boolean rotated;

    @Schema(description = "Joylashuvdagi bo'lak eni (list eni bo'ylab)")
    private BigDecimal layoutPieceWidth;

    @Schema(description = "Joylashuvdagi bo'lak bo'yi (list bo'yi bo'ylab)")
    private BigDecimal layoutPieceHeight;

    private Integer columns;
    private Integer rows;
    private Integer perSheet;
    private Integer sheetsNeeded;

    private BigDecimal sheetArea;
    private BigDecimal sheetsArea;
    private BigDecimal piecesArea;

    @Schema(description = "Listlar maydoni - bo'laklar maydoni")
    private BigDecimal leftoverArea;

    @Schema(description = "Foydali qoldiqlar maydoni")
    private BigDecimal usefulArea;

    @Schema(description = "Chiqindi: mayda qoldiqlar va kesuv chizig'i")
    private BigDecimal scrapArea;

    private BigDecimal usagePercent;
    private BigDecimal leftoverPercent;
    private BigDecimal scrapPercent;

    @Schema(description = "Bir xil joylashuvli listlar guruhlari: to'liq listlar va oxirgi list")
    private List<SheetLayout> sheets;

    @Getter
    @Builder
    public static class SheetLayout {
        @Schema(description = "Shu joylashuvdagi listlar soni")
        private Integer count;
        private Integer pieces;
        private Integer columns;
        private Integer rows;
        @Schema(description = "Oxirgi qatordagi bo'laklar soni")
        private Integer lastRowPieces;
        private BigDecimal usedWidth;
        private BigDecimal usedHeight;
        private List<Remnant> remnants;
    }

    @Getter
    @Builder
    public static class Remnant {
        private BigDecimal x;
        private BigDecimal y;
        private BigDecimal width;
        private BigDecimal height;
        private BigDecimal area;
        private boolean useful;
        @Schema(description = "Qoldiqqa yana nechta shunday bo'lak sig'adi")
        private Integer fitsPieces;
    }
}
