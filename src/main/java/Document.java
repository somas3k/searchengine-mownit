import org.apache.commons.math3.linear.OpenMapRealVector;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class Document implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String path;
    private String url;
    private String title;
    private HashMap<Integer, Integer> bowVector;

    public Document(int id, String path, String url, String title) {
        this.id = id;
        this.path = path;
        this.url = url;
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public int getId() {
        return id;
    }

    public void setVector(HashMap<Integer,Integer> vector) {
       this.bowVector = vector;
    }

    public HashMap<Integer, Integer> getBowVector() {
        return bowVector;
    }

    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", url='" + url + '\'' +
                ", title='" + title + '\'' +
                '}';
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    private static Document buildNewDocument(String line, Scanner scanner, int id) throws IOException {

        String url = "";
        StringTokenizer parser = new StringTokenizer(line, " ", true);
        String token;
        while(parser.hasMoreTokens()){
            token = parser.nextToken();
            if(token.contains("url=")){
                String[] splitted = token.split(Pattern.quote("\""));
                url = splitted[1];
            }
        }
        String path = "docs/" + id + ".txt";
        String line1 = scanner.nextLine();
        Document doc = new Document(id, path, url, line1);
        while(!line1.contains("</doc>")){
            if(scanner.hasNextLine()){
                line1 = scanner.nextLine();
            }
            else {
                System.out.println("Problem");
            }
        }
        return doc;

    }

    public static void buildDocsDescriptions(String path){
        ArrayList<Document> docs = new ArrayList<>();
        int id = 0;
        try (Scanner scanner = new Scanner(Paths.get(path), StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("<doc id=")) {
                    docs.add(buildNewDocument(line, scanner, id++));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream("docs.ser"))) {
            for (Document doc : docs) {
                oos.writeObject(doc);
                //System.out.println("Done");
                System.out.println(doc);
            }
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }
}
