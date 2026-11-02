package hu.kolozsvaripeter.parceltracker.exception;

public class ShipmentStateConflictException extends RuntimeException {
    public ShipmentStateConflictException(String message) {
        super(message);
    }
}
