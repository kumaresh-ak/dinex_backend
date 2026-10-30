package com.example.dineX.hotel;

import com.example.dineX.audit.AuditService;
import com.example.dineX.common.ApiException;
import com.example.dineX.common.PageResponse;
import com.example.dineX.security.TenantStatusCache;
import com.example.dineX.security.UserRole;
import com.example.dineX.storage.FileStorageService;
import com.example.dineX.user.User;
import com.example.dineX.user.UserRepository;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HotelService {
    private final HotelRepository hotels;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService storage;
    private final AuditService audit;
    private final TenantStatusCache tenantStatus;

    public HotelService(HotelRepository hotels, UserRepository users, PasswordEncoder passwordEncoder,
                        FileStorageService storage, AuditService audit, TenantStatusCache tenantStatus) {
        this.hotels = hotels;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.storage = storage;
        this.audit = audit;
        this.tenantStatus = tenantStatus;
    }

    @Transactional
    public HotelDtos.View register(HotelDtos.Register request, MultipartFile logo, MultipartFile qr) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (hotels.existsByEmail(email) || users.existsByUsername(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        Hotel hotel = new Hotel();
        hotel.setEmail(email);
        apply(hotel, request.hotelName(), request.ownerName(), request.mobileNumber(), request.address(),
                request.city(), request.state(), request.country(), request.pincode(), request.gstNumber(), request.upiId());
        hotel.setSubscriptionStatus(Hotel.SubscriptionStatus.PENDING);
        hotel.setEnabled(true);
        if (hasFile(logo)) {
            hotel.setLogoUrl(storage.store(logo, "logos"));
        }
        if (hasFile(qr)) {
            hotel.setQrImageUrl(storage.store(qr, "qr"));
        }
        Hotel saved = hotels.save(hotel);
        users.save(new User(saved.getId(), email, passwordEncoder.encode(request.password()),
                request.ownerName(), request.mobileNumber(), UserRole.HOTEL_ADMIN));
        audit.record(saved.getId(), "HOTEL_REGISTERED", "Hotel", saved.getId(), null);
        return view(saved);
    }

    @Transactional(readOnly = true)
    public HotelDtos.View profile(Long hotelId) {
        return view(find(hotelId));
    }

    @Transactional
    public HotelDtos.View updateDetails(Long hotelId, HotelDtos.Update request) {
        Hotel hotel = find(hotelId);
        apply(hotel, request.hotelName(), request.ownerName(), request.mobileNumber(), request.address(),
                request.city(), request.state(), request.country(), request.pincode(), request.gstNumber(), request.upiId());
        audit.record(hotelId, "HOTEL_PROFILE_UPDATED", "Hotel", hotelId, null);
        return view(hotel);
    }

    @Transactional
    public HotelDtos.View replaceLogo(Long hotelId, MultipartFile file) {
        Hotel hotel = find(hotelId);
        hotel.setLogoUrl(storage.store(file, "logos"));
        audit.record(hotelId, "HOTEL_LOGO_UPDATED", "Hotel", hotelId, null);
        return view(hotel);
    }

    @Transactional
    public HotelDtos.View replaceQr(Long hotelId, MultipartFile file) {
        Hotel hotel = find(hotelId);
        hotel.setQrImageUrl(storage.store(file, "qr"));
        audit.record(hotelId, "HOTEL_QR_UPDATED", "Hotel", hotelId, null);
        return view(hotel);
    }

    @Transactional(readOnly = true)
    public PageResponse<HotelDtos.View> list(Pageable pageable) {
        Page<Hotel> page = hotels.findByDeletedFlagFalse(pageable);
        return PageResponse.of(page.map(this::view));
    }

    @Transactional(readOnly = true)
    public HotelDtos.View get(Long id) {
        return view(find(id));
    }

    @Transactional
    public HotelDtos.View setEnabled(Long id, boolean enabled) {
        Hotel hotel = find(id);
        hotel.setEnabled(enabled);
        hotels.save(hotel);
        tenantStatus.evict(id);
        audit.record(id, enabled ? "HOTEL_ENABLED" : "HOTEL_DISABLED", "Hotel", id, null);
        return view(hotel);
    }

    @Transactional
    public void softDelete(Long id) {
        Hotel hotel = find(id);
        hotel.setDeletedFlag(true);
        hotel.setEnabled(false);
        hotels.save(hotel);
        for (User user : users.findByHotelIdAndDeletedFlagFalse(id)) {
            user.setEnabled(false);
            user.setDeletedFlag(true);
            users.save(user);
        }
        tenantStatus.evict(id);
        audit.record(id, "HOTEL_DELETED", "Hotel", id, null);
    }

    private Hotel find(Long id) {
        return hotels.findByIdAndDeletedFlagFalse(id).orElseThrow(() -> ApiException.notFound("Hotel"));
    }

    private static boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private static void apply(Hotel hotel, String hotelName, String ownerName, String mobileNumber, String address,
                              String city, String state, String country, String pincode, String gstNumber, String upiId) {
        hotel.setHotelName(hotelName.trim());
        hotel.setOwnerName(ownerName.trim());
        hotel.setMobileNumber(mobileNumber.trim());
        hotel.setAddress(address.trim());
        hotel.setCity(city.trim());
        hotel.setState(state.trim());
        hotel.setCountry(country.trim());
        hotel.setPincode(pincode.trim());
        hotel.setGstNumber(gstNumber == null || gstNumber.isBlank() ? null : gstNumber.trim());
        hotel.setUpiId(upiId.trim());
    }

    private HotelDtos.View view(Hotel h) {
        return new HotelDtos.View(h.getId(), h.getHotelName(), h.getOwnerName(), h.getMobileNumber(), h.getEmail(),
                h.getAddress(), h.getCity(), h.getState(), h.getCountry(), h.getPincode(), h.getGstNumber(),
                h.getUpiId(), h.getLogoUrl(), h.getQrImageUrl(), h.getSubscriptionStatus().name(), h.isEnabled());
    }
}
