package com.example.tientienyouthmanagement.controller;

import com.example.tientienyouthmanagement.model.YouthMember;
import com.example.tientienyouthmanagement.service.AiService;
import com.example.tientienyouthmanagement.service.YouthMemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiApiController {

    private final AiService aiService;
    private final YouthMemberService youthMemberService;

    public AiApiController(AiService aiService, YouthMemberService youthMemberService) {
        this.aiService = aiService;
        this.youthMemberService = youthMemberService;
    }

    /**
     * API Chat với Trợ lý AI Chi đoàn Thôn Tiền Tiến
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        Map<String, Object> result = aiService.askCopilot(message);
        return ResponseEntity.ok(result);
    }

    /**
     * API Tự động viết nhận xét đoàn viên bằng AI
     */
    @PostMapping("/generate-review")
    public ResponseEntity<Map<String, String>> generateReview(@RequestBody Map<String, String> request) {
        String memberId = request.get("memberId");
        String reviewType = request.get("reviewType");

        YouthMember member = null;
        if (memberId != null && !memberId.isBlank()) {
            member = youthMemberService.getById(memberId);
        }

        if (member == null) {
            // Có thể truyền trực tiếp thông tin từ form client
            member = YouthMember.builder()
                    .fullName(request.getOrDefault("fullName", "Đoàn viên"))
                    .position(request.getOrDefault("position", "Đoàn viên"))
                    .village(request.getOrDefault("village", "Xóm 1 (Thôn Tiền Tiến)"))
                    .residenceStatus(request.getOrDefault("residenceStatus", "Tại thôn"))
                    .classification(request.getOrDefault("classification", "Khá"))
                    .activityNotes(request.getOrDefault("activityNotes", ""))
                    .volunteerDays(Integer.parseInt(request.getOrDefault("volunteerDays", "2")))
                    .build();
        }

        String review = aiService.generateMemberReview(member, reviewType);
        Map<String, String> resp = new HashMap<>();
        resp.put("review", review);
        return ResponseEntity.ok(resp);
    }

    /**
     * API AI Bóc tách văn bản tự do thành danh sách đoàn viên
     */
    @PostMapping("/parse-text")
    public ResponseEntity<Map<String, Object>> parseText(@RequestBody Map<String, String> request) {
        String rawText = request.get("rawText");
        List<YouthMember> parsed = aiService.parseUnstructuredText(rawText);
        Map<String, Object> resp = new HashMap<>();
        resp.put("count", parsed.size());
        resp.put("members", parsed);
        return ResponseEntity.ok(resp);
    }

    /**
     * API Lưu nhanh danh sách đoàn viên vừa được AI bóc tách
     */
    @PostMapping("/save-imported-members")
    public ResponseEntity<Map<String, Object>> saveImportedMembers(@RequestBody List<YouthMember> members) {
        int count = youthMemberService.importMembers(members);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("importedCount", count);
        resp.put("message", "Đã lưu thành công " + count + " đoàn viên vào Sổ Chi Đoàn Thôn Tiền Tiến!");
        return ResponseEntity.ok(resp);
    }
}
