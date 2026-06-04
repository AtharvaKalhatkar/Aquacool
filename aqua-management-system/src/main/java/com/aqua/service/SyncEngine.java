package com.aqua.service;

import com.aqua.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SyncEngine {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final java.util.concurrent.atomic.AtomicBoolean isSyncing = new java.util.concurrent.atomic.AtomicBoolean(false);
    
    // Supabase configuration constants
    private static final String BASE_URL = "https://uszuutvdfavikxbyrduy.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVzenV1dHZkZmF2aWt4YnlyZHV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzg1NTczODEsImV4cCI6MjA5NDEzMzM4MX0.o-m2FoorW7H3J8wA5_v9OlfKbU007u2QM41VjnwimR0";
    
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void startAutoSync() {
        System.out.println("⚡ Initializing Cloud Sync Engine (HTTP-REST Protocol v2.0)...");
        scheduler.scheduleAtFixedRate(() -> {
            try {
                runSync();
            } catch (Exception e) {
                System.err.println("❌ Sync Error: " + e.getMessage());
            }
        }, 1, 60, TimeUnit.SECONDS);
    }

    public static void stopAutoSync() {
        scheduler.shutdownNow();
    }

    public static void runSync() {
        if (!isSyncing.compareAndSet(false, true)) {
            System.out.println("Sync already in progress, skipping request.");
            return;
        }
        
        try {
            System.out.println("🔄 Syncing with Cloud via Secure HTTPS...");
            Connection localDb = DatabaseConnection.getConnection();
 
            // STEP 1: PULL REMOTE -> LOCAL
            pullTable("customers", localDb);
            pullTable("deliveries", localDb);
            pullTable("bills", localDb);
 
            // STEP 2: PUSH LOCAL -> REMOTE
            pushCustomers(localDb);
            pushDeliveries(localDb);
            pushBills(localDb);
 
            System.out.println("✅ Cloud Sync Successful!");
 
        } catch (Exception e) {
            System.err.println("🚨 SYNC FAILED: " + e.getMessage());
        } finally {
            isSyncing.set(false);
        }
    }

    // ------------------------------------------------------------------------
    // 📥 PULL LOGIC (GET from Cloud -> Write Local)
    // ------------------------------------------------------------------------
    private static void pullTable(String table, Connection db) throws Exception {
        boolean autoCommit = db.getAutoCommit();
        db.setAutoCommit(false);
        
        int offset = 0;
        int limit = 1000;
        int totalIntegrated = 0;
        java.util.List<Integer> activeIds = new java.util.ArrayList<>();
        
        try {
            while (true) {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + table + "?select=*&limit=" + limit + "&offset=" + offset))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    System.err.println("Failed to fetch " + table + ": Code " + response.statusCode());
                    break;
                }

                String body = response.body().trim();
                if (body.equals("[]") || body.equals("[  ]") || body.length() <= 4) {
                    break;
                }

                String[] items = body.split("\\}\\s*,\\s*\\{");
                if (items.length == 0) {
                    break;
                }

                for (String item : items) {
                    String clean = item.replace("[{", "").replace("}]", "").replace("{", "").replace("}", "");
                    if (clean.trim().isEmpty()) continue;

                    int parsedId = extractInt(clean, "\"id\":");
                    if (parsedId > 0) {
                        activeIds.add(parsedId);
                    }

                    if (table.equals("customers")) {
                        int id = parsedId;
                        String name = extractStr(clean, "\"name\":\"");
                        
                        if (id <= 0 || name == null || name.trim().isEmpty()) {
                            continue;
                        }
                        
                        String q = "INSERT INTO customers (id, name, address, mobile, route, email, sync_status) VALUES (?, ?, ?, ?, ?, ?, 'SYNCED') " +
                                   "ON CONFLICT(id) DO UPDATE SET name=excluded.name, address=excluded.address, mobile=excluded.mobile, route=excluded.route, email=excluded.email, sync_status='SYNCED'";
                        try (PreparedStatement ps = db.prepareStatement(q)) {
                            ps.setInt(1, id);
                            ps.setString(2, name);
                            ps.setString(3, extractStr(clean, "\"address\":\""));
                            ps.setString(4, extractStr(clean, "\"mobile\":\""));
                            ps.setString(5, extractStr(clean, "\"route\":\""));
                            ps.setString(6, extractStr(clean, "\"email\":\""));
                            ps.executeUpdate();
                            totalIntegrated++;
                        }
                    } 
                    else if (table.equals("deliveries")) {
                        int id = parsedId;
                        if (id <= 0) continue;
                        String q = "INSERT INTO deliveries (id, customer_id, delivery_date, jar_qty, bottle_qty, sync_status) VALUES (?, ?, ?, ?, ?, 'SYNCED') " +
                                   "ON CONFLICT(id) DO UPDATE SET customer_id=excluded.customer_id, delivery_date=excluded.delivery_date, jar_qty=excluded.jar_qty, bottle_qty=excluded.bottle_qty, sync_status='SYNCED'";
                        try (PreparedStatement ps = db.prepareStatement(q)) {
                            ps.setInt(1, id);
                            ps.setInt(2, extractInt(clean, "\"customer_id\":"));
                            ps.setString(3, extractStr(clean, "\"delivery_date\":\""));
                            ps.setInt(4, extractInt(clean, "\"jar_qty\":"));
                            ps.setInt(5, extractInt(clean, "\"bottle_qty\":"));
                            ps.executeUpdate();
                            totalIntegrated++;
                        }
                    }
                    else if (table.equals("bills")) {
                        int id = parsedId;
                        if (id <= 0) continue;
                        String q = "INSERT OR REPLACE INTO bills (id, customer_id, bill_month, bill_year, total_jars, total_bottles, jar_rate, bottle_rate, jar_amount, bottle_amount, grand_total, status, sync_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCED')";
                        try (PreparedStatement ps = db.prepareStatement(q)) {
                            ps.setInt(1, id);
                            ps.setInt(2, extractInt(clean, "\"customer_id\":"));
                            ps.setInt(3, extractInt(clean, "\"bill_month\":"));
                            ps.setInt(4, extractInt(clean, "\"bill_year\":"));
                            ps.setInt(5, extractInt(clean, "\"total_jars\":"));
                            ps.setInt(6, extractInt(clean, "\"total_bottles\":"));
                            ps.setDouble(7, extractDouble(clean, "\"jar_rate\":"));
                            ps.setDouble(8, extractDouble(clean, "\"bottle_rate\":"));
                            ps.setDouble(9, extractDouble(clean, "\"jar_amount\":"));
                            ps.setDouble(10, extractDouble(clean, "\"bottle_amount\":"));
                            ps.setDouble(11, extractDouble(clean, "\"grand_total\":"));
                            ps.setString(12, extractStr(clean, "\"status\":\""));
                            ps.executeUpdate();
                            totalIntegrated++;
                        }
                    }
                }
                
                if (items.length < limit) {
                    break;
                }
                offset += limit;
            }

            // Prune deleted records locally
            if (!activeIds.isEmpty()) {
                StringBuilder deleteQuery = new StringBuilder("DELETE FROM " + table + " WHERE sync_status != 'PENDING' AND id NOT IN (");
                for (int i = 0; i < activeIds.size(); i++) {
                    if (i > 0) deleteQuery.append(",");
                    deleteQuery.append(activeIds.get(i));
                }
                deleteQuery.append(")");
                try (Statement stmt = db.createStatement()) {
                    stmt.executeUpdate(deleteQuery.toString());
                }
            } else if (offset == 0) {
                // Cloud is completely empty for this table
                try (Statement stmt = db.createStatement()) {
                    stmt.executeUpdate("DELETE FROM " + table + " WHERE sync_status != 'PENDING'");
                }
            }

            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw e;
        } finally {
            db.setAutoCommit(autoCommit);
        }
        System.out.println(">> PULL [" + table + "] COMPLETED! Successfully integrated " + totalIntegrated + " entries.");
    }

    // ------------------------------------------------------------------------
    // 📤 PUSH LOGIC (POST to Cloud)
    // ------------------------------------------------------------------------
    private static void pushCustomers(Connection db) throws Exception {
        String q = "SELECT * FROM customers WHERE sync_status = 'PENDING'";
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder("[");
        try (Statement s = db.createStatement(); ResultSet rs = s.executeQuery(q)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                ids.add(id);
                if (sb.length() > 1) sb.append(",");
                sb.append(String.format("{\"id\":%d,\"name\":\"%s\",\"address\":\"%s\",\"mobile\":\"%s\",\"route\":\"%s\",\"email\":\"%s\"}",
                    id, 
                    escapeJson(rs.getString("name")), 
                    escapeJson(rs.getString("address")), 
                    escapeJson(rs.getString("mobile")), 
                    escapeJson(rs.getString("route")), 
                    escapeJson(rs.getString("email"))));
            }
        }
        sb.append("]");
        if (!ids.isEmpty()) {
            if (upsertToCloud("customers", sb.toString())) {
                try (Statement stmt = db.createStatement()) {
                    for (int id : ids) {
                        stmt.addBatch("UPDATE customers SET sync_status = 'SYNCED' WHERE id = " + id);
                    }
                    stmt.executeBatch();
                }
            }
        }
    }

    private static void pushDeliveries(Connection db) throws Exception {
        String q = "SELECT * FROM deliveries WHERE sync_status = 'PENDING'";
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder("[");
        try (Statement s = db.createStatement(); ResultSet rs = s.executeQuery(q)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                ids.add(id);
                if (sb.length() > 1) sb.append(",");
                sb.append(String.format("{\"id\":%d,\"customer_id\":%d,\"delivery_date\":\"%s\",\"jar_qty\":%d,\"bottle_qty\":%d}",
                    id, rs.getInt("customer_id"), escapeJson(rs.getString("delivery_date")), rs.getInt("jar_qty"), rs.getInt("bottle_qty")));
            }
        }
        sb.append("]");
        if (!ids.isEmpty()) {
            if (upsertToCloud("deliveries", sb.toString())) {
                try (Statement stmt = db.createStatement()) {
                    for (int id : ids) {
                        stmt.addBatch("UPDATE deliveries SET sync_status = 'SYNCED' WHERE id = " + id);
                    }
                    stmt.executeBatch();
                }
            }
        }
    }

    private static void pushBills(Connection db) throws Exception {
        String q = "SELECT * FROM bills WHERE sync_status = 'PENDING'";
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder("[");
        try (Statement s = db.createStatement(); ResultSet rs = s.executeQuery(q)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                ids.add(id);
                if (sb.length() > 1) sb.append(",");
                sb.append(String.format(java.util.Locale.US, "{\"id\":%d,\"customer_id\":%d,\"bill_month\":%d,\"bill_year\":%d,\"total_jars\":%d,\"total_bottles\":%d,\"jar_rate\":%.2f,\"bottle_rate\":%.2f,\"jar_amount\":%.2f,\"bottle_amount\":%.2f,\"grand_total\":%.2f,\"status\":\"%s\"}",
                    id, rs.getInt("customer_id"), rs.getInt("bill_month"), rs.getInt("bill_year"), rs.getInt("total_jars"), rs.getInt("total_bottles"), rs.getDouble("jar_rate"), rs.getDouble("bottle_rate"), rs.getDouble("jar_amount"), rs.getDouble("bottle_amount"), rs.getDouble("grand_total"), escapeJson(rs.getString("status"))));
            }
        }
        sb.append("]");
        if (!ids.isEmpty()) {
            if (upsertToCloud("bills", sb.toString())) {
                try (Statement stmt = db.createStatement()) {
                    for (int id : ids) {
                        stmt.addBatch("UPDATE bills SET sync_status = 'SYNCED' WHERE id = " + id);
                    }
                    stmt.executeBatch();
                }
            }
        }
    }

    private static String escapeJson(String val) {
        if (val == null) return "";
        return val.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private static boolean upsertToCloud(String table, String jsonPayload) throws Exception {
        String url = BASE_URL + table + "?on_conflict=id";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("apikey", API_KEY)
            .header("Authorization", "Bearer " + API_KEY)
            .header("Content-Type", "application/json")
            .header("Prefer", "resolution=merge-duplicates") 
            .timeout(java.time.Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    // ------------------------------------------------------------------------
    // 🔍 UTILITY PARSERS
    // ------------------------------------------------------------------------
    private static String extractStr(String raw, String key) {
        int idx = raw.indexOf(key);
        if (idx == -1) return "";
        int start = idx + key.length();
        int end = raw.indexOf("\"", start);
        if (end == -1) return "";
        return raw.substring(start, end);
    }

    private static int extractInt(String raw, String key) {
        int idx = raw.indexOf(key);
        if (idx == -1) return 0;
        int start = idx + key.length();
        int end = findDelimiter(raw, start);
        try {
            return Integer.parseInt(raw.substring(start, end).trim().replace(":", "").replace("\"", ""));
        } catch (Exception e) { return 0; }
    }

    private static double extractDouble(String raw, String key) {
        int idx = raw.indexOf(key);
        if (idx == -1) return 0.0;
        int start = idx + key.length();
        int end = findDelimiter(raw, start);
        try {
            return Double.parseDouble(raw.substring(start, end).trim().replace(":", "").replace("\"", ""));
        } catch (Exception e) { return 0.0; }
    }

    private static int findDelimiter(String raw, int start) {
        int c = raw.indexOf(",", start);
        int q = raw.indexOf("\"", start);
        if (c == -1 && q == -1) return raw.length();
        if (c == -1) return q;
        if (q == -1) return c;
        return Math.min(c, q);
    }
}
