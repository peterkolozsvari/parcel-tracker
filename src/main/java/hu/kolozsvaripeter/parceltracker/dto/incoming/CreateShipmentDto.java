package hu.kolozsvaripeter.parceltracker.dto.incoming;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateShipmentDto(

        @NotNull(message = "Sender ID is required")
        @Positive(message = "Sender ID must be greater than zero")
        Long senderId,

        @NotBlank(message = "Receiver name is required")
        @Size(
                max = 100,
                message = "Receiver name must not exceed 100 characters"
        )
        String receiverName,

        @NotNull(message = "Pickup address is required")
        @Valid
        CreateAddressDto pickupAddress,

        @NotNull(message = "Delivery address is required")
        @Valid
        CreateAddressDto deliveryAddress,

        @NotEmpty(message = "At least one parcel is required")
        List<@NotNull @Valid CreateParcelDto> parcels
) {
}
