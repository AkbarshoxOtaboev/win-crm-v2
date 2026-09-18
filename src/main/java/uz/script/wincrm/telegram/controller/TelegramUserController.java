package uz.script.wincrm.telegram.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.script.wincrm.telegram.TelegramUser;
import uz.script.wincrm.telegram.repository.TelegramUserRepository;
import uz.script.wincrm.telegram.response.TelegramUserResponse;
import uz.script.wincrm.utils.RestApiResponse;

@RestController
@RequestMapping("/api/telegram/users")
@RequiredArgsConstructor
@Tag(name = "Telegram registered users")
public class TelegramUserController {

    private final TelegramUserRepository telegramUserRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('BOT_SETTINGS_VIEW')")
    @Operation(summary = "Bot orqali ro'yxatdan o'tgan mijozlar (pageable)")
    public ResponseEntity<?> fetchAll(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TelegramUserResponse> page = telegramUserRepository.findAllWithClient(pageable)
                .map(this::toResponse);

        return ResponseEntity.ok(
                RestApiResponse.<Page<TelegramUserResponse>>builder()
                        .message("Telegram foydalanuvchilar")
                        .data(page)
                        .build()
        );
    }

    private TelegramUserResponse toResponse(TelegramUser user) {
        return TelegramUserResponse.builder()
                .id(user.getId())
                .chatId(user.getChatId())
                .telegramUsername(user.getTelegramUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole())
                .verified(user.isVerified())
                .clientId(user.getClient() != null ? user.getClient().getId() : null)
                .clientFullName(user.getClient() != null ? user.getClient().getFullName() : null)
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
