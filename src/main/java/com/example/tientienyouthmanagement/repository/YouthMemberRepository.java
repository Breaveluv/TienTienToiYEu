package com.example.tientienyouthmanagement.repository;

import com.example.tientienyouthmanagement.model.YouthMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface YouthMemberRepository extends JpaRepository<YouthMember, String> {

    List<YouthMember> findAllByOrderByVillageAscFullNameAsc();

    @Query("SELECT m FROM YouthMember m WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(m.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(m.village) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(m.position) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " m.phone LIKE CONCAT('%', :search, '%') OR " +
           " LOWER(m.unionCardNumber) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:village IS NULL OR :village = '' OR :village = 'all' OR LOWER(m.village) = LOWER(:village)) AND " +
           "(:residenceStatus IS NULL OR :residenceStatus = '' OR :residenceStatus = 'all' OR LOWER(m.residenceStatus) = LOWER(:residenceStatus)) " +
           "ORDER BY m.village ASC, m.fullName ASC")
    List<YouthMember> searchMembers(@Param("search") String search,
                                    @Param("village") String village,
                                    @Param("residenceStatus") String residenceStatus);

    List<YouthMember> findTop5ByOrderByVolunteerDaysDesc();

    long countByResidenceStatusIgnoreCase(String residenceStatus);

    long countByClassificationIgnoreCase(String classification);

    @Query("SELECT COALESCE(SUM(m.volunteerDays), 0) FROM YouthMember m")
    long sumVolunteerDays();
}
