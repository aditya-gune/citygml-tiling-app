import com.sun.deploy.util.ArrayUtil;
import org.h2.util.StringUtils;

import javax.xml.transform.Result;
import java.sql.*;
import java.io.*;
import java.util.*;
import org.apache.commons.lang3.ArrayUtils;

public class TilingAdapter {

    static final String DB_URL = "jdbc:postgresql://localhost:5432/";
    static final String USER = "postgres";
    static final String PASS = "pass";

    public static void main(String []args) throws IOException {
        if(args.length < 2){
            System.out.println("Error: please call in the format");
            System.out.println("java -jar /citygml-tiling-app.jar \"path/to/gml\" \"path/to/createdb.bat\"");
            return;
        }
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = null;
            Statement stmt = null;
            System.out.println(args);
            String path = args[0];
            String batpath = args[1];
            boolean hasPostgis = false;


            String[] files = getFiles(path);
            String[] errors = {};

            for (int i = 0; i < files.length; i++){
                System.out.print("\n\n");
                updateBatFile(batpath,files[i]);
                errors = createDB(stmt,conn,files[i], batpath, errors);

            }
            if(!ArrayUtils.isEmpty(errors)){
                System.out.println("The following databases had errors:");
                for(int i = 0; i < errors.length; i ++){
                    System.out.println(errors[i]);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }



    }

    private static String[] checkDB(Statement stmt, Connection conn, String dbname, String[] errors) {
        try{
            conn = DriverManager.getConnection(DB_URL + "postgres", USER, PASS);
            stmt = conn.createStatement();
            System.out.println("Checking CityDB " + dbname);
            ResultSet rs = stmt.executeQuery("SELECT version FROM citydb_version();");
            String version = "";
            while(rs.next())
            {
                version = rs.getString("version");
                System.out.println("CityDB version " + version);
            }

            if(!StringUtils.isNullOrEmpty(version)){
                System.out.println("Database " + dbname + "OK!");
            } else {
                ArrayUtils.add(errors, dbname);
            }
            stmt.close();
            conn.close();

            Thread.sleep(3000);

        } catch (SQLException ex) {
            System.err.println("SQLException: " + ex.getMessage());
            System.err.println("SQLState: " + ex.getSQLState());
            System.err.println("VendorError: " + ex.getErrorCode());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return errors;
    }

    private static String[] getFiles(String path) {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        String[] files = new String[listOfFiles.length];
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()&& listOfFiles[i].getName().endsWith(".gml")) {
                String filename = listOfFiles[i].getName().replace(".gml", "");;
                //System.out.println("File " + filename);
                files[i] = filename;
            }
        }
        return files;
    }

    private static String[] createDB(Statement stmt, Connection conn, String dbname, String batpath, String[] errors) {
        String DBNAME = dbname;

        try {
            //Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(DB_URL + "postgres", USER, PASS);


            stmt = conn.createStatement();
            System.out.println("Creating DB " + DBNAME);
            int i = stmt.executeUpdate(" DROP DATABASE IF EXISTS " + DBNAME + "; \n CREATE DATABASE "+ DBNAME + " WITH OWNER=postgres;" );

            stmt.close();
            conn.close();


            conn = DriverManager.getConnection(DB_URL + DBNAME, USER, PASS);
            stmt = conn.createStatement();
            stmt.executeUpdate("CREATE EXTENSION postgis;");
            ResultSet rs = stmt.executeQuery("SELECT extversion e FROM pg_catalog.pg_extension WHERE extname='postgis'");
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
