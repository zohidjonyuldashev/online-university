package uz.pdp.online_university.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.pdp.online_university.entity.ApplicantStatusHistory;

import java.util.List;

@Repository
public interface ApplicantStatusHistoryRepository extends JpaRepository<ApplicantStatusHistory, Long> {
    List<ApplicantStatusHistory> findByApplicantProfileIdOrderByTimestampDesc(Long profileId);
}
