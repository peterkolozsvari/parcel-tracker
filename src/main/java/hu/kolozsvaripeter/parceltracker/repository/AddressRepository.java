package hu.kolozsvaripeter.parceltracker.repository;

import hu.kolozsvaripeter.parceltracker.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
}
