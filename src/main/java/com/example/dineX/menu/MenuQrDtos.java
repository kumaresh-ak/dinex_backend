package com.example.dineX.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class MenuQrDtos {
    private MenuQrDtos() { }

    /** Hotel owner view of the common menu QR code. */
    public record MenuQrView(boolean generated, String url) { }

    /** Public page data: hotel branding and the tables a guest can pick from. */
    public record PublicMenuView(String hotelName, String logoUrl, List<String> tableNumbers) { }

    public record TableSelectionRequest(@NotBlank @Size(max = 20) String tableNumber) { }

    /** Session started from the common menu QR; tableQrToken lets the guest page load the menu. */
    public record MenuQrSession(String sessionToken, String tableNumber, String status, String tableQrToken) { }
}
