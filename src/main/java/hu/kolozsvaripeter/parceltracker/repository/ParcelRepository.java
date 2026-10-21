package hu.kolozsvaripeter.parceltracker.repository;

import hu.kolozsvaripeter.parceltracker.domain.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParcelRepository extends JpaRepository<Shipment, Long> {
}
