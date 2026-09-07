package com.example.tientienyouthmanagement.service;

import com.example.tientienyouthmanagement.model.YouthMember;
import com.example.tientienyouthmanagement.repository.YouthMemberRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class YouthMemberService {

    private final YouthMemberRepository repository;

    @Autowired
    public YouthMemberService(YouthMemberRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        if (repository.count() == 0) {
            initSampleData();
        }
    }

    private void initSampleData() {
        save(YouthMember.builder()
                .id("DV-TT01")
                .fullName("Nguyễn Văn An")
                .birthYear("2002")
                .gender("Nam")
                .village("Đội 5 (Thôn Tiền Tiến)")
                .position("Bí thư Chi đoàn Thôn")
                .unionCardNumber("TD-892140")
                .joinDate("26/03/2018")
                .residenceStatus("Tại thôn")
                .phone("0982.145.892")
                .classification("Xuất sắc")
                .avatar("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop&crop=faces")
                .activityNotes("Tổ trưởng Tổ Công nghệ số cộng đồng Thôn Tiền Tiến, hướng dẫn bà con trong thôn cài đặt định danh VNeID.")
                .volunteerDays(14)
                .build());

        save(YouthMember.builder()
                .id("DV-TT02")
                .fullName("Trần Thị Mai Anh")
                .birthYear("2001")
                .gender("Nữ")
                .village("Đội 6 (Thôn Tiền Tiến)")
                .position("Phó Bí thư Chi đoàn Thôn")
                .unionCardNumber("TD-763401")
                .joinDate("19/05/2017")
                .residenceStatus("Tại thôn")
                .phone("0974.321.654")
                .classification("Xuất sắc")
                .avatar("https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200&h=200&fit=crop&crop=faces")
                .activityNotes("Phụ trách đội văn nghệ Thôn Tiền Tiến, phụ trách sinh hoạt hè và dạy múa hát cho các em thiếu nhi tại Nhà văn hóa thôn.")
                .volunteerDays(16)
                .build());

        save(YouthMember.builder()
                .id("DV-TT03")
                .fullName("Lê Hoàng Minh")
                .birthYear("2003")
                .gender("Nam")
                .village("Đội 7 (Thôn Tiền Tiến)")
                .position("Chi ủy viên Chi đoàn")
                .unionCardNumber("TD-665123")
                .joinDate("26/03/2019")
                .residenceStatus("Tại thôn")
                .phone("0912.876.543")
                .classification("Xuất sắc")
                .avatar("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=200&fit=crop&crop=faces")
                .activityNotes("Tham gia tổ an ninh tuần tra Thôn Tiền Tiến, tích cực tham gia dọn dẹp đường làng ngõ xóm.")
                .volunteerDays(18)
                .build());

        save(YouthMember.builder()
                .id("DV-TT04")
                .fullName("Phạm Quỳnh Trang")
                .birthYear("2004")
                .gender("Nữ")
                .village("Đội 8 (Thôn Tiền Tiến)")
                .position("Đoàn viên")
                .unionCardNumber("TD-902345")
                .joinDate("26/03/2020")
                .residenceStatus("Sinh viên học xa")
                .phone("0965.432.189")
                .classification("Khá")
                .avatar("https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&h=200&fit=crop&crop=faces")
                .activityNotes("Sinh viên ĐH Sư phạm, tham gia sinh hoạt hè tại Thôn Tiền Tiến, phụ trách lớp ôn tập hè miễn phí cho học sinh trong thôn.")
                .volunteerDays(8)
                .build());

        save(YouthMember.builder()
                .id("DV-TT05")
                .fullName("Vũ Đức Thịnh")
                .birthYear("2000")
                .gender("Nam")
                .village("Đội 15 (Thôn Tiền Tiến)")
                .position("Đoàn viên phát triển kinh tế")
                .unionCardNumber("TD-554128")
                .joinDate("26/03/2016")
                .residenceStatus("Tại thôn")
                .phone("0903.214.567")
                .classification("Xuất sắc")
                .avatar("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&h=200&fit=crop&crop=faces")
                .activityNotes("Thanh niên làm kinh tế giỏi Thôn Tiền Tiến (mô hình nuôi ốc bươu đen và vườn cây ăn trái), tạo việc làm cho thanh niên thôn.")
                .volunteerDays(11)
                .build());

        save(YouthMember.builder()
                .id("DV-TT06")
                .fullName("Hoàng Bảo Ngọc")
                .birthYear("2005")
                .gender("Nữ")
                .village("Đội 5 (Thôn Tiền Tiến)")
                .position("Đoàn viên")
                .unionCardNumber("TD-112349")
                .joinDate("26/03/2021")
                .residenceStatus("Sinh viên học xa")
                .phone("0948.765.432")
                .classification("Khá")
                .avatar("https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop&crop=faces")
                .activityNotes("Sinh viên trường Cao đẳng, nộp phiếu sinh hoạt hè tại Chi đoàn Thôn Tiền Tiến.")
                .volunteerDays(5)
                .build());

        save(YouthMember.builder()
                .id("DV-TT07")
                .fullName("Đặng Quốc Tuấn")
                .birthYear("2002")
                .gender("Nam")
                .village("Đội 6 (Thôn Tiền Tiến)")
                .position("Đoàn viên")
                .unionCardNumber("TD-443901")
                .joinDate("26/03/2018")
                .residenceStatus("Đi làm ăn xa")
                .phone("0932.119.876")
                .classification("Khá")
                .avatar("https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200&h=200&fit=crop&crop=faces")
                .activityNotes("Lao động tại KCN Bắc Ninh, giữ liên lạc thường xuyên qua nhóm Zalo Chi đoàn Thôn Tiền Tiến.")
                .volunteerDays(2)
                .build());

        save(YouthMember.builder()
                .id("DV-TT08")
                .fullName("Bùi Thị Thu Hằng")
                .birthYear("1999")
                .gender("Nữ")
                .village("Đội 7 (Thôn Tiền Tiến)")
                .position("Đoàn viên tích cực")
                .unionCardNumber("TD-332187")
                .joinDate("26/03/2015")
                .residenceStatus("Tại thôn")
                .phone("0978.654.123")
                .classification("Xuất sắc")
                .avatar("https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=200&h=200&fit=crop&crop=faces")
                .activityNotes("Tham gia đội bóng chuyền hơi thanh niên Thôn Tiền Tiến và các hoạt động trồng hoa dọc đường trục thôn.")
                .volunteerDays(15)
                .build());
    }

    @Transactional(readOnly = true)
    public List<YouthMember> getAll() {
        return repository.findAllByOrderByVillageAscFullNameAsc();
    }

    @Transactional(readOnly = true)
    public List<YouthMember> search(String search, String village, String residenceStatus) {
        return repository.searchMembers(search, village, residenceStatus);
    }

    @Transactional(readOnly = true)
    public YouthMember getById(String id) {
        return repository.findById(id).orElse(null);
    }

    public YouthMember save(YouthMember member) {
        if (member.getId() == null || member.getId().isBlank()) {
            member.setId("DV-TT" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        }
        if (member.getAvatar() == null || member.getAvatar().isBlank()) {
            member.setAvatar("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&h=200&fit=crop&crop=faces");
        }
        if (member.getUnionCardNumber() == null || member.getUnionCardNumber().isBlank()) {
            member.setUnionCardNumber("TD-" + (int) (100000 + Math.random() * 900000));
        }
        if (member.getClassification() == null || member.getClassification().isBlank()) {
            member.setClassification("Khá");
        }
        return repository.save(member);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }

    public int importMembers(List<YouthMember> members) {
        int count = 0;
        for (YouthMember m : members) {
            save(m);
            count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        long total = repository.count();

        long localCount = repository.countByResidenceStatusIgnoreCase("Tại thôn");
        long awayCount = repository.countByResidenceStatusIgnoreCase("Đi làm ăn xa");
        long studentCount = repository.countByResidenceStatusIgnoreCase("Sinh viên học xa");
        long volunteerTotal = repository.sumVolunteerDays();
        long excellentCount = repository.countByClassificationIgnoreCase("Xuất sắc");

        stats.put("totalMembers", total);
        stats.put("localCount", localCount);
        stats.put("awayCount", awayCount);
        stats.put("studentCount", studentCount);
        stats.put("volunteerTotal", volunteerTotal);
        stats.put("excellentCount", excellentCount);

        // Group by village
        List<YouthMember> all = repository.findAll();
        Map<String, Long> villageDistribution = all.stream()
                .filter(m -> m.getVillage() != null)
                .collect(Collectors.groupingBy(YouthMember::getVillage, Collectors.counting()));
        stats.put("villageDistribution", villageDistribution);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<YouthMember> getTopVolunteers() {
        return repository.findTop5ByOrderByVolunteerDaysDesc();
    }

    public List<String> getAllVillages() {
        return Arrays.asList(
                "Đội 5 (Thôn Tiền Tiến)",
                "Đội 6 (Thôn Tiền Tiến)",
                "Đội 7 (Thôn Tiền Tiến)",
                "Đội 8 (Thôn Tiền Tiến)",
                "Đội 15 (Thôn Tiền Tiến)"
        );
    }

    public List<String> getAllResidenceStatuses() {
        return Arrays.asList(
                "Tại thôn",
                "Đi làm ăn xa",
                "Sinh viên học xa",
                "Tạm vắng"
        );
    }
}
