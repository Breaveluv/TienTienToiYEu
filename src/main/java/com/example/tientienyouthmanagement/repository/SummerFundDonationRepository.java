package com.example.tientienyouthmanagement.repository;

import com.example.tientienyouthmanagement.model.SummerFundDonation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SummerFundDonationRepository extends JpaRepository<SummerFundDonation, Long> {

    List<SummerFundDonation> findAllByOrderByDonationDateDescIdDesc();

    List<SummerFundDonation> findTop5ByOrderByAmountDesc();

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM SummerFundDonation d")
    long sumTotalDonationAmount();

    @Query("SELECT COUNT(d) FROM SummerFundDonation d WHERE d.itemDonation IS NOT NULL AND TRIM(d.itemDonation) != ''")
    long countItemDonations();

    @Query("SELECT d FROM SummerFundDonation d WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(d.donorName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(d.villageOrUnit) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(d.receiptCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(d.itemDonation) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:purpose IS NULL OR :purpose = '' OR :purpose = 'all' OR LOWER(d.donationPurpose) = LOWER(:purpose)) AND " +
           "(:village IS NULL OR :village = '' OR :village = 'all' OR LOWER(d.villageOrUnit) = LOWER(:village)) " +
           "ORDER BY d.donationDate DESC, d.id DESC")
    List<SummerFundDonation> searchDonations(@Param("search") String search,
                                            @Param("purpose") String purpose,
                                            @Param("village") String village);
}
