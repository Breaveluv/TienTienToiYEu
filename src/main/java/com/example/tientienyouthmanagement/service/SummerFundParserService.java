package com.example.tientienyouthmanagement.service;

import com.example.tientienyouthmanagement.model.SummerFundDonation;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SummerFundParserService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public List<SummerFundDonation> parseFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) filename = "";
        filename = filename.toLowerCase();

        try (InputStream is = file.getInputStream()) {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                return parseExcel(is);
            } else {
                return parseCsv(is);
            }
        }
    }

    public List<SummerFundDonation> fetchAndParseGoogleSheet(String sheetUrl) throws Exception {
        if (sheetUrl == null || sheetUrl.isBlank()) {
            throw new IllegalArgumentException("Đường dẫn Google Sheet không được để trống!");
        }

        String exportUrl = convertToCsvExportUrl(sheetUrl.trim());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(exportUrl))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() == 200) {
            try (InputStream is = response.body()) {
                return parseCsv(is);
            }
        } else if (response.statusCode() == 404 || response.statusCode() == 403 || response.statusCode() == 302) {
            throw new IllegalStateException("Không thể đọc Google Sheet (Mã HTTP " + response.statusCode() + "). Vui lòng đảm bảo bảng tính đã được BẬT quyền chia sẻ: 'Bất kỳ ai có liên kết đều có thể xem'!");
        } else {
            throw new IllegalStateException("Lỗi tải Google Sheet từ máy chủ Google (Mã: " + response.statusCode() + ")");
        }
    }

    public List<SummerFundDonation> parseExcel(InputStream is) throws Exception {
        List<List<String>> rawRows = new ArrayList<>();
        Workbook workbook = WorkbookFactory.create(is);
        Sheet sheet = workbook.getSheetAt(0);

        for (Row row : sheet) {
            if (row == null) continue;
            List<String> rowCells = new ArrayList<>();
            int lastCellNum = row.getLastCellNum();
            for (int i = 0; i < lastCellNum; i++) {
                rowCells.add(getCellValue(row.getCell(i)));
            }
            rawRows.add(rowCells);
        }
        workbook.close();

        return parseTableData(rawRows);
    }

    public List<SummerFundDonation> parseCsv(InputStream is) throws Exception {
        List<List<String>> rawRows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                List<String> tokens = parseCsvLine(line);
                rawRows.add(tokens);
            }
        }
        return parseTableData(rawRows);
    }

    private List<SummerFundDonation> parseTableData(List<List<String>> rows) {
        List<SummerFundDonation> result = new ArrayList<>();
        if (rows == null || rows.isEmpty()) return result;

        int headerRowIndex = -1;
        int nameCol = -1;
        int amountCol = -1;
        int villageCol = -1;
        int itemCol = -1;
        int noteCol = -1;
        int purposeCol = -1;

        // BƯỚC 1: Tìm dòng tiêu đề (Header Row)
        for (int i = 0; i < Math.min(rows.size(), 10); i++) {
            List<String> row = rows.get(i);
            int matchedKeywords = 0;

            for (int c = 0; c < row.size(); c++) {
                String val = row.get(c).toLowerCase().trim();
                if (val.contains("họ và tên") || val.contains("họ tên") || (val.contains("tên") && !val.contains("số tiền")) || val.contains("người ủng hộ")) {
                    matchedKeywords++;
                } else if (val.contains("tiền") || val.contains("mức ủng hộ") || val.contains("kinh phí") || val.contains("sotien")) {
                    matchedKeywords++;
                } else if (val.contains("đội") || val.contains("xóm") || val.contains("thôn") || val.contains("địa chỉ") || val.contains("địa bàn")) {
                    matchedKeywords++;
                }
            }

            if (matchedKeywords >= 2) {
                headerRowIndex = i;
                List<String> headerRow = rows.get(i);
                for (int c = 0; c < headerRow.size(); c++) {
                    String colName = headerRow.get(c).toLowerCase().trim();
                    if (nameCol == -1 && (colName.contains("họ và tên") || colName.contains("họ tên") || colName.contains("người ủng hộ") || (colName.contains("tên") && !colName.contains("tiền")))) {
                        nameCol = c;
                    } else if (amountCol == -1 && (colName.contains("tiền") || colName.contains("mức") || colName.contains("kinh phí") || colName.contains("sotien"))) {
                        amountCol = c;
                    } else if (villageCol == -1 && (colName.contains("đội") || colName.contains("xóm") || colName.contains("thôn") || colName.contains("địa chỉ") || colName.contains("địa bàn") || colName.contains("khu"))) {
                        villageCol = c;
                    } else if (itemCol == -1 && (colName.contains("hiện vật") || colName.contains("vật phẩm") || colName.contains("quà"))) {
                        itemCol = c;
                    } else if (purposeCol == -1 && (colName.contains("mục đích") || colName.contains("nội dung"))) {
                        purposeCol = c;
                    } else if (noteCol == -1 && (colName.contains("ghi chú") || colName.contains("lời nhắn") || colName.contains("lời chúc") || colName.contains("chú thích"))) {
                        noteCol = c;
                    }
                }
                break;
            }
        }

        // BƯỚC 2: Duyệt các dòng dữ liệu
        int startRow = headerRowIndex >= 0 ? headerRowIndex + 1 : 0;

        for (int i = startRow; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.isEmpty()) continue;

            String donorName = "";
            String village = "Tại thôn";
            long amount = 0L;
            String item = null;
            String purpose = "Ủng hộ chung Hoạt động hè";
            String note = null;

            if (headerRowIndex >= 0 && nameCol >= 0) {
                // Cách 1: Dùng mapping theo header
                if (nameCol < row.size()) donorName = row.get(nameCol).trim();
                if (amountCol >= 0 && amountCol < row.size()) amount = parseAmount(row.get(amountCol));
                if (villageCol >= 0 && villageCol < row.size()) {
                    String v = row.get(villageCol).trim();
                    if (!v.isBlank()) village = v;
                }
                if (itemCol >= 0 && itemCol < row.size()) item = emptyToNull(row.get(itemCol));
                if (purposeCol >= 0 && purposeCol < row.size()) {
                    String p = row.get(purposeCol).trim();
                    if (!p.isBlank()) purpose = p;
                }
                if (noteCol >= 0 && noteCol < row.size()) note = emptyToNull(row.get(noteCol));
            }

            // Cách 2: Heuristic thông minh nếu cách 1 không lấy được tên hoặc không có header
            if (donorName.isBlank() || isPureNumber(donorName)) {
                SmartRowDetected detected = detectRowSmart(row);
                if (detected != null) {
                    donorName = detected.donorName;
                    if (!detected.village.isBlank()) village = detected.village;
                    if (detected.amount > 0) amount = detected.amount;
                    if (detected.item != null) item = detected.item;
                    if (detected.note != null) note = detected.note;
                }
            }

            // BỘ LỌC BẢO VỆ: Loại bỏ hoàn toàn các dòng rác, dòng tiêu đề phụ, dòng tổng cộng
            if (donorName.isBlank()) continue;
            if (isPureNumber(donorName)) continue; // Ví dụ "129", "130", "131" là số thứ tự dòng trống
            if (isIgnoredKeyword(donorName)) continue; // "Tổng cộng", "Cộng", "STT"...

            // Nếu số tiền bằng 0 và không có hiện vật và tên chỉ có 1-2 ký tự số -> bỏ qua
            if (amount == 0 && item == null && donorName.length() < 3) continue;

            result.add(SummerFundDonation.builder()
                    .donorName(donorName)
                    .donorTitle("Bà con nhân dân")
                    .villageOrUnit(village)
                    .amount(amount)
                    .itemDonation(item)
                    .donationPurpose(purpose)
                    .paymentMethod(amount > 0 ? "Tiền mặt" : "Hiện vật")
                    .donationDate(LocalDate.now())
                    .note(note)
                    .build());
        }

        return result;
    }

    private static class SmartRowDetected {
        String donorName = "";
        String village = "";
        long amount = 0L;
        String item = null;
        String note = null;
    }

    private SmartRowDetected detectRowSmart(List<String> row) {
        // Thu thập các ô có dữ liệu không rỗng
        List<IndexedCell> cells = new ArrayList<>();
        for (int i = 0; i < row.size(); i++) {
            String val = row.get(i).trim();
            if (!val.isEmpty()) {
                cells.add(new IndexedCell(i, val));
            }
        }

        if (cells.isEmpty()) return null;

        // Nếu dòng chỉ có 1 ô và là số (ví dụ chỉ có "129") -> dòng số thứ tự trống -> Bỏ qua
        if (cells.size() == 1 && isPureNumber(cells.get(0).value)) {
            return null;
        }

        SmartRowDetected res = new SmartRowDetected();
        IndexedCell nameCell = null;
        IndexedCell amountCell = null;
        IndexedCell villageCell = null;

        // 1. Tìm ô chứa số tiền (chứa số >= 1000 hoặc có ký tự tiền tệ như đ, ₫, k)
        for (IndexedCell c : cells) {
            long amt = parseAmount(c.value);
            if (amt >= 1000L || c.value.toLowerCase().contains("000") || c.value.toLowerCase().endsWith("đ") || c.value.toLowerCase().endsWith("k")) {
                if (amt > 0) {
                    amountCell = c;
                    res.amount = amt;
                    break;
                }
            }
        }

        // 2. Tìm ô chứa địa bàn / xóm / đội (chứa chữ "Đội", "Xóm", "Thôn", "Khu")
        for (IndexedCell c : cells) {
            if (c == amountCell) continue;
            String lower = c.value.toLowerCase();
            if (lower.contains("đội") || lower.contains("xóm") || lower.contains("thôn") || lower.contains("cụm") || lower.contains("khu")) {
                villageCell = c;
                res.village = c.value;
                break;
            }
        }

        // 3. Tìm ô chứa Họ và Tên: ô dạng chữ, không phải thuần số, không phải ô tiền, không phải ô xóm
        for (IndexedCell c : cells) {
            if (c == amountCell || c == villageCell) continue;
            if (isPureNumber(c.value)) continue; // Bỏ qua cột STT
            if (isIgnoredKeyword(c.value)) continue;

            nameCell = c;
            res.donorName = c.value;
            break;
        }

        // 4. Các ô còn lại làm ghi chú hoặc hiện vật
        for (IndexedCell c : cells) {
            if (c != amountCell && c != villageCell && c != nameCell) {
                if (!isPureNumber(c.value)) {
                    if (c.value.toLowerCase().contains("thùng") || c.value.toLowerCase().contains("quả") || c.value.toLowerCase().contains("bộ")) {
                        res.item = c.value;
                    } else if (res.note == null) {
                        res.note = c.value;
                    }
                }
            }
        }

        if (res.donorName.isBlank()) return null;
        return res;
    }

    private static class IndexedCell {
        int col;
        String value;
        IndexedCell(int col, String value) {
            this.col = col;
            this.value = value;
        }
    }

    private boolean isIgnoredKeyword(String str) {
        String lower = str.toLowerCase().trim();
        return lower.equals("stt") || lower.equals("số tt") || lower.equals("tổng cộng") ||
               lower.equals("tổng") || lower.equals("cộng") || lower.startsWith("tổng số") ||
               lower.equals("họ và tên") || lower.equals("người ủng hộ");
    }

    private boolean isPureNumber(String str) {
        if (str == null || str.isBlank()) return false;
        String clean = str.trim().replaceAll("[,.]", "");
        return clean.matches("^\\d+$");
    }

    private long parseAmount(String str) {
        if (str == null || str.isBlank()) return 0L;
        try {
            String clean = str.trim().toLowerCase();
            // Nếu có chữ "k" (ví dụ 200k = 200.000)
            if (clean.endsWith("k") || clean.endsWith(" k")) {
                clean = clean.replaceAll("[^0-9]", "");
                if (!clean.isEmpty()) {
                    return Long.parseLong(clean) * 1000L;
                }
            }
            // Làm sạch ký tự tiền tệ: ₫, đ, vnđ, dấu phẩy, dấu chấm
            clean = clean.replaceAll("[^0-9]", "");
            if (clean.isEmpty()) return 0L;
            return Long.parseLong(clean);
        } catch (Exception e) {
            return 0L;
        }
    }

    private String emptyToNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }

    public byte[] generateExcelTemplate() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("DS_Ung_Ho_He_Thon_Tien_Tien");

        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.CORAL.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row header = sheet.createRow(0);
        String[] columns = {
                "Họ và Tên Người / Đơn Vị Ủng Hộ",
                "Danh Xưng / Vai Trò",
                "Xóm / Địa Bàn (Thôn Tiền Tiến)",
                "Số Tiền Ủng Hộ (VNĐ)",
                "Hiện Vật Ủng Hộ (nếu có)",
                "Mục Đích Ủng Hộ",
                "Hình Thức",
                "Lời Nhắn Gửi Thiếu Nhi"
        };

        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 6500);
        }

        Object[][] sampleData = {
                {"Bác Nguyễn Văn Thắng", "Trưởng Ban Mặt trận Thôn", "Xóm 2", 1500000, "1 thùng bánh kẹo", "Trại hè Thiếu nhi", "Tiền mặt", "Chúc các cháu mùa hè vui tươi, chăm ngoan!"},
                {"Công ty TNHH Cơ Khí Tiền Tiến", "Doanh nghiệp địa phương", "Doanh nghiệp / Mạnh thường quân", 3000000, "Cúp & cờ lưu niệm", "Giải bóng đá Thanh thiếu nhi", "Chuyển khoản VietQR", "Đồng hành cùng giải bóng đá hè thanh thiếu niên."},
                {"Anh Lê Hoàng Nam", "Cựu Bí thư Chi đoàn (Hà Nội)", "Con em làm ăn xa quê", 2000000, "", "Đêm hội Văn nghệ hè", "Chuyển khoản VietQR", "Tiếp sức thanh thiếu nhi quê nhà."},
                {"Gia đình Chị Phạm Thị Hoa", "Bà con nhân dân", "Xóm 1", 500000, "2 thùng sữa tươi", "Ủng hộ chung Hoạt động hè", "Tiền mặt", "Ủng hộ bồi dưỡng cho các cháu tập văn nghệ."}
        };

        for (int i = 0; i < sampleData.length; i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < sampleData[i].length; j++) {
                Cell cell = row.createCell(j);
                Object val = sampleData[i][j];
                if (Number.class.isAssignableFrom(val.getClass())) {
                    cell.setCellValue(((Number) val).doubleValue());
                } else {
                    cell.setCellValue(val.toString());
                }
            }
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return bos.toByteArray();
    }

    private String convertToCsvExportUrl(String url) {
        Pattern pattern = Pattern.compile("/spreadsheets/d/([a-zA-Z0-9-_]+)");
        Matcher matcher = pattern.matcher(url);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Định dạng link Google Sheet không hợp lệ! Vui lòng sao chép toàn bộ đường link từ trình duyệt.");
        }

        String spreadsheetId = matcher.group(1);
        String gid = "0";
        Pattern gidPattern = Pattern.compile("[#&?]gid=([0-9]+)");
        Matcher gidMatcher = gidPattern.matcher(url);
        if (gidMatcher.find()) {
            gid = gidMatcher.group(1);
        }

        return "https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/export?format=csv&gid=" + gid;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }

        switch (type) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                long val = (long) cell.getNumericCellValue();
                return String.valueOf(val);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> list = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if ((c == ',' || c == '\t') && !inQuotes) {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString());
        return list;
    }
}
