package com.example.tientienyouthmanagement.service;

import com.example.tientienyouthmanagement.model.SummerFundDonation;
import com.example.tientienyouthmanagement.repository.SummerFundDonationRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class SummerFundService {

    private final SummerFundDonationRepository repository;

    public SummerFundService(SummerFundDonationRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        if (repository.count() == 0) {
            initSampleDonations();
        }
    }

    public List<SummerFundDonation> getAllDonations() {
        return repository.findAllByOrderByDonationDateDescIdDesc();
    }

    public List<SummerFundDonation> search(String search, String purpose, String village) {
        return repository.searchDonations(search, purpose, village);
    }

    public SummerFundDonation save(SummerFundDonation donation) {
        if (donation.getDonationDate() == null) {
            donation.setDonationDate(LocalDate.now());
        }
        if (donation.getReceiptCode() == null || donation.getReceiptCode().isBlank()) {
            donation.setReceiptCode(generateReceiptCode());
        }
        if (donation.getAmount() == null) {
            donation.setAmount(0L);
        }
        return repository.save(donation);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public void deleteAllDonations() {
        repository.deleteAll();
    }

    public int importDonations(List<SummerFundDonation> donations) {
        if (donations == null || donations.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (SummerFundDonation donation : donations) {
            if (donation.getDonorName() != null && !donation.getDonorName().isBlank()) {
                save(donation);
                count++;
            }
        }
        return count;
    }

    public Map<String, Object> getFundStatistics() {
        long totalAmount = repository.sumTotalDonationAmount();
        long targetAmount = 25_000_000L; // Chỉ tiêu hoạt động hè: 25 triệu VNĐ
        double percentAchieved = targetAmount > 0 ? ((double) totalAmount / targetAmount) * 100.0 : 0.0;
        long donorCount = repository.count();
        long itemCount = repository.countItemDonations();
        List<SummerFundDonation> topDonors = repository.findTop5ByOrderByAmountDesc();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAmount", totalAmount);
        stats.put("targetAmount", targetAmount);
        stats.put("percentAchieved", Math.round(percentAchieved * 10.0) / 10.0);
        stats.put("donorCount", donorCount);
        stats.put("itemCount", itemCount);
        stats.put("topDonors", topDonors);
        return stats;
    }

    public List<String> getAllPurposes() {
        return List.of(
                "Trại hè Thiếu nhi",
                "Giải bóng đá Thanh thiếu nhi",
                "Đêm hội Văn nghệ hè",
                "Khen thưởng Học sinh giỏi hè",
                "Ủng hộ chung Hoạt động hè"
        );
    }

    public List<String> getAllVillages() {
        return List.of(
                "Đội 5",
                "Đội 6",
                "Đội 7",
                "Đội 8",
                "Đội 15",
                "Con em làm ăn xa quê",
                "Doanh nghiệp / Mạnh thường quân",
                "Ban ngành đoàn thể thôn"
        );
    }

    private String generateReceiptCode() {
        long nextIndex = repository.count() + 1;
        return String.format("QHH-2026-%03d", nextIndex);
    }

    private void initSampleDonations() {
        List<SummerFundDonation> sampleList = List.of(
                SummerFundDonation.builder()
                        .receiptCode("QHH-2026-001")
                        .donorName("Bác Nguyễn Văn Thắng")
                        .donorTitle("Trưởng Ban Công tác Mặt trận Thôn")
                        .villageOrUnit("Đội 7")
                        .amount(1_500_000L)
                        .itemDonation("1 thùng bánh kẹo thiếu nhi")
                        .donationPurpose("Trại hè Thiếu nhi")
                        .paymentMethod("Tiền mặt")
                        .donationDate(LocalDate.of(2026, 6, 2))
                        .note("Chúc Chi đoàn tổ chức một mùa hè sôi nổi, an toàn và bổ ích cho các cháu thiếu niên nhi đồng Thôn Tiền Tiến!")
                        .build(),

                SummerFundDonation.builder()
                        .receiptCode("QHH-2026-002")
                        .donorName("Công ty TNHH Cơ Khí Tiền Tiến")
                        .donorTitle("Doanh nghiệp địa phương")
                        .villageOrUnit("Doanh nghiệp / Mạnh thường quân")
                        .amount(3_000_000L)
                        .itemDonation("Cúp & cờ lưu niệm giải đấu")
                        .donationPurpose("Giải bóng đá Thanh thiếu nhi")
                        .paymentMethod("Chuyển khoản VietQR")
                        .donationDate(LocalDate.of(2026, 6, 4))
                        .note("Đồng hành cùng phong trào thể dục thể thao, rèn luyện sức khỏe thanh niên hè 2026.")
                        .build(),

                SummerFundDonation.builder()
                        .receiptCode("QHH-2026-003")
                        .donorName("Anh Lê Hoàng Nam")
                        .donorTitle("Cựu Bí thư Chi đoàn (Xa quê tại Hà Nội)")
                        .villageOrUnit("Con em làm ăn xa quê")
                        .amount(2_000_000L)
                        .itemDonation(null)
                        .donationPurpose("Đêm hội Văn nghệ hè")
                        .paymentMethod("Chuyển khoản VietQR")
                        .donationDate(LocalDate.of(2026, 6, 8))
                        .note("Gửi chút tình cảm của người con xa quê tiếp sức cho thế hệ măng non và các bạn trẻ thôn nhà!")
                        .build(),

                SummerFundDonation.builder()
                        .receiptCode("QHH-2026-004")
                        .donorName("Gia đình Bác Phạm Văn Hải")
                        .donorTitle("Bà con nhân dân")
                        .villageOrUnit("Đội 5")
                        .amount(500_000L)
                        .itemDonation("2 thùng nước khoáng đóng chai")
                        .donationPurpose("Ủng hộ chung Hoạt động hè")
                        .paymentMethod("Tiền mặt")
                        .donationDate(LocalDate.of(2026, 6, 11))
                        .note("Ủng hộ các bạn đoàn viên phục vụ nước uống cho các cháu sinh hoạt hè.")
                        .build(),

                SummerFundDonation.builder()
                        .receiptCode("QHH-2026-005")
                        .donorName("Chi hội Phụ nữ Thôn Tiền Tiến")
                        .donorTitle("Ban ngành đoàn thể thôn")
                        .villageOrUnit("Ban ngành đoàn thể thôn")
                        .amount(1_000_000L)
                        .itemDonation("Hỗ trợ nấu chè liên hoan khai mạc hè")
                        .donationPurpose("Trại hè Thiếu nhi")
                        .paymentMethod("Tiền mặt")
                        .donationDate(LocalDate.of(2026, 6, 12))
                        .note("Phối hợp cùng Chi đoàn chăm lo đời sống tinh thần cho các cháu thiếu nhi.")
                        .build(),

                SummerFundDonation.builder()
                        .receiptCode("QHH-2026-006")
                        .donorName("Cửa Hàng Tạp Hóa Cô Mai")
                        .donorTitle("Hộ kinh doanh đội 6")
                        .villageOrUnit("Đội 6")
                        .amount(800_000L)
                        .itemDonation("50 chiếc khăn quàng đỏ + 5 quả bóng chuyền")
                        .donationPurpose("Khen thưởng Học sinh giỏi hè")
                        .paymentMethod("Tiền mặt")
                        .donationDate(LocalDate.of(2026, 6, 15))
                        .note("Tặng quà khích lệ các em học sinh có hoàn cảnh vươn lên học tốt.")
                        .build(),

                SummerFundDonation.builder()
                        .receiptCode("QHH-2026-007")
                        .donorName("Anh Trần Văn Dũng & Nhóm Bạn trẻ Tiền Tiến")
                        .donorTitle("Đoàn viên thanh niên")
                        .villageOrUnit("Con em làm ăn xa quê")
                        .amount(2_500_000L)
                        .itemDonation("Bộ loa kéo di động mini phục vụ tập văn nghệ hè")
                        .donationPurpose("Đêm hội Văn nghệ hè")
                        .paymentMethod("Chuyển khoản VietQR")
                        .donationDate(LocalDate.of(2026, 6, 18))
                        .note("Tuổi trẻ Tiền Tiến xung kích, nhiệt huyết vì quê hương!")
                        .build(),

                SummerFundDonation.builder()
                        .receiptCode("QHH-2026-008")
                        .donorName("Nhà Xe Tiến Thuận")
                        .donorTitle("Doanh nghiệp vận tải")
                        .villageOrUnit("Doanh nghiệp / Mạnh thường quân")
                        .amount(2_000_000L)
                        .itemDonation(null)
                        .donationPurpose("Giải bóng đá Thanh thiếu nhi")
                        .paymentMethod("Chuyển khoản VietQR")
                        .donationDate(LocalDate.of(2026, 6, 20))
                        .note("Ủng hộ giải bóng đá giao lưu giữa các chi đoàn cụm.")
                        .build()
        );

        repository.saveAll(sampleList);
    }
}
