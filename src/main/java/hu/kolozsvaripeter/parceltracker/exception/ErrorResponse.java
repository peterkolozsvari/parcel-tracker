package hu.kolozsvaripeter.parceltracker.exception;

import java.util.List;

/** Public API error contract. Internal exception details never belong in this response. */
public record ErrorResponse(String errorCode, String message, List<FieldViolation> fieldErrors) {
    public ErrorResponse { fieldErrors = List.copyOf(fieldErrors); }
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of());
    }
    public record FieldViolation(String field, String message) {}
}
