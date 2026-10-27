package com.example.dineX.menu;

import com.example.dineX.menu.MenuDtos.CategoryRequest;
import com.example.dineX.menu.MenuDtos.CategoryView;
import com.example.dineX.menu.MenuDtos.ItemRequest;
import com.example.dineX.menu.MenuDtos.ItemView;
import com.example.dineX.menu.MenuDtos.ToggleRequest;
import com.example.dineX.security.AuthUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/hotel/menu")
public class MenuController {
    private final MenuService menu;

    public MenuController(MenuService menu) {
        this.menu = menu;
    }

    @GetMapping("/categories")
    public List<CategoryView> categories(@AuthenticationPrincipal AuthUser user) {
        return menu.categories(user.hotelId());
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryView createCategory(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CategoryRequest request) {
        return menu.createCategory(user.hotelId(), request);
    }

    @PutMapping("/categories/{id}")
    public CategoryView updateCategory(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                       @Valid @RequestBody CategoryRequest request) {
        return menu.updateCategory(user.hotelId(), id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        menu.deleteCategory(user.hotelId(), id);
    }

    @GetMapping("/items")
    public List<ItemView> items(@AuthenticationPrincipal AuthUser user,
                                @RequestParam(value = "categoryId", required = false) Long categoryId) {
        return menu.items(user.hotelId(), categoryId);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemView createItem(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ItemRequest request) {
        return menu.createItem(user.hotelId(), request);
    }

    @PutMapping("/items/{id}")
    public ItemView updateItem(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                               @Valid @RequestBody ItemRequest request) {
        return menu.updateItem(user.hotelId(), id, request);
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        menu.deleteItem(user.hotelId(), id);
    }

    @PatchMapping("/items/{id}/availability")
    public ItemView availability(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                 @RequestBody ToggleRequest request) {
        return menu.setAvailability(user.hotelId(), id, request.value());
    }

    @PatchMapping("/items/{id}/visibility")
    public ItemView visibility(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                               @RequestBody ToggleRequest request) {
        return menu.setVisibility(user.hotelId(), id, request.value());
    }

    @PostMapping(value = "/items/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ItemView image(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                          @RequestPart("file") MultipartFile file) {
        return menu.uploadImage(user.hotelId(), id, file);
    }
}
