package tn.economic.system.connection;
import java.sql.Connection;
import tn.economic.system.config.DataBaseConfig;
import java.sql.DriverManager;
public class DBConnection {
    private static Connection connection;

    private DBConnection() {}

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("oracle.jdbc.driver.OracleDriver");
                connection = DriverManager.getConnection(
                        DataBaseConfig.URL,
                        DataBaseConfig.USER,
                        DataBaseConfig.PASSWORD
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Database connection error", e);
        }
        return connection;
    }
}
