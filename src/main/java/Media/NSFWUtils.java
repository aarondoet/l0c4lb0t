package Media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class NSFWUtils {

    public static String getBoobs(){
        Connection con = Jsoup.connect("http://api.oboobs.ru/boobs/0/1/random/").ignoreContentType(true);
        try {
            Document doc = con.get();
            JsonNode pics = new ObjectMapper().readTree(doc.body().text());
            return "http://media.oboobs.ru/" + pics.get(0).get("preview").asText();
        }catch (IOException ex){
            ex.printStackTrace();
            return null;
        }
    }

    public static String getAss(){
        Connection con = Jsoup.connect("http://api.obutts.ru/butts/0/1/random/").ignoreContentType(true);
        try{
            Document doc = con.get();
            JsonNode pics = new ObjectMapper().readTree(doc.body().text());
            return "http://media.obutts.ru/" + pics.get(0).get("preview").asText();
        }catch (IOException ex){
            ex.printStackTrace();
            return null;
        }
    }

    public static String getAsian(){
        return "";
    }

    public static String getBDSM(){
        return "";
    }

    public static String getHentai(){
        return "";
    }

    public static String getLingerie(){
        return "";
    }

    public static String getNeko(){
        return "";
    }

    public static String getSnapchat(){
        return "";
    }

    public static String getTrap(){
        return "";
    }

    public static String getUniform(){
        return "";
    }

}
