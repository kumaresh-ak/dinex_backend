package com.example.dineX.hotel;

import com.example.dineX.common.PageResponse;
import com.example.dineX.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

public final class HotelController {
    private HotelController() { }

    /** Public self-service registration. Creates the hotel and its HOTEL_ADMIN login; subscription is requested next. */
    @RestController
    @RequestMapping("/api/v1/public/hotels")
    public static class PublicRegistrationController {
        private final HotelService hotels;

        public PublicRegistrationController(HotelService hotels) {
            this.hotels = hotels;
        }

        @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @ResponseStatus(HttpStatus.CREATED)
        public HotelDtos.View register(@Valid @RequestPart("data") HotelDtos.Register data,
                                       @RequestPart(value = "logo", required = false) MultipartFile logo,
                                       @RequestPart(value = "qr", required = false) MultipartFile qr) {
            return hotels.register(data, logo, qr);
        }
    }

    /** Hotel-admin profile. Exempt from the subscription gate so an expired hotel can still update details. */
    @RestController
    @RequestMapping("/api/v1/hotel/profile")
    public static class ProfileController {
        private final HotelService hotels;

        public ProfileController(HotelService hotels) {
            this.hotels = hotels;
        }

        @GetMapping
        public HotelDtos.View get(@AuthenticationPrincipal AuthUser user) {
            return hotels.profile(user.hotelId());
        }

        @PutMapping
        public HotelDtos.View update(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody HotelDtos.Update request) {
            return hotels.updateDetails(user.hotelId(), request);
        }

        @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public HotelDtos.View logo(@AuthenticationPrincipal AuthUser user, @RequestPart("file") MultipartFile file) {
            return hotels.replaceLogo(user.hotelId(), file);
        }

        @PostMapping(value = "/qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public HotelDtos.View qr(@AuthenticationPrincipal AuthUser user, @RequestPart("file") MultipartFile file) {
            return hotels.replaceQr(user.hotelId(), file);
        }
    }

    /** Platform-level hotel management for SUPER_ADMIN. */
    @RestController
    @RequestMapping("/api/v1/admin/hotels")
    public static class AdminController {
        private final HotelService hotels;

        public AdminController(HotelService hotels) {
            this.hotels = hotels;
        }

        @GetMapping
        public PageResponse<HotelDtos.View> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
            return hotels.list(pageable);
        }

        @GetMapping("/{id}")
        public HotelDtos.View get(@PathVariable Long id) {
            return hotels.get(id);
        }

        @PutMapping("/{id}")
        public HotelDtos.View update(@PathVariable Long id, @Valid @RequestBody HotelDtos.Update request) {
            return hotels.updateDetails(id, request);
        }

        @PatchMapping("/{id}/enabled")
        public HotelDtos.View enabled(@PathVariable Long id, @RequestBody HotelDtos.EnabledRequest request) {
            return hotels.setEnabled(id, request.enabled());
        }

        @DeleteMapping("/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(@PathVariable Long id) {
            hotels.softDelete(id);
        }
    }
}
