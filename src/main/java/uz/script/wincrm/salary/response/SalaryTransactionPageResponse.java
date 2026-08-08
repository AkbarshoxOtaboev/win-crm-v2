package uz.script.wincrm.salary.response;

import io.swagger.v3.oas.annotations.media.Schema;
import uz.script.wincrm.utils.response.PageResponse;

@Schema(name = "Salary Transaction Page Response", description = "Sahifalangan oylik ledger yozuvlari")
public class SalaryTransactionPageResponse extends PageResponse<SalaryTransactionResponse> {
}
