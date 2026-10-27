package com.example.dineX.hotel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class HotelDtos {
    private HotelDtos() { }

    public record Register(
            @NotBlank @Size(max = 160) String hotelName,
            @NotBlank @Size(max = 160) String ownerName,
            @NotBlank @Pattern(regexp = "^[0-9+\\- ]{7,30}$", message = "Invalid mobile number") String mobileNumber,
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters") String password,
            @NotBlank @Size(max = 255) String address,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Size(max = 100) String country,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits") String pincode,
            @Pattern(regexp = "^$|^[0-9A-Z]{15}$", message = "Invalid GST number") String gstNumber,
            @NotBlank @Size(max = 120) String upiId) { }

    public record Update(
            @NotBlank @Size(max = 160) String hotelName,
            @NotBlank @Size(max = 160) String ownerName,
            @NotBlank @Pattern(regexp = "^[0-9+\\- ]{7,30}$", message = "Invalid mobile number") String mobileNumber,
            @NotBlank @Size(max = 255) String address,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Size(max = 100) String country,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits") String pincode,
            @Pattern(regexp = "^$|^[0-9A-Z]{15}$", message = "Invalid GST number") String gstNumber,
            @NotBlank @Size(max = 120) String upiId) { }

    public record View(Long id, String hotelName, String ownerName, String mobileNumber, String email,
                       String address, String city, String state, String country, String pincode,
                       String gstNumber, String upiId, String logoUrl, String qrImageUrl,
                       String subscriptionStatus, boolean enabled) { }

    public record EnabledRequest(boolean enabled) { }
}
