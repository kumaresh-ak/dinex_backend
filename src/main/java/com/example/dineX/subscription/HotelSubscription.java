package com.example.dineX.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hotel_subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class HotelSubscription {
    public enum Status { PENDING_APPROVAL, ACTIVE, EXPIRED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "payment_reference", length = 120)
    private String paymentReference;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_proof_url", length = 500)
    private String paymentProofUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.PENDING_APPROVAL;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "created_date", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdDate;
}
