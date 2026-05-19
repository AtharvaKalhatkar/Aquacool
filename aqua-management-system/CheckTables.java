import java.sql.*;
import java.io.File;

public class CheckTables {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String path = System.getProperty("user.home") + File.separator + ".aqua_management" + File.separator + "aqua_management.db";
        String url = "jdbc:sqlite:" + path;
        
        System.out.println("--- DB CHECK ---");
        try (Connection c = DriverManager.getConnection(url);
             Statement st = c.createStatement()) {
             
            System.out.println("\n[CUSTOMERS]");
            try (ResultSet rs = st.executeQuery("SELECT * FROM customers")) {
                while(rs.next()) System.out.println("ID=" + rs.getInt("id") + " Name=" + rs.getString("name") + " Status=" + rs.getString("sync_status"));
            }
            
            System.out.println("\n[DELIVERIES]");
            try (ResultSet rs = st.executeQuery("SELECT * FROM deliveries")) {
                while(rs.next()) System.out.println("ID=" + rs.getInt("id") + " CustID=" + rs.getInt("customer_id") + " Date=" + rs.getString("delivery_date") + " Jars=" + rs.getInt("jar_qty") + " Status=" + rs.getString("sync_status"));
            }
            
            System.out.println("\n[BILLS]");
            try (ResultSet rs = st.executeQuery("SELECT * FROM bills")) {
                while(rs.next()) System.out.println("ID=" + rs.getInt("id") + " CustID=" + rs.getInt("customer_id") + " Total=" + rs.getDouble("grand_total") + " Status=" + rs.getString("sync_status"));
            }
        }
    }
}
