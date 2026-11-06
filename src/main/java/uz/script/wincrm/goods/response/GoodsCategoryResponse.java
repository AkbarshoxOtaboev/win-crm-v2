package uz.script.wincrm.goods.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.utils.Status;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(description = "Mahsulot kategoriyasi")
public class GoodsCategoryResponse {

    private Long id;
    private String name;
    private Status status;
    private String createdUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
