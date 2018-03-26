import javax.xml.transform.Result;
import java.sql.*;
import java.util.*;
import java.io.*;


class CollisionEntry{

    String date;
    String time;
    Double longitude;
    Double latitude;
    String on_street; // datatype 1 strval
    String cross_street; // 1 strval
    String injuries; // 2 intval
    String deaths; // 2 intval
    String cfveh1; // 1 strval
    String cfveh2; // 1 strval
    String cfveh3; // 1 strval
    String cfveh4; // 1 strval
    String cfveh5; // 1 strval
    String uniquekey; // 1 strval
    String vehtype1; // 1 strval
    String vehtype2; // 1 strval
    String vehtype3; // 1 strval
    String vehtype4; // 1 strval
    String vehtype5; // 1 strval
    String id; // (cityobject_id)

    public String toString(){
        return this.date + " " + this.time + " (" + String.valueOf(this.longitude)+ ", " + String.valueOf(latitude)+ ") " + this.on_street + " " + this.cross_street + " " + this.injuries + " " + this.deaths;
    }
}

public class JDBCHandler {

    static final String DB_URL = "jdbc:postgresql://localhost:5432/traffic-collisions";

    static final String USER = "postgres";
    static final String PASS = "pass";

    public static void main(String []args) {
        System.out.println("Hello World"); // prints Hello World

        Connection conn = null;
        Statement stmt = null;



        try {

            ArrayList<CollisionEntry> collisionEntryArrayList = GetDataFromCSV("C:\\Users\\gunea\\Documents\\NYCLOD1\\collisions-roads.csv");

            for (int i = 0; i < collisionEntryArrayList.size(); i++)
            {
                System.out.println(collisionEntryArrayList.get(i).toString());
            }

            Class.forName("org.postgresql.Driver");

            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            System.out.println("Creating statement...");
            stmt = conn.createStatement();
            String sql;
            sql = "SELECT cityobject.id, citydb.objectclass.classname,cityobject.gmlid, ST_AsText(ST_Transform(cityobject.envelope, 4326)) envelope FROM citydb.cityobject\n" +
                    "INNER JOIN citydb.objectclass ON objectclass.id = cityobject.objectclass_id\n" +
                    "LIMIT 10;";

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
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static ArrayList<CollisionEntry> GetDataFromCSV(String path) throws IOException {
        ArrayList<CollisionEntry> collisionEntryArrayList = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(new File(path)));
        String line;
        while ((line = br.readLine()) != null) {

            String[] entries = line.split(",");

            CollisionEntry ce = new CollisionEntry();

            ce.date = entries[0];
            ce.time = entries[1];
            ce.on_street = entries[7];
            ce.cross_street = entries[8];
            ce.injuries = entries[10];

            ce.deaths = entries[11];
            ce.cfveh1 = entries[18];
            ce.cfveh2 = entries[19];
            ce.cfveh3 = entries[20];
            ce.cfveh4 = entries[21];
            ce.cfveh5 = entries[22];
            ce.uniquekey = entries[23];
            ce.vehtype1 = entries[24];
            ce.vehtype2 = entries[25];
            ce.vehtype3 = entries[26];
            ce.vehtype4 = entries[27];
            ce.vehtype5 = entries[28];
            ce.id = entries[30];

            collisionEntryArrayList.add(ce);
        }

        return collisionEntryArrayList;
    }
}
