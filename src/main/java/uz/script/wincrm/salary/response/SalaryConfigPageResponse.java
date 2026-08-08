package uz.script.wincrm.salary.response;

import io.swagger.v3.oas.annotations.media.Schema;
import uz.script.wincrm.utils.response.PageResponse;

@Schema(name = "Salary Config Page Response", description = "Sahifalangan oylik konfiguratsiyalar ro'yxati")
public class SalaryConfigPageResponse extends PageResponse<SalaryConfigResponse> {
}
