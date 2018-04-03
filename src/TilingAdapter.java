import javax.xml.transform.Result;
import java.sql.*;
import java.io.*;
import java.util.*;

public class TilingAdapter {

    static final String DB_URL = "jdbc:postgresql://localhost:5432/";
    static final String USER = "postgres";
    static final String PASS = "pass";

    public static void main(String []args) throws IOException {
        Connection conn = null;
        Statement stmt = null;

        String DBNAME = "egg";
        boolean hasPostgis = false;

        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL + "postgres", USER, PASS);

            System.out.println("Creating statement...");
            stmt = conn.createStatement();

            stmt.executeUpdate(" DROP DATABASE IF EXISTS " + DBNAME + "; \n CREATE DATABASE "+ DBNAME + " WITH OWNER=postgres;" );
            stmt.close();
            conn.close();


            conn = DriverManager.getConnection(DB_URL + DBNAME, USER, PASS);
            stmt = conn.createStatement();
            stmt.executeUpdate("CREATE EXTENSION postgis;");

            /*ResultSet rs = stmt.executeQuery("SELECT ext.name FROM pg_available_extensions ext;");
            while(rs.next()) {
                String name = rs.getString("name");
                if(name == "postgis"){
                    hasPostgis = true;
                    rs.close();
                }
            }
            if(!hasPostgis)
            {
                return;
            }*/
            stmt.close();
            conn.close();
            String path = "";
            String createdbsql = "D:\\Program Files\\3DCityDB-Importer-Exporter\\3dcitydb\\postgresql\\CREATE_DB.sql";
            String command = "\"D:\\Program Files\\PostgreSQL\\10\\bin\\psql\" -U postgres -w -d " + DBNAME + " -f \"" + createdbsql + "\"";
            System.out.print("Running: ");
            System.out.print(command);
            System.out.println();
            Process p = Runtime.getRuntime().exec(command);


        } catch (ClassNotFoundException e) {

            System.err.println(e);
        } catch (SQLException ex) {
            System.err.println("SQLException: " + ex.getMessage());
            System.err.println("SQLState: " + ex.getSQLState());
            System.err.println("VendorError: " + ex.getErrorCode());
        }


    }


}
