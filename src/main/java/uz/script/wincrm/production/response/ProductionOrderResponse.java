package uz.script.wincrm.production.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.production.enums.ProductionAssignmentStatus;
import uz.script.wincrm.production.enums.ProductionOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ProductionOrderResponse {
    private Long id;
    private Long saleOrderId;
    private String clientFullName;
    private ProductionOrderStatus productionStatus;
    private Long currentWorkshopId;
    private String currentWorkshopName;
    private ProductionAssignmentStatus currentAssignmentStatus;
    private Long currentAssignmentId;
    private LocalDateTime startedAt;
    private LocalDateTime doneAt;
    private String note;
    private LocalDateTime createdAt;
    private List<RouteStep> route;
    private Long nextWorkshopId;
    private String nextWorkshopName;

    /** Faqat sex doskasida: shu sexdagi topshiriq (buyurtma keyingi sexga o'tgan bo'lsa ham). */
    private Long boardAssignmentId;
    private ProductionAssignmentStatus boardAssignmentStatus;
    private LocalDateTime acceptedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime orderDate;
    private LocalDateTime plannedReadyDate;
    private String saleOrderComment;
    private List<Item> items;
    private List<Image> images;

    @Getter
    @Setter
    @Builder
    public static class RouteStep {
        private Integer stepNo;
        private Long workshopId;
        private String workshopName;
        /** DONE, CURRENT yoki PLANNED */
        private String state;
    }

    /** Sex uchun pozitsiya: narxlarsiz, faqat o'lcham va miqdor. */
    @Getter
    @Setter
    @Builder
    public static class Item {
        private Long id;
        private Long goodsId;
        private String goodsName;
        private BigDecimal width;
        private BigDecimal height;
        /** Oyna uchun kv.m, boshqa mahsulotda dona/miqdor. */
        private BigDecimal count;
        /** Oyna uchun dona soni (eni va bo'yi bo'lsa). */
        private BigDecimal pieces;
    }

    @Getter
    @Setter
    @Builder
    public static class Image {
        private Long id;
        private String url;
        private String originalFileName;
    }
}
