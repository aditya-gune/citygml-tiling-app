import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
//import org.json.*;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

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
        String dbname = "nyc";
        String citydbpath = "D:\\Program Files\\3DCityDB-Importer-Exporter\\lib\\";
        String outputpath = "D:\\Aditya\\Desktop\\School\\OSU\\MS\\Research\\test\\gml";
        String xmlpath = "D:\\Aditya\\Desktop\\School\\OSU\\MS\\Research\\test\\xml";
        int n = 2;
        int m = n;
        Map<String, String> tiledict = new HashMap<String, String>();
        Map<String, String> xmldict = new HashMap<String, String>();
        Connection conn = null;
        Statement stmt = null;
        String[] ext = getExtents(stmt,conn, dbname);
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

                xmldict = createConfig(tile_xmin, tile_xmax, tile_ymin, tile_ymax, i, j, xmlpath, xmldict);

                String is = Integer.toString(i);
                String js = Integer.toString(j);
                String txmin = Double.toString(tile_xmin);
                String txmax = Double.toString(tile_xmax);
                String tymin = Double.toString(tile_ymin);
                String tymax = Double.toString(tile_ymax);
                String key = is + "," + js;
                String val = txmin + "," + tymin + "," + txmax + "," + tymax;
                tiledict.put(key, val);

            }
            System.out.println();
        }
        for(Map.Entry<String, String> e : xmldict.entrySet())
        {
            String k = dbname + "_" + e.getKey().replace(",","_");
            export(k, xmlpath, citydbpath, outputpath);

        }


    }

    private static Map<String,String> createConfig(double tile_xmin, double tile_xmax, double tile_ymin, double tile_ymax, int i, int j, String xmlpath, Map<String, String> xmldict) {
        String filepath ="./src/configTemplate.xml";
        String content = "";
        String key = "nyc_"+Integer.toString(i)+"_"+Integer.toString(j);
        String newfile = key+".xml";
        String newpath = xmlpath + "\\" + newfile;
        try
        {
            content = new String ( Files.readAllBytes( Paths.get(filepath) ) );
            content = content.replace("XMIN", Double.toString(tile_xmin));
            content = content.replace("XMAX", Double.toString(tile_xmax));
            content = content.replace("YMIN", Double.toString(tile_ymin));
            content = content.replace("YMAX", Double.toString(tile_ymax));
            PrintWriter out = new PrintWriter(newpath);
            out.println(content);
            out.close();
            xmldict.put(key, newpath);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return xmldict;

    }

    private static void export(String gml, String xmlpath, String citydbpath, String outputpath){
        String cmd = "java -jar \""+citydbpath+"3dcitydb-impexp.jar \" -shell -config \"" + xmlpath + "\" -export \"" + outputpath+"\\"+gml+ ".gml\"";
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", cmd);
        System.out.println(cmd);
        //File dir = new File(citydbpath);
        //pb.directory(dir);
        Process p = null;
        try {
            p = pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
