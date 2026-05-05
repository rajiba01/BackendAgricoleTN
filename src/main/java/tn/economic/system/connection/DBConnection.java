
package tn.economic.system.connection;

import java.sql.Connection;
import tn.economic.system.config.DataBaseConfig;
import java.sql.DriverManager;

public class DBConnection {
    private static Connection connection;

    private DBConnection() {}

    private static String envOrDefault(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("oracle.jdbc.driver.OracleDriver");

                String url = envOrDefault("DB_URL", DataBaseConfig.URL);
                String user = envOrDefault("DB_USER", DataBaseConfig.USER);
                String password = envOrDefault("DB_PASSWORD", DataBaseConfig.PASSWORD);

                connection = DriverManager.getConnection(url, user, password);
            }
        } catch (Exception e) {
            throw new RuntimeException("Database connection error", e);
        }
        return connection;
    }
}