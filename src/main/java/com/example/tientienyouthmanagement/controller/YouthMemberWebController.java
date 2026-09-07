package com.example.tientienyouthmanagement.controller;

import com.example.tientienyouthmanagement.model.YouthMember;
import com.example.tientienyouthmanagement.service.DocumentParserService;
import com.example.tientienyouthmanagement.service.YouthMemberService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class YouthMemberWebController {

    private final YouthMemberService youthMemberService;
    private final DocumentParserService documentParserService;

    public YouthMemberWebController(YouthMemberService youthMemberService, DocumentParserService documentParserService) {
        this.youthMemberService = youthMemberService;
        this.documentParserService = documentParserService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        Map<String, Object> stats = youthMemberService.getStatistics();
        List<YouthMember> topVolunteers = youthMemberService.getTopVolunteers();

        model.addAttribute("stats", stats);
        model.addAttribute("topVolunteers", topVolunteers);
        model.addAttribute("activeTab", "dashboard");
        return "index";
    }

    @GetMapping("/members")
    public String members(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String village,
            @RequestParam(required = false) String residenceStatus,
            Model model) {

        List<YouthMember> members = youthMemberService.search(search, village, residenceStatus);

        model.addAttribute("members", members);
        model.addAttribute("villages", youthMemberService.getAllVillages());
        model.addAttribute("residences", youthMemberService.getAllResidenceStatuses());
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("selectedVillage", village != null ? village : "all");
        model.addAttribute("selectedResidence", residenceStatus != null ? residenceStatus : "all");
        model.addAttribute("totalCount", members.size());
        model.addAttribute("activeTab", "members");
        return "members";
    }

    @GetMapping("/members/new")
    public String newMemberForm(Model model) {
        YouthMember member = new YouthMember();
        member.setVillage("Đội 5 (Thôn Tiền Tiến)");
        member.setPosition("Đoàn viên");
        member.setResidenceStatus("Tại thôn");
        member.setClassification("Khá");
        member.setGender("Nam");

        model.addAttribute("member", member);
        model.addAttribute("isEdit", false);
        model.addAttribute("villages", youthMemberService.getAllVillages());
        model.addAttribute("residences", youthMemberService.getAllResidenceStatuses());
        model.addAttribute("activeTab", "members");
        return "member-form";
    }

    @PostMapping("/members/new")
    public String createMember(@ModelAttribute YouthMember member, RedirectAttributes redirectAttributes) {
        youthMemberService.save(member);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã tiếp nhận đoàn viên \"" + member.getFullName() + "\" vào Sổ Chi Đoàn Thôn Tiền Tiến thành công!");
        return "redirect:/members";
    }

    @GetMapping("/members/{id}/edit")
    public String editMemberForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        YouthMember member = youthMemberService.getById(id);
        if (member == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đoàn viên có mã " + id);
            return "redirect:/members";
        }

        model.addAttribute("member", member);
        model.addAttribute("isEdit", true);
        model.addAttribute("villages", youthMemberService.getAllVillages());
        model.addAttribute("residences", youthMemberService.getAllResidenceStatuses());
        model.addAttribute("activeTab", "members");
        return "member-form";
    }

    @PostMapping("/members/{id}/edit")
    public String updateMember(@PathVariable String id, @ModelAttribute YouthMember member, RedirectAttributes redirectAttributes) {
        YouthMember existing = youthMemberService.getById(id);
        if (existing != null) {
            member.setId(id);
            if (member.getAvatar() == null || member.getAvatar().isBlank()) {
                member.setAvatar(existing.getAvatar());
            }
            if (member.getUnionCardNumber() == null || member.getUnionCardNumber().isBlank()) {
                member.setUnionCardNumber(existing.getUnionCardNumber());
            }
            youthMemberService.save(member);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật thông tin đoàn viên \"" + member.getFullName() + "\" thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đoàn viên để cập nhật!");
        }
        return "redirect:/members";
    }

    @GetMapping("/members/{id}/delete")
    public String confirmDeletePage(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        YouthMember member = youthMemberService.getById(id);
        if (member == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đoàn viên có mã " + id);
            return "redirect:/members";
        }
        model.addAttribute("member", member);
        model.addAttribute("activeTab", "members");
        return "delete-confirm";
    }

    @PostMapping("/members/{id}/delete")
    public String deleteMember(@PathVariable String id, RedirectAttributes redirectAttributes) {
        YouthMember member = youthMemberService.getById(id);
        String name = member != null ? member.getFullName() : id;
        youthMemberService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã xóa đoàn viên \"" + name + "\" khỏi Sổ Chi Đoàn Thôn Tiền Tiến.");
        return "redirect:/members";
    }

    @GetMapping("/members/import")
    public String importPage(Model model) {
        model.addAttribute("activeTab", "import");
        return "import";
    }

    @PostMapping("/members/import")
    public String handleImport(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn một tệp tin (Excel, Word hoặc PDF) để tải lên!");
            return "redirect:/members/import";
        }

        try {
            List<YouthMember> parsedList = documentParserService.parseFile(file);
            if (parsedList.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy dữ liệu đoàn viên hợp lệ trong tệp tin vừa tải lên!");
                return "redirect:/members/import";
            }

            int count = youthMemberService.importMembers(parsedList);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã đọc và nhập thành công " + count + " đoàn viên từ tệp \"" + file.getOriginalFilename() + "\" vào Sổ Chi Đoàn Thôn Tiền Tiến!");
            return "redirect:/members";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xử lý tệp: " + e.getMessage());
            return "redirect:/members/import";
        }
    }

    @GetMapping("/members/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] excelBytes = documentParserService.generateExcelTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Mau_Danh_Sach_Chi_Doan_Thon_Tien_Tien.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/members/{id}/certificate")
    public String viewCertificate(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        YouthMember member = youthMemberService.getById(id);
        if (member == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin đoàn viên để in giấy nhận xét!");
            return "redirect:/members";
        }

        model.addAttribute("member", member);
        model.addAttribute("activeTab", "members");
        return "certificate";
    }

    @GetMapping("/ai/copilot")
    public String aiCopilot(Model model) {
        model.addAttribute("stats", youthMemberService.getStatistics());
        model.addAttribute("activeTab", "copilot");
        return "copilot";
    }
}
