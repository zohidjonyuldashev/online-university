package uz.pdp.online_university.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.pdp.online_university.entity.ApplicantProfile;

import java.util.Optional;

@Repository
public interface ApplicantProfileRepository extends JpaRepository<ApplicantProfile, Long> {
    Optional<ApplicantProfile> findByUserId(Long userId);
}
