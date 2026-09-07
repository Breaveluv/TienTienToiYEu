package com.example.tientienyouthmanagement.service;

import com.example.tientienyouthmanagement.model.YouthMember;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class DocumentParserService {

    public List<YouthMember> parseFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) filename = "";
        filename = filename.toLowerCase();

        try (InputStream is = file.getInputStream()) {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                return parseExcel(is);
            } else if (filename.endsWith(".docx")) {
                return parseWord(is);
            } else if (filename.endsWith(".pdf")) {
                return parsePdf(is);
            } else if (filename.endsWith(".csv") || filename.endsWith(".txt")) {
                return parseCsv(is);
            } else {
                try {
                    return parseExcel(is);
                } catch (Exception e) {
                    return parseCsv(file.getInputStream());
                }
            }
        }
    }

    public List<YouthMember> parseExcel(InputStream is) throws Exception {
        List<YouthMember> list = new ArrayList<>();
        Workbook workbook = WorkbookFactory.create(is);
        Sheet sheet = workbook.getSheetAt(0);

        boolean isHeader = true;
        for (Row row : sheet) {
            if (row == null) continue;

            String firstCol = getCellValue(row.getCell(0));
            if (firstCol.isBlank()) continue;

            if (isHeader && (firstCol.equalsIgnoreCase("họ và tên") || firstCol.equalsIgnoreCase("họ tên") || firstCol.equalsIgnoreCase("stt") || firstCol.equalsIgnoreCase("name"))) {
                isHeader = false;
                continue;
            }
            isHeader = false;

            String name = getCellValue(row.getCell(0));
            if (name.isBlank() || name.equalsIgnoreCase("stt") || name.equalsIgnoreCase("họ và tên")) continue;

            String birthYear = getCellValue(row.getCell(1));
            String village = getCellValue(row.getCell(2));
            String position = getCellValue(row.getCell(3));
            String phone = getCellValue(row.getCell(4));
            String residence = getCellValue(row.getCell(5));
            String card = getCellValue(row.getCell(6));

            list.add(buildMember(name, birthYear, village, position, phone, residence, card));
        }
        workbook.close();
        return list;
    }

    public List<YouthMember> parseWord(InputStream is) throws Exception {
        List<YouthMember> list = new ArrayList<>();
        XWPFDocument doc = new XWPFDocument(is);

        for (XWPFTable table : doc.getTables()) {
            boolean isHeader = true;
            for (XWPFTableRow row : table.getRows()) {
                List<XWPFTableCell> cells = row.getTableCells();
                if (cells.isEmpty()) continue;

                String cell0 = cells.get(0).getText().trim();
                if (isHeader && (cell0.equalsIgnoreCase("stt") || cell0.equalsIgnoreCase("họ và tên") || cell0.equalsIgnoreCase("họ tên"))) {
                    isHeader = false;
                    continue;
                }
                isHeader = false;

                String name = cell0;
                if (name.isBlank()) continue;

                String birthYear = cells.size() > 1 ? cells.get(1).getText().trim() : "2003";
                String village = cells.size() > 2 ? cells.get(2).getText().trim() : "Xóm 1 (Thôn Tiền Tiến)";
                String position = cells.size() > 3 ? cells.get(3).getText().trim() : "Đoàn viên";
                String phone = cells.size() > 4 ? cells.get(4).getText().trim() : "0988.xxx.xxx";
                String residence = cells.size() > 5 ? cells.get(5).getText().trim() : "Tại thôn";
                String card = cells.size() > 6 ? cells.get(6).getText().trim() : "";

                list.add(buildMember(name, birthYear, village, position, phone, residence, card));
            }
        }

        if (list.isEmpty()) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                String text = p.getText().trim();
                if (text.isBlank() || text.length() < 3) continue;

                if (text.contains(",") || text.contains("-") || text.contains("\t")) {
                    String[] parts = text.split("[,\\-\\t]");
                    if (parts.length >= 2) {
                        String name = parts[0].replaceAll("^\\d+[\\.\\)]\\s*", "").trim();
                        String village = parts.length > 1 ? parts[1].trim() : "Xóm 1 (Thôn Tiền Tiến)";
                        String phone = parts.length > 2 ? parts[2].trim() : "0988.xxx.xxx";
                        list.add(buildMember(name, "2002", village, "Đoàn viên", phone, "Tại thôn", ""));
                    }
                }
            }
        }

        doc.close();
        return list;
    }

    public List<YouthMember> parsePdf(InputStream is) throws Exception {
        List<YouthMember> list = new ArrayList<>();
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            String[] lines = text.split("\\r?\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isBlank()) continue;

                if (line.matches("^(\\d+[\\.\\)]\\s+)?[\\p{L}\\s]{2,40}\\s*[,\\-\\t|]\\s*.*")) {
                    String clean = line.replaceAll("^\\d+[\\.\\)]\\s*", "").trim();
                    String[] parts = clean.split("[,\\-\\t|]");
                    if (parts.length >= 2) {
                        String name = parts[0].trim();
                        String village = parts.length > 1 ? parts[1].trim() : "Xóm 1 (Thôn Tiền Tiến)";
                        String phone = parts.length > 2 ? parts[2].trim() : "0988.xxx.xxx";
                        String pos = parts.length > 3 ? parts[3].trim() : "Đoàn viên";
                        list.add(buildMember(name, "2003", village, pos, phone, "Tại thôn", ""));
                    }
                }
            }
        }
        return list;
    }

    public List<YouthMember> parseCsv(InputStream is) throws Exception {
        List<YouthMember> list = new ArrayList<>();
        Scanner scanner = new Scanner(is, StandardCharsets.UTF_8);
        boolean isHeader = true;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isBlank()) continue;

            String[] parts = line.split("[,;\\t]");
            if (parts.length < 2) continue;

            if (isHeader && (parts[0].toLowerCase().contains("họ") || parts[0].toLowerCase().contains("name"))) {
                isHeader = false;
                continue;
            }
            isHeader = false;

            String name = parts[0].trim();
            String birthYear = parts.length > 1 ? parts[1].trim() : "2003";
            String village = parts.length > 2 ? parts[2].trim() : "Xóm 1 (Thôn Tiền Tiến)";
            String pos = parts.length > 3 ? parts[3].trim() : "Đoàn viên";
            String phone = parts.length > 4 ? parts[4].trim() : "0988.xxx.xxx";
            String res = parts.length > 5 ? parts[5].trim() : "Tại thôn";
            String card = parts.length > 6 ? parts[6].trim() : "";

            list.add(buildMember(name, birthYear, village, pos, phone, res, card));
        }
        return list;
    }

    public byte[] generateExcelTemplate() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("DS_Doan_Vien_Thon_Tien_Tien");

        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row header = sheet.createRow(0);
        String[] columns = {"Họ và Tên Đoàn Viên", "Năm Sinh", "Xóm / Cụm Dân Cư (Thôn Tiền Tiến)", "Chức Vụ", "Số Điện Thoại", "Tình Trạng Sinh Hoạt", "Số Thẻ Đoàn"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 6500);
        }

        Object[][] sampleData = {
                {"Lê Văn Hùng", "2003", "Xóm 1 (Thôn Tiền Tiến)", "Đoàn viên", "0982.111.222", "Tại thôn", "TD-882190"},
                {"Trần Thị Thảo", "2004", "Xóm 2 (Thôn Tiền Tiến)", "Phó Bí thư Chi đoàn", "0973.333.444", "Tại thôn", "TD-771234"},
                {"Nguyễn Văn Cường", "2001", "Xóm 3 (Thôn Tiền Tiến)", "Đoàn viên", "0912.555.666", "Đi làm ăn xa", "TD-991283"},
                {"Phạm Thị Lan", "2005", "Xóm 4 (Thôn Tiền Tiến)", "Đoàn viên", "0965.777.888", "Sinh viên học xa", "TD-445612"}
        };

        for (int i = 0; i < sampleData.length; i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < sampleData[i].length; j++) {
                row.createCell(j).setCellValue(sampleData[i][j].toString());
            }
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return bos.toByteArray();
    }

    private YouthMember buildMember(String name, String birth, String village, String pos, String phone, String res, String card) {
        if (village == null || village.isBlank()) village = "Xóm 1 (Thôn Tiền Tiến)";
        if (pos == null || pos.isBlank()) pos = "Đoàn viên";
        if (phone == null || phone.isBlank()) phone = "0988.xxx.xxx";
        if (res == null || res.isBlank()) res = "Tại thôn";
        if (birth == null || birth.isBlank()) birth = "2003";
        if (card == null || card.isBlank()) card = "TD-" + (int)(100000 + Math.random() * 900000);

        return YouthMember.builder()
                .id("DV-TT" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .fullName(name.trim())
                .birthYear(birth.trim())
                .gender("Nam")
                .village(village.trim())
                .position(pos.trim())
                .phone(phone.trim())
                .unionCardNumber(card.trim())
                .joinDate("26/03/2020")
                .residenceStatus(res.trim())
                .classification("Khá")
                .avatar("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&h=200&fit=crop&crop=faces")
                .activityNotes("Đoàn viên Chi đoàn Thôn Tiền Tiến.")
                .volunteerDays(2)
                .build();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
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
}
