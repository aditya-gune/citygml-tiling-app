import java.io.PrintWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;

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
        int n = 2;
        int m = n;

        Connection conn = null;
        Statement stmt = null;
        String[] ext = getExtents(stmt,conn, "nyc");
        double[] extents = new double[4];

        for(int i = 0; i < ext.length; i++){
            extents[i] = Double.parseDouble(ext[i]);
        }
        //extents = {xmin, ymin, xmax, ymax}
        double xmin, xmax, ymin, ymax, xw, yw, tile_xw, tile_yw, tile_xmin, tile_xmax, tile_ymin, tile_ymax;
        xmin = extents[0];
        ymin = extents[1];
        xmax = extents[2];
        ymax = extents[3];
        xw = xmax - xmin;
        yw = ymax - ymin;
        tile_xw = xw/n;
        tile_yw = yw/m;
        System.out.println(Double.toString(xw)+"/5 = "+tile_xw);
        System.out.println(Double.toString(yw)+"/5 = "+tile_yw);

        for (int j = 0; j < m; j++){
            for(int i = 0; i < n; i++){
                tile_xmin = xmin + (i*tile_xw);
                tile_xmax = tile_xmin + tile_xw;
                tile_ymin = ymin + (j*tile_yw);
                tile_ymax = tile_ymin + tile_yw;
                System.out.print("("+ Double.toString(tile_xmin)+ ", "+ Double.toString(tile_ymin)+ ")");
                System.out.println("\t("+ Double.toString(tile_xmax)+ ", "+ Double.toString(tile_ymax)+ ")");
                createConfig(tile_xmin, tile_xmax, tile_ymin, tile_ymax, i, j);

            }
            System.out.println();
        }



    }

    private static void createConfig(double tile_xmin, double tile_xmax, double tile_ymin, double tile_ymax, int i, int j) {
        String filepath ="./src/configTemplate.xml";
        String content = "";
        String newfile = "./src/nyc_"+Integer.toString(i)+"_"+Integer.toString(j)+".xml";
        try
        {
            content = new String ( Files.readAllBytes( Paths.get(filepath) ) );
            content = content.replace("XMIN", Double.toString(tile_xmin));
            content = content.replace("XMAX", Double.toString(tile_xmax));
            content = content.replace("YMIN", Double.toString(tile_ymin));
            content = content.replace("YMAX", Double.toString(tile_ymax));
            PrintWriter out = new PrintWriter(newfile);
            out.println(content);
            out.close();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


    }

    private static void export(double xmin, double xmax, double ymin, double ymax){


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
