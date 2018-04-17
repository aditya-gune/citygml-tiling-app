import com.sun.deploy.util.ArrayUtil;
import org.citydb.cmd.ImpExpCmd;
import org.citydb.config.Config;
import org.citygml4j.builder.jaxb.JAXBBuilder;
import org.h2.util.StringUtils;

import javax.xml.transform.Result;
import java.sql.*;
import java.io.*;
import java.util.*;
import org.apache.commons.lang3.ArrayUtils;
import javax.xml.bind.*;

import org.citydb.*;


class Point{
    private double x;
    private double y;

    public Point(double x, double y){
        this.x = x;
        this.y = y;
    }

    public double getX(){return this.x;}
    public double getY(){return this.y;}


}

public class TileExporter {

    static final String DB_URL = "jdbc:postgresql://localhost:5432/";
    static final String USER = "postgres";
    static final String PASS = "pass";


    public static void main(String []args) throws IOException {
        /*if(args.length < 2){
            System.out.println("Error: please call in the format");
            System.out.println("java -jar /citygml-tiling-app.jar \"path/to/gml\" \"path/to/createdb.bat\"");
            return;
        }*/
        JAXBBuilder jb;
        JAXBContext kmlContext, colladaContext, projectContext, guiContext;
        Connection conn = null;
        Statement stmt = null;
        String[] ext = getExtents(stmt,conn, "nyc");
        double[] extents = new double[4];

        for(int i = 0; i < ext.length; i++){
            extents[i] = Double.parseDouble(ext[i]);
        }
        Config cfg = new Config();
        ImpExpCmd gml = new ImpExpCmd(jb, projectContext, guiContext, cfg);


    }

    private static String[] getExtents(Statement stmt, Connection conn, String dbname) {
        String DBNAME = dbname;
        String extentsStr = "";
        String[] extents = new String[4];
        try {
            //Class.forName("org.postgresql.Driver");

            conn = DriverManager.getConnection(DB_URL + DBNAME, USER, PASS);
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("Select ST_Extent(ST_Transform(envelope, 4326)) e from citydb.cityobject where envelope is not null");


            while(rs.next())
            {
                extentsStr = rs.getString("e");
                System.out.println("Extents are " + extentsStr);

            }
            stmt.close();
            conn.close();

            if(extentsStr != null)
            {
                String[] rex = extentsStr.split(",");

                String[] lowerleft = rex[0].split("\\s");
                String[] upperright = rex[1].split("\\s");
                for (int i = 0; i < lowerleft.length; i++){
                    lowerleft[i] = lowerleft[i].replaceAll("[^\\d-.]", "");
                    upperright[i]=upperright[i].replaceAll("[^\\d-.]", "");
                }
                System.out.println("Extents are ");
                System.out.println(Arrays.toString(lowerleft));
                System.out.println(Arrays.toString(upperright));
                extents[0] = lowerleft[0];
                extents[1] = lowerleft[1];
                extents[2] = upperright[0];
                extents[3] = upperright[1];
            }


        } catch (SQLException ex) {
            System.err.println("SQLException: " + ex.getMessage());
            System.err.println("SQLState: " + ex.getSQLState());
            System.err.println("VendorError: " + ex.getErrorCode());
        }
        
        return extents;
    }



}
