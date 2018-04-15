import com.sun.deploy.util.ArrayUtil;
import org.h2.util.StringUtils;

import javax.xml.transform.Result;
import java.sql.*;
import java.io.*;
import java.util.*;
import org.apache.commons.lang3.ArrayUtils;

public class DBCreator {

    static final String DB_URL = "jdbc:postgresql://localhost:5432/";
    static final String USER = "postgres";
    static final String PASS = "pass";

    public static void main(String []args) throws IOException {
        if(args.length < 2){
            System.out.println("Error: please call in the format");
            System.out.println("java -jar /citygml-tiling-app.jar \"path/to/gml\" \"path/to/createdb.bat\"");
            return;
        }



    }

    private static String[] getExtents(Statement stmt, Connection conn, String dbname, String batpath, String[] errors) {
        String DBNAME = dbname;

        try {
            //Class.forName("org.postgresql.Driver");

            conn = DriverManager.getConnection(DB_URL + DBNAME, USER, PASS);
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("Select ST_Extent(ST_Transform(envelope, 4326)) from citydb.cityobject where envelope is not null");
            boolean done = false;
            String postgis = "";
            while(rs.next())
            {
                postgis = rs.getString("e");
                System.out.println("Checking postgis version " + postgis);
            }
            stmt.close();
            conn.close();
            System.out.println("Initializing database...");
            Thread.sleep(3000);

            while(!done){
                if(!StringUtils.isNullOrEmpty(postgis)){
                    done = true;
                    applyCityDBSchema(batpath, dbname);
                }
            }




        } catch (SQLException ex) {
            System.err.println("SQLException: " + ex.getMessage());
            System.err.println("SQLState: " + ex.getSQLState());
            System.err.println("VendorError: " + ex.getErrorCode());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return checkDB(stmt, conn, dbname, errors);
    }

    private static void applyCityDBSchema(String batpath, String dbname)
    {
        System.out.println("Attepmpting to apply CityDB schema to " + dbname );
        if(batpath.substring(batpath.length() - 1).equals("\"")){
            batpath = batpath.substring(0, batpath.length() - 1);
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "START", "CREATE_DB.bat");
            File dir = new File(batpath);
            pb.directory(dir);
            Process p = null;
            p = pb.start();
            Thread.sleep(3000);
        } catch (IOException e) {
            System.out.println("Could not create CityDB");
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Applying CityDB schema...");

    }
    private static void updateBatFile(String path, String update) throws IOException
    {
        //regex: CITYDB=(.*)$
        File originalFile = new File(path + "\\CREATE_DB.bat");
        BufferedReader br = new BufferedReader(new FileReader(originalFile));

        // Construct the new file that will later be renamed to the original
        // filename.
        File tempFile = new File(path + "\\CREATE_DB-TEMP.bat");
        PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

        String line = null;
        // Read from the original file and write to the new
        // unless content matches data to be removed.
        while ((line = br.readLine()) != null) {

            if (line.contains("set CITYDB")) {

                String strCurrentDB = line.substring(line.lastIndexOf("="), line.length());
                line = line.substring(0,line.lastIndexOf("=")) + "="+ update;
                System.out.println("Current line: " + strCurrentDB + "; Replaced with: " + update);
            }

            pw.println(line);
            pw.flush();
        }
        pw.close();
        br.close();

        // Delete the original file
        if (!originalFile.delete()) {
            System.out.println("Could not delete file");
            return;
        }

        // Rename the new file to the filename the original file had.
        if (!tempFile.renameTo(originalFile))
            System.out.println("Could not rename file");

    }

}
