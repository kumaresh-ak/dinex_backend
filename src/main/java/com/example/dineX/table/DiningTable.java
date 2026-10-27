package com.example.dineX.table;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dining_tables")
@Getter
@Setter
@NoArgsConstructor
public class DiningTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "table_number", nullable = false, length = 30)
    private String tableNumber;

    @Column(name = "table_name", length = 100)
    private String tableName;

    @Column(nullable = false)
    private int capacity = 4;

    @Column(name = "qr_token", unique = true, length = 64)
    private String qrToken;

    @Column(name = "qr_url", length = 500)
    private String qrUrl;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "deleted_flag", nullable = false)
    private boolean deletedFlag = false;
}
