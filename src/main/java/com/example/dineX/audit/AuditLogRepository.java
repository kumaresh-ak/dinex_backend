package com.example.dineX.audit;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:hotelId is null or a.hotelId = :hotelId)
              and (:action is null or a.action = :action)
              and (:from is null or a.createdDate >= :from)
              and (:to is null or a.createdDate < :to)
            order by a.id desc
            """)
    List<AuditLog> search(@Param("hotelId") Long hotelId,
                          @Param("action") String action,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          Pageable pageable);
}
