package com.example.dineX.menu;

import com.example.dineX.menu.MenuQrDtos.MenuQrSession;
import com.example.dineX.menu.MenuQrDtos.MenuQrView;
import com.example.dineX.menu.MenuQrDtos.PublicMenuView;
import com.example.dineX.menu.MenuQrDtos.TableSelectionRequest;
import com.example.dineX.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

public final class MenuQrController {
    private MenuQrController() { }

    @RestController
    @RequestMapping("/api/v1/hotel/menu-qr")
    public static class HotelMenuQrController {
        private final MenuQrService menuQr;

        public HotelMenuQrController(MenuQrService menuQr) {
            this.menuQr = menuQr;
        }

        @GetMapping
        public MenuQrView current(@AuthenticationPrincipal AuthUser user) {
            return menuQr.current(user.hotelId());
        }

        @PostMapping
        public MenuQrView generate(@AuthenticationPrincipal AuthUser user) {
            return menuQr.generate(user.hotelId());
        }

        @GetMapping(value = "/image", produces = MediaType.IMAGE_PNG_VALUE)
        public ResponseEntity<byte[]> image(@AuthenticationPrincipal AuthUser user) {
            return ResponseEntity.ok()
                    .header("Cache-Control", "no-store")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(menuQr.png(user.hotelId()));
        }
    }

    @RestController
    @RequestMapping("/api/v1/public/menu-qr")
    public static class PublicMenuQrController {
        private final MenuQrService menuQr;

        public PublicMenuQrController(MenuQrService menuQr) {
            this.menuQr = menuQr;
        }

        @GetMapping("/{token}")
        public PublicMenuView tables(@PathVariable String token) {
            return menuQr.tablesFor(token);
        }

        @PostMapping("/{token}/session")
        public MenuQrSession start(@PathVariable String token, @Valid @RequestBody TableSelectionRequest request) {
            return menuQr.startSession(token, request.tableNumber());
        }
    }
}
