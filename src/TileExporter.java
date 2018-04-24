import java.io.File;
import java.nio.file.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import org.json.*;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

public class TileExporter {

    static final String DB_URL = "jdbc:postgresql://localhost:5432/";
    static final String USER = "postgres";
    static final String PASS = "pass";
    static String DBNAME = "";
    static int N = 0;
    static int M = 0;
    static String CITYDBPATH = "";
    static String XMLPATH = "";
    static String OUTPUTPATH = "";

    public static void main(String []args) throws IOException {
        if(args.length < 5){
            System.out.println("Error: please call in the format");
            System.out.println("java -jar /citygml-tiling-app.jar DBNAME n m \"path/to/citydb.jar\" \"path/to/*.gml\"");
            for(int i =0; i < args.length; i++){
                System.out.println(Integer.toString(i)+": "+args[i]);
            }
            return;

        }

        DBNAME = args[0];            //"nyc";
        N = Integer.parseInt(args[1]);  //2
        M = Integer.parseInt(args[2]);  //n
        CITYDBPATH = args[3];        //"D:\\Program Files\\3DCityDB-Importer-Exporter\\lib\\";
        OUTPUTPATH = args[4];           //"D:\\Aditya\\Desktop\\School\\OSU\\MS\\Research\\test\\xml";
        XMLPATH = args[4]+"\\xml";        //"D:\\Aditya\\Desktop\\School\\OSU\\MS\\Research\\test\\gml";

        JSONObject tiledict = new JSONObject();
        Map<String, String> xmldict = new HashMap<String, String>();
        Connection conn = null;
        Statement stmt = null;
        double[] extents = new double[4];
        String jsonfile = OUTPUTPATH+"\\tiledict.json";

        String[] ext = getExtents(stmt,conn);


        for(int i = 0; i < ext.length; i++){
            System.out.println(ext);
            extents[i] = Double.parseDouble(ext[i]);
        }

        //extents = {xmin, ymin, xmax, ymax}
        tiledict = calculateTiling(extents, tiledict, xmldict);


        try(FileWriter file = new FileWriter(jsonfile)){
            file.write(tiledict.toString());
            System.out.println("Wrote tile dictionary to: " + jsonfile);
        }
        for(Map.Entry<String, String> e : xmldict.entrySet())
        {
            String k = e.getKey();
            export(k, e.getValue());

        }

    }

    private static JSONObject calculateTiling(double[] extents, JSONObject tiledict, Map<String,String> xmldict) {
        double xmin, xmax, ymin, ymax, xw, yw, tile_xw, tile_yw, tile_xmin, tile_xmax, tile_ymin, tile_ymax;
        xmin = extents[0];
        ymin = extents[1];
        xmax = extents[2];
        ymax = extents[3];
        xw = xmax - xmin;
        yw = ymax - ymin;
        tile_xw = xw/N;
        tile_yw = yw/M;

        try {
            //tiledict.put("key_x, key_y", "tile_xmin, tile_ymin, tile_xmax, tile_ymax");

            for (int j = 0; j < M; j++) {
                for (int i = 0; i < N; i++) {
                    tile_xmin = xmin + (i * tile_xw);
                    tile_xmax = tile_xmin + tile_xw;
                    tile_ymin = ymin + (j * tile_yw);
                    tile_ymax = tile_ymin + tile_yw;

                    System.out.print("(" + Double.toString(tile_xmin) + ", " + Double.toString(tile_ymin) + ")");
                    System.out.println("\t(" + Double.toString(tile_xmax) + ", " + Double.toString(tile_ymax) + ")");

                    xmldict = createConfig(tile_xmin, tile_xmax, tile_ymin, tile_ymax, i, j, xmldict);

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
        }catch (JSONException e) {
            e.printStackTrace();
        }
        return tiledict;
    }

    private static Map<String,String> createConfig(double tile_xmin, double tile_xmax, double tile_ymin, double tile_ymax, int i, int j, Map<String, String> xmldict) {
        Path path = FileSystems.getDefault().getPath("").toAbsolutePath();
        System.out.println("Current path: " + path);
        String filepath = path + "\\resources\\configTemplate.xml";
        String content = "";
        String key = DBNAME+"_"+Integer.toString(i)+"_"+Integer.toString(j);
        String newfile = key+".xml";
        String newpath = XMLPATH + "\\" + newfile;
        new File(XMLPATH).mkdirs();
        try
        {
            content = new String ( Files.readAllBytes( Paths.get(filepath) ) );
            content = content.replace("DBNAME", DBNAME);
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

    private static void export(String gml, String xml){
        String cmd = "java -jar \""+CITYDBPATH+"\\3dcitydb-impexp.jar \" -shell -config \"" + xml + "\" -export \"" + OUTPUTPATH+"\\"+gml+ ".gml\"";
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", cmd);
        System.out.println(cmd);
        //File dir = new File(CITYDBPATH);
        //pb.directory(dir);
        Process p = null;
        try {
            p = pb.start();
            p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static String[] getExtents(Statement stmt, Connection conn) {

        String extentsStr = "";
        String[] extents = new String[4];
        try {
            Class.forName("org.postgresql.Driver");

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
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return extents;
    }



}
