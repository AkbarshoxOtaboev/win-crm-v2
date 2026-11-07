package uz.script.wincrm.cutting.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ManualLayoutResponse {

    private final String layout;
    private final LocalDateTime savedAt;
    private final boolean hasPrevious;
}
