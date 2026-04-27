package ara.project.takalo.purchase.infrastructure.importing;

import ara.project.takalo.purchase.application.port.out.PurchaseImportParser;
import ara.project.takalo.purchase.domain.model.ImportFormat;
import ara.project.takalo.purchase.domain.model.ParsedPurchaseRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelPurchaseImportParser implements PurchaseImportParser {

    private static final int COL_PURCHASE_DATE = 0;
    private static final int COL_PRODUCT_NAME = 1;
    private static final int COL_QUANTITY = 2;
    private static final int COL_UNIT_PRICE = 3;
    // private static final int COL_DISCOUNT = 4;
//    private static final int COL_EXPIRY_DATE = 5;
//    private static final int COL_STORE_NAME = 6;

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    };

    @Override
    public boolean supports(ImportFormat format) {
        return ImportFormat.EXCEL_XLSX.equals(format);
    }

    @Override
    public List<ParsedPurchaseRow> parse(InputStream source) {
        try (Workbook workbook = new XSSFWorkbook(source)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<ParsedPurchaseRow> rows = new ArrayList<>();
            int firstDataRow = sheet.getFirstRowNum() + 1;
            int lastRow = sheet.getLastRowNum();
            for (int i = firstDataRow; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmpty(row)) {
                    continue;
                }
                rows.add(toParsedRow(row));
            }
            return rows;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ParsedPurchaseRow toParsedRow(Row row) {
        return new ParsedPurchaseRow(
                row.getRowNum() + 1,
                readDate(row.getCell(COL_PURCHASE_DATE)),
                readString(row.getCell(COL_PRODUCT_NAME)),
                readDouble(row.getCell(COL_QUANTITY)),
                readBigDecimal(row.getCell(COL_UNIT_PRICE)),
                BigDecimal.ZERO,
                null,
                null
        );
    }

    private boolean isEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = readString(cell);
                if (value != null && !value.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String readString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : trimTrailingZero(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getStringCellValue();
            default -> null;
        };
    }

    private Double readDouble(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                String raw = cell.getStringCellValue().trim().replace(',', '.');
                if (raw.isEmpty()) yield null;
                try { yield Double.parseDouble(raw); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }

    private BigDecimal readBigDecimal(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> {
                String raw = cell.getStringCellValue().trim().replace(',', '.');
                if (raw.isEmpty()) yield null;
                try { yield new BigDecimal(raw); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }

    private LocalDate readDate(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (cell.getCellType() == CellType.STRING) {
            String raw = cell.getStringCellValue().trim();
            if (raw.isEmpty()) return null;
            for (DateTimeFormatter fmt : DATE_FORMATS) {
                try { return LocalDate.parse(raw, fmt); }
                catch (DateTimeParseException ignored) { }
            }
        }
        return null;
    }

    private String trimTrailingZero(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}