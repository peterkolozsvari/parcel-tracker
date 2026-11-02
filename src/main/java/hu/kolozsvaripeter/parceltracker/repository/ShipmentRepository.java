package hu.kolozsvaripeter.parceltracker.repository;

import hu.kolozsvaripeter.parceltracker.domain.Shipment;
import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Shipment s WHERE s.id = :id")
    Optional<Shipment> findByIdForUpdate(@Param("id") Long id);

    boolean existsByTrackingNumber(String trackingNumber);
    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    @Query("""
        SELECT s FROM Shipment s
        WHERE (:status IS NULL OR s.status = :status)
          AND (:senderId IS NULL OR s.sender.id = :senderId)
        """)
    Page<Shipment> findAllByFilters(
            @Param("status") ShipmentStatus status,
            @Param("senderId") Long senderId,
            Pageable pageable
    );
}

