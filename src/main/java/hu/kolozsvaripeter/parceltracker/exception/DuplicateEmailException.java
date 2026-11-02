package hu.kolozsvaripeter.parceltracker.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() { super("Email address is already registered"); }
}
