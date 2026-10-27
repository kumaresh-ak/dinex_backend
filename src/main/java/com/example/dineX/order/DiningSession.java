package com.example.dineX.order;

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
@Table(name = "dining_sessions")
@Getter
@Setter
@NoArgsConstructor
public class DiningSession {
    public enum Status { OPEN, BILL_GENERATED, PAYMENT_PENDING, PAID, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "session_number", nullable = false, unique = true, length = 40)
    private String sessionNumber;

    @Column(name = "session_token", unique = true, length = 64)
    private String sessionToken;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", length = 160)
    private String customerName;

    @Column(name = "customer_mobile", length = 30)
    private String customerMobile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.OPEN;

    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
