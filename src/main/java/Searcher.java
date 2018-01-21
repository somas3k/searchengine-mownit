import edu.stanford.nlp.process.Morphology;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

public class Searcher {
    private Map<String, Integer> indexMap = new HashMap<>();
    private static Set<String> commons = new HashSet<>();
    private OpenMapRealMatrix termByDoc;
    private HashMap<Integer, Integer> wordAppearances;
    private int docAmount = 15056;

    static {
        commons.add("a");
        commons.add("and");
        commons.add("be");
        commons.add("for");
        commons.add("from");
        commons.add("has");
        commons.add("i");
        commons.add("in");
        commons.add("is");
        commons.add("it");
        commons.add("of");
        commons.add("on");
        commons.add("to");
        commons.add("the");
        commons.add("you");
        commons.add("he");
        commons.add("she");
        commons.add("we");
        commons.add("they");
    }

    public void saveToFile(){
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("map.ser"))){
            oos.writeObject(indexMap);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void buildIndexMap(){
        int id = 0;
        Morphology morphy = new Morphology();
        try (Scanner scanner = new Scanner(Paths.get("dic.txt"))) {
            while (scanner.hasNextLine()) {
                String token = scanner.nextLine();
                if (token.length() > 1) {
                    token = token.toLowerCase();
                    if(!commons.contains(token)) {
                        indexMap.put(token, id++);
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void fillVectors(){
        Morphology morphy = new Morphology();
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream("docs.ser"));
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("docs2.ser"))) {
            Document doc = (Document) ois.readObject();

            while(doc!=null) {
                HashMap<Integer, Integer> bowVector = new HashMap<>();
                System.out.println("Reading document id: " + doc.getId());
                Scanner scanner = new Scanner(Paths.get(doc.getPath()), StandardCharsets.UTF_8.name());
                while (scanner.hasNext()) {
                    String token = scanner.next();
                    if (token.length() > 1) {
                        token = token.toLowerCase();
                        token = token.replaceAll("[^A-Za-z\\-\\s]+", "");
                        token = morphy.stem(token);
                        if(indexMap.containsKey(token)){
                            int id = indexMap.get(token);
                            if(bowVector.containsKey(id)){
                                int oldValue = bowVector.get(id);
                                bowVector.put(id, ++oldValue);
                            }
                            else{
                                bowVector.put(id, 1);
                            }
                        }
                    }
                }
                doc.setVector(bowVector);
                oos.writeObject(doc);
                doc = (Document) ois.readObject();

            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private OpenMapRealVector buildRequestVector(String request, boolean normalize){
        Morphology morphy = new Morphology();
        String[] tokens = request.split(" ");
        OpenMapRealVector v = new OpenMapRealVector(termByDoc.getRowDimension());
        for(String token : tokens){
            if(token.length()>1){
                token = token.toLowerCase();
                token = token.replaceAll("[^A-Za-z\\-\\s]+", "");
                token = morphy.stem(token);
                if(indexMap.containsKey(token)){
                    int id = indexMap.get(token);
                    v.setEntry(id, v.getEntry(id)+1);
                }
            }
        }
        if(normalize){
            double normV = v.getNorm();
            for(int i = 0; i < v.getDimension(); ++i){
                double value = v.getEntry(i);
                v.setEntry(i, value/normV);
            }
        }
        return v;
    }

    private class DocRank implements Comparable<DocRank>{
        Integer id;
        Double rank;

        @Override
        public int compareTo(DocRank docRank) {
            return rank.compareTo(docRank.rank);
        }

        @Override
        public boolean equals(Object o) {
            DocRank obj = (DocRank) o;
            return id.equals(obj.id);
        }

        public DocRank(int id, double rank) {
            this.id = id;
            this.rank = rank;
        }

        @Override
        public String toString() {
            return "DocRank{" +
                    "id=" + id +
                    ", rank=" + rank.toString() +
                    '}';
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            Object clone = super.clone();
            System.out.println(((DocRank)clone).toString());
            return new DocRank(id,rank);
        }
    }


    public void buildMatrix(){
        termByDoc = new OpenMapRealMatrix(indexMap.size(), docAmount);
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream("docs2.ser"))) {
            Document doc = (Document) ois.readObject();
            int i = 0;
            while (doc != null) {
                HashMap<Integer, Integer> vector = doc.getBowVector();
                for(Integer key : vector.keySet()){
                    termByDoc.setEntry(key,i,vector.get(key));
                }
                doc = (Document) ois.readObject();
                i++;
            }

        }

        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void saveMatrixToFile(String path){
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))){
            oos.writeObject(termByDoc);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public void loadMatrixFromFile(String path){
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))){
            termByDoc = (OpenMapRealMatrix) ois.readObject();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void processIDF(){
        wordAppearances = new HashMap<>();
        for(int i = 0; i < termByDoc.getRowDimension(); ++i){
            for(int j = 0; j < termByDoc.getColumnDimension(); ++j){
                double v = termByDoc.getEntry(i,j);
                if(v > 0){
                    if(!wordAppearances.containsKey(i)){
                        wordAppearances.put(i, 1);
                    }
                    else{
                        int app = wordAppearances.get(i);
                        wordAppearances.put(i, ++app);
                    }
                }

            }
        }
        for (int i = 0; i < termByDoc.getColumnDimension(); ++i){
            for(int j = 0; j < termByDoc.getRowDimension(); ++j){
                double v = termByDoc.getEntry(j, i);
                if(v > 0){
                    v = v * Math.log(((double)docAmount)/wordAppearances.get(j));
                    termByDoc.setEntry(j, i, v);
                }
            }
        }
    }

    public void showPartOfMatrix(){
        int i = 0;
        for(int j = 0; j < termByDoc.getRowDimension(); ++j){
            System.out.println(termByDoc.getEntry(j,i));
        }
    }

    public void normalizeVectors(){
        for(int i = 0; i < termByDoc.getColumnDimension(); ++i) {
            RealVector d = termByDoc.getColumnVector(i);
            double normD = d.getNorm();
            if(normD != 0) {
                for (int j = 0; j < termByDoc.getRowDimension(); ++j) {
                    double v = termByDoc.getEntry(j, i);
                    termByDoc.setEntry(j, i, v / normD);
                }
            }
        }
    }

    public Document[] search(String request, int k){
        OpenMapRealVector v = buildRequestVector(request, true);
        List<DocRank> ranking = new ArrayList<>();
        for(int i = 0; i < termByDoc.getColumnDimension(); ++i) {
            RealVector d = termByDoc.getColumnVector(i);
            double dotV = v.dotProduct(d);
            ranking.add(new DocRank(i,dotV));


        }
        Collections.sort(ranking);
        ranking = ranking.subList(ranking.size()-k, ranking.size());
        Collections.reverse(ranking);
        for(DocRank x : ranking){
            System.out.println(x.rank.toString());
        }

        Document[] docs = new Document[k];

        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(getClass().getResource("serialized/docs.ser").getPath()))){
            while (true) {
                Document doc = (Document) ois.readObject();

                DocRank tmp = new DocRank(doc.getId(),0);
                if(ranking.contains(tmp)){

                    docs[ranking.indexOf(tmp)] = doc;
                }

            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return docs;
    }

    public void loadDictionary(String path){
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))){
            indexMap = (Map<String, Integer>) ois.readObject();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
