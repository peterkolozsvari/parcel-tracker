package hu.kolozsvaripeter.parceltracker.exception;

public class UserAccessConflictException extends RuntimeException {
    public UserAccessConflictException(String message) { super(message); }
}
