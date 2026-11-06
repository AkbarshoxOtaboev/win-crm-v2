package uz.script.wincrm.finishedgoods.response;

import lombok.Builder;
import lombok.Getter;
import uz.script.wincrm.finishedgoods.enums.FinishedGoodsDocStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MaterialIssueResponse {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long workshopId;
    private String workshopName;
    private String comment;
    private LocalDateTime documentDate;
    private FinishedGoodsDocStatus docStatus;
    private LocalDateTime createdAt;
    private String createdUsername;
    private List<MaterialIssueItemResponse> items;
}
