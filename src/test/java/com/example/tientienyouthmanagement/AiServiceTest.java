package com.example.tientienyouthmanagement;

import com.example.tientienyouthmanagement.model.YouthMember;
import com.example.tientienyouthmanagement.service.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiServiceTest {

    @Autowired
    private AiService aiService;

    @Test
    void testAskCopilotWithVillageData() {
        Map<String, Object> resp = aiService.askCopilot("Thôn Tiền Tiến có bao nhiêu đoàn viên?");
        assertNotNull(resp);
        String reply = (String) resp.get("reply");
        assertNotNull(reply);
        assertTrue(reply.contains("Thôn Tiền Tiến") || reply.contains("đoàn viên"));
    }

    @Test
    void testGenerateMemberReviewSummer() {
        YouthMember member = YouthMember.builder()
                .fullName("Nguyễn Văn An")
                .position("Bí thư Chi đoàn")
                .village("Xóm 1 (Thôn Tiền Tiến)")
                .residenceStatus("Tại thôn")
                .volunteerDays(14)
                .classification("Xuất sắc")
                .build();

        String review = aiService.generateMemberReview(member, "summer_review");
        assertNotNull(review);
        assertTrue(review.contains("Nguyễn Văn An"));
        assertTrue(review.contains("14 buổi"));
    }

    @Test
    void testParseUnstructuredText() {
        String rawText = "1. Trần Văn Tuấn, 2003, Xóm 2, 0987.123.456, Đoàn viên\n" +
                         "2. Hoàng Thị Hương - 2004 - Cụm Đình - Chùa - Sinh viên - 0978.888.999";

        List<YouthMember> list = aiService.parseUnstructuredText(rawText);
        assertEquals(2, list.size());
        assertEquals("Trần Văn Tuấn", list.get(0).getFullName());
        assertEquals("2003", list.get(0).getBirthYear());
        assertTrue(list.get(0).getVillage().contains("Xóm 2"));

        assertEquals("Hoàng Thị Hương", list.get(1).getFullName());
        assertEquals("2004", list.get(1).getBirthYear());
        assertEquals("Sinh viên học xa", list.get(1).getResidenceStatus());
    }
}
