package ara.project.takalo.purchase.infrastructure.importing;

import ara.project.takalo.purchase.domain.model.ImportFormat;
import ara.project.takalo.purchase.domain.model.ParsedPurchaseRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelPurchaseImportParserTest {

    private final ExcelPurchaseImportParser parser = new ExcelPurchaseImportParser();

    @Test
    void supports_onlyExcelXlsx() {
        assertThat(parser.supports(ImportFormat.EXCEL_XLSX)).isTrue();
        for (ImportFormat format : ImportFormat.values()) {
            if (format != ImportFormat.EXCEL_XLSX) {
                assertThat(parser.supports(format)).isFalse();
            }
        }
    }

    @Test
    void parse_readsRowsAndSkipsHeader() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            writeHeader(sheet);
            CellStyle dateStyle = dateStyle(wb);

            Row r1 = sheet.createRow(1);
            setDate(r1.createCell(0), LocalDate.of(2026, 4, 27), dateStyle);
            r1.createCell(1).setCellValue("Lait");
            r1.createCell(2).setCellValue(2.0);
            r1.createCell(3).setCellValue(1.50);
            r1.createCell(4).setCellValue("Boissons");

            Row r2 = sheet.createRow(2);
            setDate(r2.createCell(0), LocalDate.of(2026, 4, 28), dateStyle);
            r2.createCell(1).setCellValue("Pain");
            r2.createCell(2).setCellValue(1.0);
            r2.createCell(3).setCellValue(0.90);

            List<ParsedPurchaseRow> rows = parser.parse(toStream(wb));

            assertThat(rows).hasSize(2);
            assertThat(rows.getFirst().lineNumber()).isEqualTo(2);
            assertThat(rows.getFirst().purchaseDate()).isEqualTo(LocalDate.of(2026, 4, 27));
            assertThat(rows.getFirst().productName()).isEqualTo("Lait");
            assertThat(rows.getFirst().quantity()).isEqualTo(2.0);
            assertThat(rows.getFirst().unitPrice()).isEqualByComparingTo("1.50");
            assertThat(rows.getFirst().discount()).isEqualByComparingTo("0");
            assertThat(rows.get(0).expiryDate()).isNull();
            assertThat(rows.get(0).storeName()).isNull();
            assertThat(rows.get(0).categoryName()).isEqualTo("Boissons");
            assertThat(rows.get(1).lineNumber()).isEqualTo(3);
            assertThat(rows.get(1).productName()).isEqualTo("Pain");
            assertThat(rows.get(1).categoryName()).isNull();
        }
    }

    @Test
    void parse_skipsBlankRows() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            writeHeader(sheet);
            CellStyle dateStyle = dateStyle(wb);

            sheet.createRow(1);

            Row r2 = sheet.createRow(2);
            setDate(r2.createCell(0), LocalDate.of(2026, 1, 5), dateStyle);
            r2.createCell(1).setCellValue("Riz");
            r2.createCell(2).setCellValue(5.0);
            r2.createCell(3).setCellValue(2.0);

            List<ParsedPurchaseRow> rows = parser.parse(toStream(wb));

            assertThat(rows).hasSize(1);
            assertThat(rows.getFirst().lineNumber()).isEqualTo(3);
            assertThat(rows.getFirst().productName()).isEqualTo("Riz");
        }
    }

    @Test
    void parse_acceptsStringDateAndCommaDecimal() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            writeHeader(sheet);

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("27/04/2026");
            r1.createCell(1).setCellValue("Beurre");
            r1.createCell(2).setCellValue("1,5");
            r1.createCell(3).setCellValue("3,25");

            List<ParsedPurchaseRow> rows = parser.parse(toStream(wb));

            assertThat(rows).hasSize(1);
            assertThat(rows.getFirst().purchaseDate()).isEqualTo(LocalDate.of(2026, 4, 27));
            assertThat(rows.getFirst().quantity()).isEqualTo(1.5);
            assertThat(rows.getFirst().unitPrice()).isEqualByComparingTo("3.25");
        }
    }

    @Test
    void parse_invalidValues_returnNull() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            writeHeader(sheet);

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("not-a-date");
            r1.createCell(1).setCellValue("Sucre");
            r1.createCell(2).setCellValue("abc");
            r1.createCell(3).setCellValue("xyz");

            List<ParsedPurchaseRow> rows = parser.parse(toStream(wb));

            assertThat(rows).hasSize(1);
            assertThat(rows.getFirst().purchaseDate()).isNull();
            assertThat(rows.getFirst().productName()).isEqualTo("Sucre");
            assertThat(rows.getFirst().quantity()).isNull();
            assertThat(rows.getFirst().unitPrice()).isNull();
        }
    }

    @Test
    void parse_emptySheet_returnsEmptyList() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            wb.createSheet();
            assertThat(parser.parse(toStream(wb))).isEmpty();
        }
    }

    @Test
    void parse_invalidStream_throwsUncheckedIOException() {
        InputStream garbage = new ByteArrayInputStream("not an xlsx file".getBytes());
        assertThatThrownBy(() -> parser.parse(garbage))
                .isInstanceOfAny(UncheckedIOException.class, RuntimeException.class);
    }

    private void writeHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("purchaseDate");
        header.createCell(1).setCellValue("productName");
        header.createCell(2).setCellValue("quantity");
        header.createCell(3).setCellValue("unitPrice");
        header.createCell(4).setCellValue("categoryName");
    }

    private CellStyle dateStyle(Workbook wb) {
        CreationHelper helper = wb.getCreationHelper();
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));
        return style;
    }

    private void setDate(Cell cell, LocalDate date, CellStyle style) {
        cell.setCellValue(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        cell.setCellStyle(style);
    }

    private InputStream toStream(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}