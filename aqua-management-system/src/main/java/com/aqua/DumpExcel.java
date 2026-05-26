package com.aqua;

import org.apache.poi.ss.usermodel.*;
import java.io.File;

public class DumpExcel {
    public static void main(String[] args) throws Exception {
        System.out.println("Reading Excel file...");
        Workbook wb = WorkbookFactory.create(new File("Bill April 2.0 with mobile no final.xlsx"));
        Sheet sheet = wb.getSheetAt(0);
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            System.out.print("Row " + r + ": ");
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                switch(cell.getCellType()) {
                    case STRING: System.out.print("[" + cell.getStringCellValue().replace("\n", " ") + "] "); break;
                    case NUMERIC: System.out.print("[" + cell.getNumericCellValue() + "] "); break;
                    case BOOLEAN: System.out.print("[" + cell.getBooleanCellValue() + "] "); break;
                    case FORMULA: 
                        try { System.out.print("[F:" + cell.getNumericCellValue() + "] "); }
                        catch(Exception e) { System.out.print("[F:" + cell.getStringCellValue() + "] "); }
                        break;
                    default: System.out.print("[] "); break;
                }
            }
            System.out.println();
        }
        wb.close();
    }
}
