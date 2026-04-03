package com.example.dhan_rsi_series.utils;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.example.dhan_rsi_series.model.CandleData;
import com.example.dhan_rsi_series.model.RsiCandle;

public class RsiExcelGenerator {

    public static byte[] generateExcel(List<RsiCandle> data) {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("RSI");

            // ================= HEADER STYLE =================
            CellStyle headerStyle = workbook.createCellStyle();

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            setBorders(headerStyle);

            // ================= DATA STYLE =================
            CellStyle dataStyle = workbook.createCellStyle();
            setBorders(dataStyle);

            // ================= HEADER =================
            Row header = sheet.createRow(0);
     
            String[] headers = {
                    "DateTime", "Open", "High", "Low",
                    "Close", "Volume", "Open Interest", "RSI",
                    "deltaRsi", "callFlow", "putFlow", "netFlow",
                    "highestOi", "atp", "DeltaLtp", "DeltaDeltaLtp", "Buy", "Sell"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ================= DATA =================
            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (RsiCandle c : data) {
                Row row = sheet.createRow(rowIdx++);

                createCell(row, 0, c.getDateTime().format(formatter), dataStyle);
                createCell(row, 1, c.getOpen(), dataStyle);
                createCell(row, 2, c.getHigh(), dataStyle);
                createCell(row, 3, c.getLow(), dataStyle);
                createCell(row, 4, c.getClose(), dataStyle);
                createCell(row, 5, c.getVolume(), dataStyle);
                createCell(row, 6, c.getOpen_interest(),
                        dataStyle);
                createCell(row, 7, c.getRsi(), dataStyle);
                createCell(row, 8, c.getDeltaRsi(), dataStyle);
                createCell(row, 9, c.getCallFlow(), dataStyle);
                createCell(row, 10, c.getPutFlow(), dataStyle);
                createCell(row, 11, c.getNetFlow(), dataStyle);
                createCell(row, 12, c.getHighestOi(), dataStyle);
                createCell(row, 13, c.getAtp(), dataStyle);
                createCell(row, 14, c.getDeltaLtp(), dataStyle);
                createCell(row, 15, c.getDeltaDeltaLtp(), dataStyle);
                createCell(row, 16, c.isBuy()?"true":"false", dataStyle);
                createCell(row, 17, c.isSell()?"true":"false", dataStyle);
            }

            // ================= AUTO SIZE =================
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }

    // ================= UTILITY METHODS =================

    private static void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void createCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void createCell(Row row, int col, long value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    
    public static RsiCandle buildRsiCandle(
            CandleData cd, double rsi, double vwap, double highRsi, double lowRsi) {

        return RsiCandle.builder()
                .rsi(rsi)
                .vwap(vwap)
                .highRsi(highRsi)
                .lowRsi(lowRsi)
                .open(cd.getOpen())
                .close(cd.getClose())
                .high(cd.getHigh())
                .low(cd.getLow())
                .volume(cd.getVolume())
                .open_interest(cd.getOpen_interest())
                .dateTime(cd.getDateTime())
                .build();
    }
    
    public static RsiCandle buildRsiCandle(
            CandleData cd, double rsi, double deltaRsi, double callFlow, double putFlow, double netFlow, double highestOi, double atp) {

        return RsiCandle.builder()
                .rsi(rsi)
                .open(cd.getOpen())
                .close(cd.getClose())
                .high(cd.getHigh())
                .low(cd.getLow())
                .volume(cd.getVolume())
                .open_interest(cd.getOpen_interest())
                .dateTime(cd.getDateTime())
                .deltaRsi(deltaRsi)
                .callFlow(callFlow)
                .putFlow(putFlow)
                .netFlow(netFlow)
                .highestOi(highestOi)
                .atp(atp)
                .build();
    }

}
