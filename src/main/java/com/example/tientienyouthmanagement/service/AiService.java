package com.example.tientienyouthmanagement.service;

import com.example.tientienyouthmanagement.model.YouthMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final YouthMemberService youthMemberService;
    private final ChatModel chatModel; // Optional Spring AI ChatModel

    @Autowired
    public AiService(YouthMemberService youthMemberService, @Autowired(required = false) ChatModel chatModel) {
        this.youthMemberService = youthMemberService;
        this.chatModel = chatModel;
    }

    /**
     * Trả lời câu hỏi của cán bộ Chi đoàn thông qua AI Copilot
     */
    public Map<String, Object> askCopilot(String userPrompt) {
        Map<String, Object> response = new HashMap<>();
        String trimmed = userPrompt != null ? userPrompt.trim() : "";
        if (trimmed.isEmpty()) {
            response.put("reply", "Chào đồng chí! Tôi có thể giúp gì cho công tác Chi đoàn Thôn Tiền Tiến hôm nay?");
            response.put("suggestions", getSuggestedQuestions());
            return response;
        }

        // Tạo System Context động từ dữ liệu thực tế của Thôn Tiền Tiến
        String dynamicContext = buildVillageContext();

        String aiReply = null;
        if (chatModel != null) {
            try {
                String fullSystemPrompt = "Bạn là Trợ lý AI Chi đoàn Thôn Tiền Tiến (Đoàn TNCS Hồ Chí Minh). " +
                        "Hãy trả lời với phong thái người đồng chí năng nổ, chuẩn mực công tác thanh niên, chính xác theo số liệu sau:\n" +
                        dynamicContext + "\n\nCâu hỏi: " + trimmed;
                aiReply = chatModel.call(fullSystemPrompt);
            } catch (Exception e) {
                log.warn("Spring AI ChatModel call failed or not configured, using smart local engine: {}", e.getMessage());
            }
        }

        if (aiReply == null || aiReply.isBlank()) {
            aiReply = generateLocalCopilotReply(trimmed);
        }

        response.put("reply", aiReply);
        response.put("suggestions", getSuggestedQuestions());
        return response;
    }

    /**
     * Tự động sinh nhận xét / đánh giá Đoàn viên phù hợp với từng hoàn cảnh
     */
    public String generateMemberReview(YouthMember member, String reviewType) {
        if (member == null) {
            return "Đồng chí chấp hành tốt chủ trương của Đảng, pháp luật của Nhà nước và quy chế của địa phương.";
        }

        if (chatModel != null) {
            try {
                String prompt = String.format(
                        "Hãy viết một đoạn nhận xét đoàn viên chuẩn văn phong Đoàn TNCS Hồ Chí Minh (khoảng 3-5 câu):\n" +
                        "- Họ tên: %s\n- Chức vụ: %s\n- Nơi cư trú/sinh hoạt: %s (%s)\n- Số buổi tình nguyện Chủ nhật xanh: %d buổi\n" +
                        "- Hoạt động cụ thể: %s\n- Xếp loại: %s\n- Loại nhận xét yêu cầu: %s (ví dụ: 'summer_review' là sinh hoạt hè nộp về trường, 'year_end' là đánh giá cuối năm, 'commendation' là khen thưởng gương sáng)",
                        member.getFullName(), member.getPosition(), member.getVillage(), member.getResidenceStatus(),
                        member.getVolunteerDays(), member.getActivityNotes(), member.getClassification(),
                        reviewType != null ? reviewType : "summer_review"
                );
                String result = chatModel.call(prompt);
                if (result != null && !result.isBlank()) {
                    return result.trim();
                }
            } catch (Exception e) {
                log.warn("Spring AI call failed for review generation: {}", e.getMessage());
            }
        }

        return generateLocalMemberReview(member, reviewType);
    }

    /**
     * Trích xuất thông tin đoàn viên từ văn bản tự do (Zalo, tin nhắn, biên bản)
     */
    public List<YouthMember> parseUnstructuredText(String rawText) {
        List<YouthMember> members = new ArrayList<>();
        if (rawText == null || rawText.isBlank()) {
            return members;
        }

        String[] lines = rawText.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.length() < 3) continue;

            // Bỏ các dòng tiêu đề chung
            String lower = line.toLowerCase();
            if (lower.startsWith("danh sách") || lower.startsWith("stt") || lower.startsWith("chi đoàn") || lower.startsWith("họ và tên")) {
                continue;
            }

            // Loại bỏ số thứ tự ở đầu dòng (VD: "1.", "1/", "1)", "01 -")
            String cleanLine = line.replaceAll("^[0-9]+[\\.\\)\\-\\:\\/]\\s*", "").trim();

            // Tách các thành phần bằng dấu phẩy, gạch ngang, chấm phẩy hoặc tab
            String[] parts = cleanLine.split("[,;\\t|\\-]+");

            String name = "";
            String birthYear = "2003";
            String village = "Xóm 1 (Thôn Tiền Tiến)";
            String position = "Đoàn viên";
            String phone = "0988.xxx.xxx";
            String residence = "Tại thôn";

            if (parts.length >= 1) {
                name = parts[0].trim();
            }

            // Dò tìm số điện thoại trong cả dòng
            Pattern phonePattern = Pattern.compile("(0[3|5|7|8|9][0-9]{8}|0[3|5|7|8|9][0-9]{1}\\.[0-9]{3}\\.[0-9]{3}|0[3|5|7|8|9][0-9]{1}\\s[0-9]{3}\\s[0-9]{3})");
            Matcher phoneMatcher = phonePattern.matcher(line);
            if (phoneMatcher.find()) {
                phone = phoneMatcher.group(1).trim();
            }

            // Dò tìm năm sinh (199x hoặc 200x)
            Pattern yearPattern = Pattern.compile("\\b(199[5-9]|200[0-9]|201[0-2])\\b");
            Matcher yearMatcher = yearPattern.matcher(line);
            if (yearMatcher.find()) {
                birthYear = yearMatcher.group(1);
            }

            // Dò tìm xóm
            if (lower.contains("xóm 1") || lower.contains("xom 1")) village = "Xóm 1 (Thôn Tiền Tiến)";
            else if (lower.contains("xóm 2") || lower.contains("xom 2")) village = "Xóm 2 (Thôn Tiền Tiến)";
            else if (lower.contains("xóm 3") || lower.contains("xom 3")) village = "Xóm 3 (Thôn Tiền Tiến)";
            else if (lower.contains("xóm 4") || lower.contains("xom 4")) village = "Xóm 4 (Thôn Tiền Tiến)";
            else if (lower.contains("đình") || lower.contains("chùa")) village = "Cụm Đình - Chùa (Thôn Tiền Tiến)";
            else if (lower.contains("đồng sau") || lower.contains("dong sau")) village = "Cụm Đồng Sau (Thôn Tiền Tiến)";

            // Dò tìm chức vụ
            if (lower.contains("bí thư") || lower.contains("bi thu")) position = "Bí thư Chi đoàn";
            else if (lower.contains("phó bí thư") || lower.contains("pho bi thu")) position = "Phó Bí thư Chi đoàn";
            else if (lower.contains("chi ủy") || lower.contains("ủy viên")) position = "Chi ủy viên";
            else if (lower.contains("tổ trưởng")) position = "Tổ trưởng tổ Thanh niên";

            // Dò tìm tình trạng cư trú
            if (lower.contains("sinh viên") || lower.contains("đại học") || lower.contains("cao đẳng")) residence = "Sinh viên học xa";
            else if (lower.contains("đi làm") || lower.contains("xa") || lower.contains("công nhân") || lower.contains("kcn")) residence = "Đi làm ăn xa";
            else if (lower.contains("tạm vắng")) residence = "Tạm vắng";

            if (!name.isBlank() && name.length() > 1 && !name.matches("^[0-9]+$")) {
                YouthMember m = YouthMember.builder()
                        .id("DV-TT" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                        .fullName(name)
                        .birthYear(birthYear)
                        .gender(name.toLowerCase().contains("thị") || name.toLowerCase().contains("ngọc") || name.toLowerCase().contains("mai") ? "Nữ" : "Nam")
                        .village(village)
                        .position(position)
                        .phone(phone)
                        .unionCardNumber("TD-" + (int)(100000 + Math.random() * 900000))
                        .joinDate("26/03/2020")
                        .residenceStatus(residence)
                        .classification("Khá")
                        .avatar("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&h=200&fit=crop&crop=faces")
                        .activityNotes("Đoàn viên được AI trích xuất tự động từ văn bản tiếp nhận.")
                        .volunteerDays(2)
                        .build();
                members.add(m);
            }
        }

        return members;
    }

    private String buildVillageContext() {
        Map<String, Object> stats = youthMemberService.getStatistics();
        List<YouthMember> topVolunteers = youthMemberService.getTopVolunteers();
        StringBuilder sb = new StringBuilder();
        sb.append("=== DỮ LIỆU HIỆN TẠI CỦA CHI ĐOÀN THÔN TIỀN TIẾN ===\n");
        sb.append("- Tổng số đoàn viên trong sổ: ").append(stats.get("totalMembers")).append(" đồng chí\n");
        sb.append("- Đoàn viên đang sinh hoạt tại thôn: ").append(stats.get("localCount")).append(" đồng chí\n");
        sb.append("- Đoàn viên đi làm ăn xa: ").append(stats.get("awayCount")).append(" đồng chí\n");
        sb.append("- Đoàn viên là sinh viên học tập xa: ").append(stats.get("studentCount")).append(" đồng chí\n");
        sb.append("- Tổng số lượt tham gia Ngày Chủ nhật xanh: ").append(stats.get("volunteerTotal")).append(" buổi\n");
        sb.append("- Đoàn viên xếp loại Xuất sắc: ").append(stats.get("excellentCount")).append(" đồng chí\n");

        sb.append("\nTop đoàn viên tích cực nhất:\n");
        for (YouthMember m : topVolunteers) {
            sb.append(String.format(" + %s (%s - %s): %d buổi tình nguyện, Ghi chú: %s\n",
                    m.getFullName(), m.getPosition(), m.getVillage(), m.getVolunteerDays(), m.getActivityNotes()));
        }
        return sb.toString();
    }

    private String generateLocalCopilotReply(String q) {
        String lower = q.toLowerCase();
        Map<String, Object> stats = youthMemberService.getStatistics();

        if (lower.contains("tổng số") || lower.contains("bao nhiêu đoàn viên") || lower.contains("thống kê") || lower.contains("tình hình")) {
            return String.format(
                    "Báo cáo đồng chí, hiện tại Chi đoàn Thôn Tiền Tiến có tổng cộng **%s đoàn viên**.\n\n" +
                    "📊 **Phân bổ nhân lực:**\n" +
                    "- Đoàn viên sinh hoạt trực tiếp tại thôn: **%s đồng chí** (lực lượng nòng cốt cho các hoạt động địa phương)\n" +
                    "- Đoàn viên đi làm ăn xa: **%s đồng chí** (duy trì sinh hoạt qua nhóm Zalo Chi đoàn)\n" +
                    "- Đoàn viên là sinh viên đang học tập: **%s đồng chí** (tham gia đông đảo vào chiến dịch hè)\n" +
                    "- Đoàn viên xếp loại Xuất sắc: **%s đồng chí**\n" +
                    "- Tổng lượt tham gia phong trào Ngày Chủ nhật xanh: **%s buổi**.",
                    stats.get("totalMembers"), stats.get("localCount"), stats.get("awayCount"),
                    stats.get("studentCount"), stats.get("excellentCount"), stats.get("volunteerTotal")
            );
        }

        if (lower.contains("đi làm ăn xa") || lower.contains("làm xa")) {
            return String.format(
                    "Hiện tại Thôn Tiền Tiến có **%s đoàn viên** đang đi làm ăn xa (chủ yếu tại các khu công nghiệp Bắc Ninh, Hải Dương, Hà Nội).\n\n" +
                    "💡 **Giải pháp quản lý:**\n" +
                    "1. Chi đoàn duy trì kênh liên lạc thường xuyên qua nhóm Zalo 'Chi đoàn Thôn Tiền Tiến'.\n" +
                    "2. Gửi thông tin đóng đoàn phí định kỳ và thông báo các hoạt động lớn của quê hương.\n" +
                    "3. Khuyến khích tham gia ủng hộ các quỹ phong trào thanh niên xây dựng nông thôn mới nâng cao tại thôn.",
                    stats.get("awayCount")
            );
        }

        if (lower.contains("sinh viên") || lower.contains("học xa") || lower.contains("hè")) {
            return String.format(
                    "Thôn Tiền Tiến hiện có **%s đồng chí** là sinh viên các trường Cao đẳng, Đại học về sinh hoạt hè.\n\n" +
                    "🌟 **Định hướng hoạt động:**\n" +
                    "- Đã phân công các đồng chí phụ trách lớp ôn tập hè miễn phí cho thiếu nhi tại Nhà văn hóa thôn.\n" +
                    "- Đảm nhận dàn dựng chương trình văn nghệ Đêm hội Trăng rằm và Hội trại thanh thiếu nhi cấp xã.\n" +
                    "- Hỗ trợ Tổ công nghệ số cộng đồng hướng dẫn bà con dịch vụ công trực tuyến.",
                    stats.get("studentCount")
            );
        }

        if (lower.contains("tích cực") || lower.contains("tình nguyện") || lower.contains("xuất sắc") || lower.contains("top")) {
            List<YouthMember> top = youthMemberService.getTopVolunteers();
            StringBuilder sb = new StringBuilder("Gương mặt tiêu biểu dẫn đầu phong trào hành động của Chi đoàn Thôn Tiền Tiến:\n\n");
            int rank = 1;
            for (YouthMember m : top) {
                sb.append(String.format("%d. **%s** (%s - %s): **%d buổi** Chủ nhật xanh. Hoạt động: *%s*\n",
                        rank++, m.getFullName(), m.getPosition(), m.getVillage(), m.getVolunteerDays(), m.getActivityNotes()));
            }
            return sb.toString();
        }

        if (lower.contains("chủ nhật xanh") || lower.contains("kế hoạch") || lower.contains("hoạt động")) {
            return "📌 **Gợi ý Kế hoạch Ra quân 'Ngày Chủ nhật xanh' Thôn Tiền Tiến:**\n\n" +
                    "1. **Mục tiêu:** Dọn dẹp vệ sinh tuyến đường trục chính từ Cổng chào thôn qua Cụm Đình - Chùa đến Nhà văn hóa thôn.\n" +
                    "2. **Lực lượng huy động:** Khoảng 25-30 đoàn viên (ưu tiên các đồng chí tại thôn và sinh viên nghỉ hè).\n" +
                    "3. **Phân công cụ thể:**\n" +
                    "   - *Xóm 1 & Xóm 2:* Phụ trách cắt tỉa hàng rào hoa và quét dọn đường ngõ xóm.\n" +
                    "   - *Xóm 3 & Xóm 4:* Khơi thông dòng chảy mương thoát nước khu vực Cụm Đồng Sau.\n" +
                    "   - *Đội thanh niên xung kích:* Thu gom rác thải nhựa và tập kết xử lý đúng quy định.\n" +
                    "4. **Thời gian:** 07h00 Chủ nhật, tập trung tại Nhà văn hóa Thôn Tiền Tiến.";
        }

        if (lower.contains("phát biểu") || lower.contains("diễn văn")) {
            return "🎤 **Dự thảo Gợi ý Bài Phát biểu cho Bí thư Chi đoàn Thôn Tiền Tiến:**\n\n" +
                    "\"Kính thưa Chi ủy, Ban lãnh đạo Thôn Tiền Tiến cùng toàn thể bà con nhân dân và các bạn đoàn viên thanh niên!\n\n" +
                    "Hôm nay, tuổi trẻ Thôn Tiền Tiến rất vinh dự được tề tựu đông đủ để cùng nhìn lại chặng đường cống hiến cho quê hương. Phát huy truyền thống quê hương anh hùng, đoàn viên thanh niên thôn ta luôn tiên phong trong mọi phong trào: từ 'Ngày Chủ nhật xanh', tuyến đường hoa thanh niên, đến phong trào Chuyển đổi số cộng đồng.\n\n" +
                    "Thay mặt Ban Chấp hành Chi đoàn, tôi xin gửi lời cảm ơn sâu sắc đến sự quan tâm chỉ đạo của Chi bộ, sự đồng lòng của toàn thể nhân dân. Tuổi trẻ Thôn Tiền Tiến xin hứa tiếp tục rèn đức luyện tài, xung kích đi đầu xây dựng nông thôn mới ngày càng văn minh, giàu đẹp!\"";
        }

        return String.format(
                "Đồng chí đã hỏi: *\"%s\"*.\n\n" +
                "Với tư cách Trợ lý AI Chi đoàn Thôn Tiền Tiến, tôi ghi nhận và đề xuất:\n" +
                "- Chi đoàn tiếp tục nắm bắt tư tưởng đoàn viên, đặc biệt tại %s với lực lượng nòng cốt.\n" +
                "- Tăng cường số hóa sổ sách, rà soát lại thông tin thẻ đoàn và chuẩn bị tốt hồ sơ sinh hoạt hè.\n" +
                "- Đồng chí có thể bấm vào các gợi ý bên dưới để tra cứu nhanh số liệu đoàn viên hoặc xây dựng kế hoạch phong trào.",
                q, "4 xóm và 2 cụm dân cư Thôn Tiền Tiến"
        );
    }

    private String generateLocalMemberReview(YouthMember member, String reviewType) {
        String name = member.getFullName();
        int days = member.getVolunteerDays();
        String pos = member.getPosition();
        String residence = member.getResidenceStatus();

        if ("year_end".equalsIgnoreCase(reviewType)) {
            return String.format(
                    "Trong năm công tác vừa qua, đồng chí %s (%s) luôn thể hiện lập trường tư tưởng vững vàng, chấp hành nghiêm túc mọi chủ trương, đường lối của Đảng và chính sách của Nhà nước. Đồng chí đã tham gia %d buổi lao động tình nguyện Ngày Chủ nhật xanh do Chi đoàn Thôn Tiền Tiến phát động. Có tinh thần trách nhiệm cao, đóng góp tích cực vào các phong trào chung của thôn, xứng đáng xếp loại Đoàn viên %s.",
                    name, pos, days, member.getClassification() != null ? member.getClassification() : "Xuất sắc"
            );
        } else if ("commendation".equalsIgnoreCase(reviewType)) {
            return String.format(
                    "Nhiệt liệt biểu dương đồng chí %s (%s, %s). Với thành tích xuất sắc tham gia %d ngày công tình nguyện vì cộng đồng, tiên phong đi đầu trong các hoạt động phong trào thanh niên tại Thôn Tiền Tiến. Đồng chí là tấm gương đoàn viên tiêu biểu, nhiệt huyết, lan tỏa tinh thần xung kích của tuổi trẻ.",
                    name, pos, member.getVillage(), days
            );
        } else {
            // Mặc định: Nhận xét sinh hoạt hè nộp về trường học / đơn vị
            if ("Sinh viên học xa".equalsIgnoreCase(residence) || "Tại thôn".equalsIgnoreCase(residence)) {
                return String.format(
                        "Trong thời gian sinh hoạt hè tại Chi đoàn Thôn Tiền Tiến, đồng chí %s luôn chấp hành tốt chủ trương của Đảng, chính sách pháp luật của Nhà nước và quy chế của địa phương. Tích cực, nhiệt tình tham gia các hoạt động do Chi đoàn và thôn phát động, đặc biệt đã đóng góp %d buổi tham gia Ngày Chủ nhật xanh và sinh hoạt thiếu nhi tại Nhà văn hóa thôn. Hoàn thành xuất sắc nhiệm vụ đoàn viên nơi cư trú.",
                        name, days
                );
            } else {
                return String.format(
                        "Đồng chí %s dù công tác, làm ăn xa quê hương nhưng luôn chấp hành tốt pháp luật của Nhà nước và quy định địa phương. Thường xuyên giữ mối liên hệ mật thiết với Chi đoàn Thôn Tiền Tiến qua nhóm Zalo chi đoàn, hoàn thành tốt nghĩa vụ công dân và trách nhiệm của người đoàn viên đối với thôn xóm.",
                        name
                );
            }
        }
    }

    public List<String> getSuggestedQuestions() {
        return Arrays.asList(
                "Thống kê tổng quan số lượng đoàn viên Thôn Tiền Tiến",
                "Tình hình đoàn viên đi làm ăn xa và sinh viên học xa",
                "Danh sách Top đoàn viên tham gia nhiều buổi Chủ nhật xanh nhất",
                "Gợi ý kế hoạch tổ chức Ngày Chủ nhật xanh tuần tới",
                "Dự thảo bài phát biểu cho Bí thư Chi đoàn thôn"
        );
    }
}
