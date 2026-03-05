package uz.pdp.online_university.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.pdp.online_university.entity.OtpVerification;
import uz.pdp.online_university.enums.OtpType;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByUserIdAndTypeAndVerifiedFalseOrderByCreatedAtDesc(
            Long userId, OtpType type
    );
}
