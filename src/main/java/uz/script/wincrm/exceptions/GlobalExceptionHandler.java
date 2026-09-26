package uz.script.wincrm.exceptions;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uz.script.wincrm.sms.SmsSendException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(
            InsufficientStockException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(SmsSendException.class)
    public ResponseEntity<ErrorResponse> handleSmsSend(
            SmsSendException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_GATEWAY, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSort(
            InvalidDataAccessApiUsageException ex,
            HttpServletRequest request
    ) {
        String message = messageSource.getMessage(
                "error.invalid.sort",
                null,
                "Noto'g'ri 'sort' maydoni yuborildi.",
                currentLocale()
        );
        return buildResponse(message, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request
    ) {
        String message = resolveOrDefault(ex.getMessage(), "error.unauthorized");
        return buildResponse(message, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex,
            HttpServletRequest request
    ) {
        String message = resolveOrDefault(ex.getMessage(), "error.forbidden");
        return buildResponse(message, HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        String message = messageSource.getMessage("error.forbidden", null, "Access denied", currentLocale());
        return buildResponse(message, HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExists(
            AlreadyExistsException ex,
            HttpServletRequest request
    ) {
        String message = messageSource.getMessage(
                ex.getMessageCode(),
                ex.getArgs(),
                ex.getMessageCode(),
                currentLocale()
        );
        return buildResponse(message, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(UserDisabledException.class)
    public ResponseEntity<ErrorResponse> handleUserDisabled(
            UserDisabledException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ex.getMessage(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return buildResponse("Noto'g'ri parametr qiymati: " + ex.getName(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return buildResponse("So'rov tanasi (JSON) noto'g'ri formatda", HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(
            Exception ex,
            HttpServletRequest request
    ) {
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.resolve(springError.getStatusCode().value());
            if (status != null && !status.is5xxServerError()) {
                return buildResponse(ex.getMessage(), status, request);
            }
        }
        Throwable current = ex;
        while (current != null) {
            if (current instanceof AlreadyExistsException already) {
                return handleAlreadyExists(already, request);
            }
            if (current instanceof BadRequestException bad) {
                return buildResponse(bad.getMessage(), HttpStatus.BAD_REQUEST, request);
            }
            if (current instanceof ForbiddenException forbidden) {
                return handleForbidden(forbidden, request);
            }
            if (current instanceof ResourceNotFoundException missing) {
                return buildResponse(missing.getMessage(), HttpStatus.NOT_FOUND, request);
            }
            current = current.getCause();
        }
        String fallback = messageSource.getMessage(
                "error.generic",
                null,
                "An unexpected error occurred",
                currentLocale()
        );
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? fallback : ex.getMessage();
        return buildResponse(message, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private String resolveOrDefault(String message, String code) {
        if (message == null || message.isBlank() || "Access denied".equalsIgnoreCase(message)) {
            return messageSource.getMessage(code, null, message, currentLocale());
        }
        return message;
    }

    private Locale currentLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        if (locale == null || locale.getLanguage() == null || locale.getLanguage().isBlank()) {
            return Locale.forLanguageTag("uz");
        }
        return locale;
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            String message,
            HttpStatus status,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(response, status);
    }
}
