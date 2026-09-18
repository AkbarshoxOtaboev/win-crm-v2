package uz.script.wincrm.telegram.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.telegram.TelegramUserRole;
import uz.script.wincrm.utils.Status;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class TelegramUserResponse {
    private Long id;
    private Long chatId;
    private String telegramUsername;
    private String firstName;
    private String lastName;
    private String phone;
    private TelegramUserRole role;
    private boolean verified;
    private Long clientId;
    private String clientFullName;
    private Status status;
    private LocalDateTime createdAt;
}
