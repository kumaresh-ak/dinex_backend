package com.example.dineX.hotel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "hotels")
@Getter
@Setter
@NoArgsConstructor
public class Hotel {
    public enum SubscriptionStatus { PENDING, ACTIVE, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_name", nullable = false, length = 160)
    private String hotelName;

    @Column(name = "owner_name", nullable = false, length = 160)
    private String ownerName;

    @Column(name = "mobile_number", nullable = false, length = 30)
    private String mobileNumber;

    @Column(nullable = false, unique = true, length = 190)
    private String email;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 20)
    private String pincode;

    @Column(name = "gst_number", length = 30)
    private String gstNumber;

    @Column(name = "upi_id", nullable = false, length = 120)
    private String upiId;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "qr_image_url", length = 500)
    private String qrImageUrl;

    @Column(name = "menu_qr_token", unique = true, length = 64)
    private String menuQrToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 30)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.PENDING;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "deleted_flag", nullable = false)
    private boolean deletedFlag = false;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
}
