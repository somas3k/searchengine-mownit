import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class Main {
    private static final Searcher searcher;

    static{
        //Document.buildDocsDescriptions("wiki_00");
        searcher = new Searcher();
        searcher.loadDictionary(searcher.getClass().getResource("serialized/map.ser").getPath());
        //searcher.buildMatrix();
        //searcher.processIDF();
        //searcher.normalizeVectors();
        //searcher.saveMatrixToFile("src/main/resources/serialized/matrixIDFNorm.ser");
        searcher.loadMatrixFromFile(searcher.getClass().getResource("serialized/matrixIDFNorm.ser").getPath());
    }

    private static String prepareTable(Document[] docs){
        StringBuilder sb = new StringBuilder();
        sb.append("<tr><th>Title</th><th>Local Path</th><th>URL</th></tr>");
        for(Document doc : docs){
            sb.append("<tr><th>").append(doc.getTitle()).append("</th><th><a href=\"").append(searcher.getClass()
                    .getResource(doc.getPath())).append("\">").append(doc.getTitle()).append("</a>")
                    .append("</th><th><a href=\"").append(doc.getUrl()).append("\">Link</a></th></tr>");
        }
        return sb.toString();
    }
    public static void main(String[] args) {
        staticFileLocation("docs");
        get("/", (request, response) -> {
            Map<String, String> model = new HashMap<>();
            model.put("template", "templates/home.vtl" );
            return new ModelAndView(model, "templates/layout.vtl");
        }, new VelocityTemplateEngine());
        get("/search", (request, response) -> {
            String query = request.queryParams("search");
            Document[] docs = searcher.search(query, 10);
            Map<String, String> model = new HashMap<>();
            model.put("table", prepareTable(docs));
            model.put("template", "templates/search.vtl" );
            model.put("query", query);
            return new ModelAndView(model, "templates/layout.vtl");
        }, new VelocityTemplateEngine());

    }
}
