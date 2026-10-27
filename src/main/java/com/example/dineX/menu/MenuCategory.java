package com.example.dineX.menu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "menu_categories")
@Getter
@Setter
@NoArgsConstructor
public class MenuCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "display_priority", nullable = false)
    private int displayPriority;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "deleted_flag", nullable = false)
    private boolean deletedFlag = false;

    @Column(name = "created_date", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdDate;
}
