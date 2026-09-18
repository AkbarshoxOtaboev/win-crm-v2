package uz.script.wincrm.filial;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.utils.Status;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class FilialResponse {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private Long directorId;
    private String directorFullName;
    private String directorUsername;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
