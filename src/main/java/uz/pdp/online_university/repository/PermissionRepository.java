package uz.pdp.online_university.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.pdp.online_university.entity.Permission;
import uz.pdp.online_university.enums.RoleName;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByKey(String key);

    boolean existsByKey(String key);

    List<Permission> findAllByModule(String module);

    Set<Permission> findAllByKeyIn(Set<String> keys);

    @Query("""
            SELECT p FROM Role r JOIN r.permissions p
            WHERE r.name = :roleName
            """)
    Set<Permission> findAllByRoleName(@Param("roleName") RoleName roleName);
}