package hu.kolozsvaripeter.parceltracker.repository;

import hu.kolozsvaripeter.parceltracker.domain.User;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    List<User> findAllByRoleAndActiveTrueOrderByFullNameAscIdAsc(Role role);
}

