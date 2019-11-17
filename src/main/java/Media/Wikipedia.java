package Media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Wikipedia{

    private String lang, title, extract;
    private boolean found;
    private int pageId;
    public Wikipedia(String lang, String word){
        this.lang = lang;
        String url = "https://" + lang + ".wikipedia.org/w/api.php?action=query&prop=extracts&format=json&explaintext&exintro&titles=" + URLEncoder.encode(word, StandardCharsets.UTF_8);
        try{
            JsonNode node = new ObjectMapper().readTree(new URL(url));
            node = node.get("query").get("pages").elements().next();
            extract = node.get("extract").asText();
            title = node.get("title").asText();
            pageId = node.get("pageid").asInt();
            found = true;
        }catch(Exception ex){
            extract = null;
            title = null;
            pageId = 0;
            found = false;
        }
    }

    public String getExtract(){return extract;}
    public String getTitle(){return title;}
    public int getPageId(){return pageId;}
    public String getUrl(){return "https://" + lang + ".wikipedia.org/?curid=" + pageId;}
    public boolean hasFound(){return found;}

}
