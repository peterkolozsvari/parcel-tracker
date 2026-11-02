package hu.kolozsvaripeter.parceltracker.exception;

import hu.kolozsvaripeter.parceltracker.controller.AdminUserController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AdminUserController.class)
public class AdminUserExceptionHandler {
    @ExceptionHandler(UserAccessConflictException.class)
    public ResponseEntity<ApiError> handleConflict(UserAccessConflictException ex) {
        return ResponseEntity.status(409).body(new ApiError("USER_ACCESS_CONFLICT", ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleInvalidParameter(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_PARAMETER", "Invalid request parameter", null));
    }
}
