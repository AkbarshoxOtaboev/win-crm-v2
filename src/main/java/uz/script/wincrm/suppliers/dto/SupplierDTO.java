package uz.script.wincrm.suppliers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import uz.script.wincrm.suppliers.SupplierKind;

@Data
@Schema(description = "Supplier create/update request")
public class SupplierDTO {

    @NotBlank(message = "Supplier name is required")
    @Schema(description = "Supplier name", example = "Artel")
    private String name;

    @Schema(description = "LOCAL for the domestic market, INTERNATIONAL when goods come from abroad", example = "LOCAL")
    private SupplierKind kind;

    @Size(max = 20)
    @Schema(description = "INN (STIR)", example = "305987654")
    private String inn;


    @NotBlank(message = "Phone is required")
    @Size(max = 40, message = "Phone number must not exceed 40 characters")
    @Schema(description = "Phone number", example = "+998-(97)-221-88-96")
    private String phone;

    @Size(max = 40, message = "Additional phone number must not exceed 40 characters")
    @Schema(description = "Additional phone", example = "+998-(97)-221-88-96")
    private String additionalPhone;



    @Schema(description = "Address")
    private String address;

    @Schema(description = "Bank name")
    private String bankName;

    @Schema(description = "MFO", example = "00450")
    private String mfo;

    @Schema(description = "Account number")
    private String accountNumber;

    @Schema(description = "Description")
    private String description;
}