import javax.xml.transform.Result;
import java.sql.*;

public class JDBCHandler {

    static final String DB_URL = "jdbc:postgresql://localhost:5432/delft";

    static final String USER = "postgres";
    static final String PASS = "pass";

    public static void main(String []args) {
        System.out.println("Hello World"); // prints Hello World

        Connection conn = null;
        Statement stmt = null;

        try {

            Class.forName("org.postgresql.Driver");

            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            System.out.println("Creating statement...");
            stmt = conn.createStatement();
            String sql;
            sql = "SELECT cityobject.id, citydb.objectclass.classname,cityobject.gmlid, cityobject.envelope FROM citydb.cityobject\n" +
                    "INNER JOIN citydb.objectclass ON objectclass.id = cityobject.objectclass_id\n" +
                    "WHERE objectclass_id = 26 LIMIT 10;";

            ResultSet rs = stmt.executeQuery(sql);

            while(rs.next()) {
                int id = rs.getInt("id");
                String classname = rs.getString("classname");
                String gmlid = rs.getString("gmlid");
                String envelope = rs.getString("envelope");

                System.out.print("ID: " + id + ", ");
                System.out.print("Class: " + classname + ", ");
                System.out.print("GML ID: " + gmlid + ", ");
                System.out.println("Envelope: " + envelope);
            }
            rs.close();
            stmt.close();
            conn.close();

        }
        catch (ClassNotFoundException e){

            System.err.println(e);
        }
        catch (SQLException ex){
            System.err.println("SQLException: " + ex.getMessage());
            System.err.println("SQLState: " + ex.getSQLState());
            System.err.println("VendorError: " + ex.getErrorCode());
        }


    }
}
