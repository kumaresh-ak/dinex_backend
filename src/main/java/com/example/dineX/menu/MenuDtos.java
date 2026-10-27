package com.example.dineX.menu;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class MenuDtos {
    private MenuDtos() { }

    public record CategoryRequest(
            @NotBlank @Size(max = 100) String name,
            int displayPriority,
            boolean enabled) { }

    public record CategoryView(Long id, String name, int displayPriority, boolean enabled, int itemCount) { }

    public record ItemRequest(
            @NotNull Long categoryId,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 1000) String description,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotNull @Min(1) @Max(240) Integer preparationMinutes,
            @NotNull MenuItem.FoodType foodType,
            int displayPriority,
            boolean available,
            boolean visible) { }

    public record ItemView(Long id, Long categoryId, String categoryName, String name, String description,
                           BigDecimal price, String imageUrl, int preparationMinutes, String foodType,
                           boolean available, boolean visible, int displayPriority) { }

    public record ToggleRequest(boolean value) { }
}
