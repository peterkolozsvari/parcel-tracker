package hu.kolozsvaripeter.parceltracker.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.Comparator;

/** The API uses this advice ahead of the legacy teaching-example handler. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception) {
        var fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldViolation(error.getField(),
                        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()))
                .sorted(Comparator.comparing(ErrorResponse.FieldViolation::field)
                        .thenComparing(ErrorResponse.FieldViolation::message)).toList();
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", "Request validation failed", fields));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, ConstraintViolationException.class,
            PropertyReferenceException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> badRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The request contains invalid data");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> authentication(AuthenticationException exception) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication failed or account unavailable");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(AccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> userMissing(UserNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "The requested user was not found");
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> shipmentMissing(ShipmentNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "SHIPMENT_NOT_FOUND", "The requested shipment was not found");
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> duplicateEmail(DuplicateEmailException exception) {
        return error(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email address is already registered");
    }

    @ExceptionHandler(ShipmentStateConflictException.class)
    public ResponseEntity<ErrorResponse> shipmentConflict(ShipmentStateConflictException exception) {
        return error(HttpStatus.CONFLICT, "SHIPMENT_STATE_CONFLICT", "The requested shipment operation is not allowed in its current state");
    }

    @ExceptionHandler(UserAccessConflictException.class)
    public ResponseEntity<ErrorResponse> userConflict(UserAccessConflictException exception) {
        return error(HttpStatus.CONFLICT, "USER_ACCESS_CONFLICT", "You cannot remove your own administrator access");
    }

    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> persistenceConflict(Exception exception) {
        log.warn("Persistence conflict", exception);
        return error(HttpStatus.CONFLICT, "DATA_CONFLICT", "The operation conflicts with the current data. Reload and try again");
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> routeMissing(Exception exception) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        var builder = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (exception.getSupportedHttpMethods() != null) {
            builder.allow(exception.getSupportedHttpMethods().toArray(org.springframework.http.HttpMethod[]::new));
        }
        return builder.body(ErrorResponse.of("METHOD_NOT_ALLOWED", "HTTP method is not supported for this resource"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> unsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Unsupported request content type");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception exception) {
        log.error("Unhandled API exception", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(code, message));
    }
}
