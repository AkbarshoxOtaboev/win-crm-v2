package uz.script.wincrm.audit;

import io.swagger.v3.oas.annotations.media.Schema;
import uz.script.wincrm.utils.response.PageResponse;

@Schema(name = "Audit Page Response", description = "Sahifalangan audit loglar ro'yxati")
public class AuditPageResponse extends PageResponse<AuditResponse> {
}
