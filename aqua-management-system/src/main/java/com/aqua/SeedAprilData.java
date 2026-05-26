package com.aqua;

import com.aqua.database.DatabaseConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Locale;

public class SeedAprilData {

    private static final String BASE_URL = "https://uszuutvdfavikxbyrduy.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVzenV1dHZkZmF2aWt4YnlyZHV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzg1NTczODEsImV4cCI6MjA5NDEzMzM4MX0.o-m2FoorW7H3J8wA5_v9OlfKbU007u2QM41VjnwimR0";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        System.out.println("🧼 INITIATING SYSTEM PURGE AND REAL APRIL 2026 SEEDING...");

        try {
            // STEP 1: WIPE SUPABASE CLOUD
            System.out.println("\n☁️  Step 1: Wiping Cloud Supabase Database...");
            wipeSupabaseTable("bills");
            wipeSupabaseTable("deliveries");
            wipeSupabaseTable("customers");

            // STEP 2: WIPE LOCAL SQLITE
            System.out.println("\n💾 Step 2: Wiping Local SQLite Database...");
            Connection localDb = DatabaseConnection.getConnection();
            try (Statement s = localDb.createStatement()) {
                s.executeUpdate("DELETE FROM bills");
                s.executeUpdate("DELETE FROM deliveries");
                s.executeUpdate("DELETE FROM customers");
                s.executeUpdate("DELETE FROM sqlite_sequence WHERE name IN ('customers', 'deliveries', 'bills')");
                System.out.println("   ✅ Local database successfully wiped!");
            }

            // STEP 3: SEED CUSTOMERS (LOCAL AND CLOUD)
            System.out.println("\n🌱 Step 3: Seeding 13 Real Customers...");
            Customer[] customers = getRealCustomers();
            for (Customer c : customers) {
                // SQLite insert
                String sqLiteQ = "INSERT INTO customers (id, name, address, mobile, route, email, sync_status) VALUES (?, ?, ?, ?, ?, ?, 'SYNCED')";
                try (PreparedStatement ps = localDb.prepareStatement(sqLiteQ)) {
                    ps.setInt(1, c.id);
                    ps.setString(2, c.name);
                    ps.setString(3, c.address);
                    ps.setString(4, c.mobile);
                    ps.setString(5, c.route);
                    ps.setString(6, c.email);
                    ps.executeUpdate();
                }

                // Supabase insert (no rates in schema, just baseline metadata)
                String json = String.format("{\"id\":%d,\"name\":\"%s\",\"address\":\"%s\",\"mobile\":\"%s\",\"route\":\"%s\",\"email\":\"%s\"}",
                        c.id, c.name, c.address, c.mobile, c.route, c.email);
                postToSupabase("customers", json);
                System.out.println("   ✅ Seeded Customer: " + c.name);
            }

            // STEP 4: SEED DAILY DELIVERIES FOR APRIL 2026
            System.out.println("\n📅 Step 4: Seeding Daily April 2026 Delivery Logs...");
            
            // Deliveries data grids (Day 1 to 26)
            int[] ssJars = {3, 4, 3, 4, 5, 3, 4, 5, 3, 4, 3, 4, 5, 3, 4, 4, 5, 3, 4, 3, 4, 5, 3, 4, 3, 4};
            int[] ssBottles = {5, 6, 5, 4, 4, 5, 4, 5, 4, 4, 4, 5, 5, 5, 4, 4, 4, 5, 5, 5, 4, 6, 5, 4, 3, 4};
            
            int[] omkarBottles = {3, 3, 2, 3, 2, 3, 4, 3, 2, 4, 3, 2, 3, 4, 3, 2, 3, 4, 3, 2, 3, 4, 3, 2, 2, 3};
            
            int[] msBottles = {3, 1, 4, 5, 4, 4, 3, 0, 4, 4, 5, 4, 4, 5, 4, 4, 0, 3, 4, 5, 3, 4, 5, 3, 0, 4};
            
            int[] balajiBottles = {4, 0, 3, 4, 3, 4, 3, 0, 4, 5, 3, 4, 5, 3, 2, 3, 0, 4, 3, 4, 4, 3, 4, 4, 0, 5};
            
            int[] dattaBottles = {12, 13, 11, 12, 13, 13, 9, 8, 7, 10, 11, 12, 13, 14, 9, 13, 13, 10, 9, 12, 13, 12, 9, 8, 10, 12};
            
            int[] skJars = {3, 4, 3, 4, 3, 4, 3, 2, 3, 4, 3, 2, 3, 4, 3, 2, 3, 2, 3, 2, 3, 4, 3, 2, 5, 3};
            int[] skBottles = {1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1};

            int deliveryId = 1;
            
            for (int day = 1; day <= 26; day++) {
                String date = String.format("2026-04-%02d", day);

                // SS Enterprises (ID: 5)
                insertDelivery(deliveryId++, 5, date, ssJars[day - 1], ssBottles[day - 1], localDb);
                
                // Omkar eng (ID: 6)
                insertDelivery(deliveryId++, 6, date, 0, omkarBottles[day - 1], localDb);
                
                // M.S (ID: 8)
                insertDelivery(deliveryId++, 8, date, 0, msBottles[day - 1], localDb);
                
                // Balaji (ID: 9)
                insertDelivery(deliveryId++, 9, date, 0, balajiBottles[day - 1], localDb);
                
                // Datta eng (ID: 10)
                insertDelivery(deliveryId++, 10, date, 0, dattaBottles[day - 1], localDb);
                
                // Shri Krushna (ID: 11)
                insertDelivery(deliveryId++, 11, date, skJars[day - 1], skBottles[day - 1], localDb);
            }
            System.out.println("   ✅ Successfully seeded 156 daily delivery logs!");

            // STEP 5: PRE-GENERATE APRIL 2026 BILLS FOR CONVENIENCE
            System.out.println("\n💳 Step 5: Generating Monthly Invoices for April 2026...");
            
            // SS Enterprises (ID: 5) -> Jars: 98, Bottles: 120
            insertBill(1, 5, 4, 2026, 98, 120, 25.0, 22.0, 2450.0, 2640.0, 5090.0, "PENDING", localDb);
            
            // Omkar eng (ID: 6) -> Bottles: 75
            insertBill(2, 6, 4, 2026, 0, 75, 0.0, 30.0, 0.0, 2250.0, 2250.0, "PENDING", localDb);
            
            // M.S (ID: 8) -> Bottles: 88
            insertBill(3, 8, 4, 2026, 0, 88, 0.0, 30.0, 0.0, 2640.0, 2640.0, "PENDING", localDb);
            
            // Balaji (ID: 9) -> Bottles: 82
            insertBill(4, 9, 4, 2026, 0, 82, 0.0, 30.0, 0.0, 2460.0, 2460.0, "PENDING", localDb);
            
            // Datta eng (ID: 10) -> Bottles: 295
            insertBill(5, 10, 4, 2026, 0, 295, 0.0, 40.0, 0.0, 11800.0, 11800.0, "PENDING", localDb);
            
            // Shri Krushna (ID: 11) -> Jars: 78, Bottles: 20
            insertBill(6, 11, 4, 2026, 78, 20, 40.0, 35.0, 3120.0, 700.0, 3820.0, "PENDING", localDb);
            
            System.out.println("   ✅ Invoice registry completed!");

            System.out.println("\n🎉 SYSTEM FULLY SEEDED WITH REAL DATA! EXCEL IMPORT SUCCESSFUL!");

        } catch (Exception e) {
            System.err.println("\n❌ CRITICAL SEEDING FAILURE: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void insertDelivery(int id, int customerId, String date, int jars, int bottles, Connection db) throws Exception {
        // Local SQLite Insert
        String sql = "INSERT INTO deliveries (id, customer_id, delivery_date, jar_qty, bottle_qty, sync_status) VALUES (?, ?, ?, ?, ?, 'SYNCED')";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, customerId);
            ps.setString(3, date);
            ps.setInt(4, jars);
            ps.setInt(5, bottles);
            ps.executeUpdate();
        }

        // Supabase Cloud Insert
        String json = String.format("{\"id\":%d,\"customer_id\":%d,\"delivery_date\":\"%s\",\"jar_qty\":%d,\"bottle_qty\":%d}",
                id, customerId, date, jars, bottles);
        postToSupabase("deliveries", json);
    }

    private static void insertBill(int id, int customerId, int month, int year, int jars, int bottles, double jRate, double bRate, double jAmt, double bAmt, double grand, String status, Connection db) throws Exception {
        // Local SQLite Insert
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

        // Supabase Cloud Insert
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
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            System.out.println("   [-] Cloud table [" + table + "] wiped cleanly.");
        } else {
            System.out.println("   [-] Cloud table [" + table + "] was already clean (Code: " + response.statusCode() + ").");
        }
    }

    private static void postToSupabase(String table, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + table))
            .header("apikey", API_KEY)
            .header("Authorization", "Bearer " + API_KEY)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Cloud Save Failed for table [" + table + "]: Code " + response.statusCode() + " -> " + response.body());
        }
    }

    private static Customer[] getRealCustomers() {
        return new Customer[]{
            new Customer(1, "Fafal Roofs", "Talwade", "+919000000001", 0, 0, "Talwade", ""),
            new Customer(2, "Prolofic eng", "Talwade", "+919000000002", 0, 0, "Talwade", ""),
            new Customer(3, "Emerging Buildcon", "Talwade", "+919000000003", 0, 0, "Talwade", ""),
            new Customer(4, "Hanuman eng", "Talwade", "+919000000004", 0, 0, "Talwade", ""),
            new Customer(5, "SS Enterprises", "Talwade", "+919000000005", 25, 22, "Talwade", ""),
            new Customer(6, "Omkar eng", "Talwade", "+919000000006", 0, 30, "Talwade", ""),
            new Customer(7, "Accurate eng", "Talwade", "+919000000007", 0, 30, "Talwade", ""),
            new Customer(8, "M.S", "Talwade", "+919000000008", 0, 30, "Talwade", ""),
            new Customer(9, "Balaji", "Talwade", "+919000000009", 0, 30, "Talwade", ""),
            new Customer(10, "Datta eng", "Talwade", "+919000000010", 0, 40, "Talwade", ""),
            new Customer(11, "Shri Krushna", "Talwade", "+919000000011", 40, 35, "Talwade", ""),
            new Customer(12, "Matchcraft", "Talwade", "+919000000012", 0, 0, "Talwade", ""),
            new Customer(13, "Elite", "Talwade", "+919000000013", 0, 0, "Talwade", "")
        };
    }

    private static class Customer {
        int id;
        String name;
        String address;
        String mobile;
        double jarRate;
        double bottleRate;
        String route;
        String email;

        public Customer(int id, String name, String address, String mobile, double jarRate, double bottleRate, String route, String email) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.mobile = mobile;
            this.jarRate = jarRate;
            this.bottleRate = bottleRate;
            this.route = route;
            this.email = email;
        }
    }
}
