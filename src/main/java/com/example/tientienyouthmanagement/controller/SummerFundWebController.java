package com.example.tientienyouthmanagement.controller;

import com.example.tientienyouthmanagement.model.SummerFundDonation;
import com.example.tientienyouthmanagement.service.SummerFundParserService;
import com.example.tientienyouthmanagement.service.SummerFundService;
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
@RequestMapping("/summer-fund")
public class SummerFundWebController {

    private final SummerFundService summerFundService;
    private final SummerFundParserService summerFundParserService;

    public SummerFundWebController(SummerFundService summerFundService, SummerFundParserService summerFundParserService) {
        this.summerFundService = summerFundService;
        this.summerFundParserService = summerFundParserService;
    }

    @GetMapping
    public String viewSummerFund(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) String village,
            Model model) {

        List<SummerFundDonation> donations = summerFundService.search(search, purpose, village);
        Map<String, Object> stats = summerFundService.getFundStatistics();

        long filteredTotalAmount = donations.stream()
                .mapToLong(d -> d.getAmount() != null ? d.getAmount() : 0L)
                .sum();
        long filteredItemCount = donations.stream()
                .filter(d -> d.getItemDonation() != null && !d.getItemDonation().isBlank())
                .count();

        model.addAttribute("donations", donations);
        model.addAttribute("stats", stats);
        model.addAttribute("purposes", summerFundService.getAllPurposes());
        model.addAttribute("villages", summerFundService.getAllVillages());
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("selectedPurpose", purpose != null ? purpose : "all");
        model.addAttribute("selectedVillage", village != null ? village : "all");
        model.addAttribute("totalFilteredCount", donations.size());
        model.addAttribute("filteredTotalAmount", filteredTotalAmount);
        model.addAttribute("filteredItemCount", filteredItemCount);
        model.addAttribute("newDonation", new SummerFundDonation());
        model.addAttribute("activeTab", "summerFund");

        return "summer-fund";
    }

    @PostMapping("/new")
    public String createDonation(@ModelAttribute SummerFundDonation donation, RedirectAttributes redirectAttributes) {
        SummerFundDonation saved = summerFundService.save(donation);
        String formattedAmount = String.format("%,d ₫", saved.getAmount() != null ? saved.getAmount() : 0);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã ghi nhận đóng góp từ \"" + saved.getDonorName() + "\" số tiền " + formattedAmount +
                (saved.getItemDonation() != null && !saved.getItemDonation().isBlank() ? " cùng hiện vật: " + saved.getItemDonation() : "") +
                " vào Quỹ Sinh Hoạt Hè Thôn Tiền Tiến!");
        return "redirect:/summer-fund";
    }

    @PostMapping("/{id}/delete")
    public String deleteDonation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        summerFundService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa khoản đóng góp khỏi danh sách quỹ.");
        return "redirect:/summer-fund";
    }

    @PostMapping("/clear-all")
    public String clearAllDonations(RedirectAttributes redirectAttributes) {
        summerFundService.deleteAllDonations();
        redirectAttributes.addFlashAttribute("successMessage", "Đã làm sạch toàn bộ danh sách để bạn sẵn sàng nhập lại dữ liệu chuẩn xác!");
        return "redirect:/summer-fund";
    }

    @PostMapping("/import-file")
    public String handleImportFile(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn một tệp tin Excel (.xlsx, .xls) hoặc CSV để tải lên!");
            return "redirect:/summer-fund";
        }

        try {
            List<SummerFundDonation> list = summerFundParserService.parseFile(file);
            if (list.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy dữ liệu đóng góp hợp lệ trong tệp tin vừa chọn!");
                return "redirect:/summer-fund";
            }

            int count = summerFundService.importDonations(list);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã đọc và nhập thành công " + count + " khoản ủng hộ từ tệp Excel \"" + file.getOriginalFilename() + "\" vào Quỹ Sinh Hoạt Hè!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xử lý file Excel: " + e.getMessage());
        }
        return "redirect:/summer-fund";
    }

    @PostMapping("/import-sheet")
    public String handleImportGoogleSheet(@RequestParam("sheetUrl") String sheetUrl, RedirectAttributes redirectAttributes) {
        if (sheetUrl == null || sheetUrl.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng dán đường link Google Sheet hợp lệ!");
            return "redirect:/summer-fund";
        }

        try {
            List<SummerFundDonation> list = summerFundParserService.fetchAndParseGoogleSheet(sheetUrl.trim());
            if (list.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy dữ liệu trong bảng Google Sheet vừa nhập!");
                return "redirect:/summer-fund";
            }

            int count = summerFundService.importDonations(list);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã đồng bộ trực tiếp thành công " + count + " khoản ủng hộ từ Google Sheet vào Quỹ Sinh Hoạt Hè Thôn Tiền Tiến!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/summer-fund";
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] excelBytes = summerFundParserService.generateExcelTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Mau_Danh_Sach_Ung_Ho_He_Thon_Tien_Tien.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
