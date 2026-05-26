package com.aqua;

import com.aqua.database.DatabaseConnection;
import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Locale;

public class ImportExcelData {

    private static final String BASE_URL = "https://uszuutvdfavikxbyrduy.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVzenV1dHZkZmF2aWt4YnlyZHV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzg1NTczODEsImV4cCI6MjA5NDEzMzM4MX0.o-m2FoorW7H3J8wA5_v9OlfKbU007u2QM41VjnwimR0";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        System.out.println("🧼 INITIATING SYSTEM PURGE AND REAL EXCEL IMPORT...");

        try {
            System.out.println("\n☁️  Step 1: Wiping Cloud Supabase Database...");
            wipeSupabaseTable("bills");
            wipeSupabaseTable("deliveries");
            wipeSupabaseTable("customers");

            System.out.println("\n💾 Step 2: Wiping Local SQLite Database...");
            Connection localDb = DatabaseConnection.getConnection();
            try (Statement s = localDb.createStatement()) {
                s.executeUpdate("DELETE FROM bills");
                s.executeUpdate("DELETE FROM deliveries");
                s.executeUpdate("DELETE FROM customers");
                s.executeUpdate("DELETE FROM sqlite_sequence WHERE name IN ('customers', 'deliveries', 'bills')");
                System.out.println("   ✅ Local database successfully wiped!");
            }

            System.out.println("\n📊 Step 3: Parsing Excel and Injecting Data...");
            Workbook wb = WorkbookFactory.create(new File("Bill April 2.0 with mobile no final.xlsx"));
            Sheet sheet = wb.getSheetAt(0);

            int customerId = 1;
            int deliveryId = 1;
            int billId = 1;
            int totalDeliveries = 0;

            for (int r = 2; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String companyName = getStr(row, 0);
                if (companyName.trim().isEmpty()) continue; // Not a main row

                // Determine rates and dual-row existence
                double rate1 = getNum(row, 1);
                double jarRate = 0;
                double bottleRate = 0;
                Row row2 = null;

                if (r + 1 <= sheet.getLastRowNum()) {
                    Row nextRow = sheet.getRow(r + 1);
                    if (nextRow != null && getStr(nextRow, 0).trim().isEmpty() && hasData(nextRow)) {
                        row2 = nextRow;
                    }
                }

                if (row2 != null) {
                    jarRate = rate1;
                    bottleRate = getNum(row2, 1);
                } else {
                    jarRate = 0;
                    bottleRate = rate1;
                }

                // Extract Mobile Number
                String mobile = extractMobile(row);

                // Insert Customer
                String sqLiteQ = "INSERT INTO customers (id, name, address, mobile, route, email, sync_status) VALUES (?, ?, ?, ?, ?, ?, 'SYNCED')";
                try (PreparedStatement ps = localDb.prepareStatement(sqLiteQ)) {
                    ps.setInt(1, customerId);
                    ps.setString(2, companyName);
                    ps.setString(3, "Talwade"); // Default
                    ps.setString(4, mobile);
                    ps.setString(5, "Talwade"); // Default
                    ps.setString(6, "");
                    ps.executeUpdate();
                }

                String cJson = String.format("{\"id\":%d,\"name\":\"%s\",\"address\":\"%s\",\"mobile\":\"%s\",\"route\":\"%s\",\"email\":\"%s\"}",
                        customerId, escape(companyName), "Talwade", mobile, "Talwade", "");
                postToSupabase("customers", cJson);

                int totalJars = 0;
                int totalBottles = 0;

                // Loop over 30 days of April (Columns 2 to 31)
                for (int day = 1; day <= 30; day++) {
                    int colIndex = day + 1;
                    int jarsToday = 0;
                    int bottlesToday = 0;

                    if (row2 != null) {
                        jarsToday = (int) getNum(row, colIndex);
                        bottlesToday = (int) getNum(row2, colIndex);
                    } else {
                        bottlesToday = (int) getNum(row, colIndex);
                    }

                    if (jarsToday > 0 || bottlesToday > 0) {
                        String date = String.format("2026-04-%02d", day);
                        insertDelivery(deliveryId++, customerId, date, jarsToday, bottlesToday, localDb);
                        totalJars += jarsToday;
                        totalBottles += bottlesToday;
                        totalDeliveries++;
                    }
                }

                if (totalJars > 0 || totalBottles > 0) {
                    double jarAmount = totalJars * jarRate;
                    double bottleAmount = totalBottles * bottleRate;
                    double grandTotal = jarAmount + bottleAmount;

                    insertBill(billId++, customerId, 4, 2026, totalJars, totalBottles, jarRate, bottleRate, jarAmount, bottleAmount, grandTotal, "PENDING", localDb);
                }

                System.out.println("   ✅ Imported: " + companyName + " (Jars: " + totalJars + ", Bottles: " + totalBottles + ", Mobile: " + mobile + ")");

                customerId++;
                if (row2 != null) r++; // Skip the secondary row
            }

            wb.close();
            System.out.println("\n🎉 SYSTEM FULLY SEEDED! Total Customers: " + (customerId - 1) + " | Total Deliveries: " + totalDeliveries);

        } catch (Exception e) {
            System.err.println("\n❌ CRITICAL SEEDING FAILURE: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getStr(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null) return "";
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue();
        if (c.getCellType() == CellType.NUMERIC) return String.valueOf((long) c.getNumericCellValue());
        return "";
    }

    private static double getNum(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null) return 0;
        if (c.getCellType() == CellType.NUMERIC) return c.getNumericCellValue();
        if (c.getCellType() == CellType.STRING) {
            try {
                String val = c.getStringCellValue().trim();
                if (val.equalsIgnoreCase("NA")) return 0;
                return Double.parseDouble(val);
            } catch (Exception e) { return 0; }
        }
        return 0;
    }

    private static boolean hasData(Row row) {
        for(int i=1; i<=33; i++) {
            if(getNum(row, i) > 0) return true;
        }
        return false;
    }

    private static String extractMobile(Row row) {
        for (int i = 37; i >= 35; i--) {
            Cell c = row.getCell(i);
            if (c == null) continue;
            if (c.getCellType() == CellType.NUMERIC) {
                return String.format("%.0f", c.getNumericCellValue());
            } else if (c.getCellType() == CellType.STRING) {
                String val = c.getStringCellValue().trim();
                if (val.matches(".*\\d.*")) {
                    return val.split("/")[0].replaceAll("[^0-9]", "");
                }
            }
        }
        return "";
    }

    private static String escape(String input) {
        return input.replace("\"", "\\\"").replace("\n", " ");
    }

    private static void insertDelivery(int id, int customerId, String date, int jars, int bottles, Connection db) throws Exception {
        String sql = "INSERT INTO deliveries (id, customer_id, delivery_date, jar_qty, bottle_qty, sync_status) VALUES (?, ?, ?, ?, ?, 'SYNCED')";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, customerId);
            ps.setString(3, date);
            ps.setInt(4, jars);
            ps.setInt(5, bottles);
            ps.executeUpdate();
        }
        String json = String.format("{\"id\":%d,\"customer_id\":%d,\"delivery_date\":\"%s\",\"jar_qty\":%d,\"bottle_qty\":%d}",
                id, customerId, date, jars, bottles);
        postToSupabase("deliveries", json);
    }

    private static void insertBill(int id, int customerId, int month, int year, int jars, int bottles, double jRate, double bRate, double jAmt, double bAmt, double grand, String status, Connection db) throws Exception {
        String sql = "INSERT INTO bills (id, customer_id, bill_month, bill_year, total_jars, total_bottles, jar_rate, bottle_rate, jar_amount, bottle_amount, grand_total, status, sync_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCED')";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, customerId);
            ps.setInt(3, month);
            ps.setInt(4, year);
            ps.setInt(5, jars);
            ps.setInt(6, bottles);
            ps.setDouble(7, jRate);
            ps.setDouble(8, bRate);
            ps.setDouble(9, jAmt);
            ps.setDouble(10, bAmt);
            ps.setDouble(11, grand);
            ps.setString(12, status);
            ps.executeUpdate();
        }
        String json = String.format(Locale.US, "{\"id\":%d,\"customer_id\":%d,\"bill_month\":%d,\"bill_year\":%d,\"total_jars\":%d,\"total_bottles\":%d,\"jar_rate\":%.2f,\"bottle_rate\":%.2f,\"jar_amount\":%.2f,\"bottle_amount\":%.2f,\"grand_total\":%.2f,\"status\":\"%s\"}",
                id, customerId, month, year, jars, bottles, jRate, bRate, jAmt, bAmt, grand, status);
        postToSupabase("bills", json);
    }

    private static void wipeSupabaseTable(String table) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + table + "?id=gt.0"))
            .header("apikey", API_KEY)
            .header("Authorization", "Bearer " + API_KEY)
            .DELETE()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void postToSupabase(String table, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + table))
            .header("apikey", API_KEY)
            .header("Authorization", "Bearer " + API_KEY)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
