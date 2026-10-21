package hu.kolozsvaripeter.parceltracker.repository;

import hu.kolozsvaripeter.parceltracker.domain.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
}
