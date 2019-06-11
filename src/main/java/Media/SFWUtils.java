package Media;

import Main.Tokens;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class SFWUtils {

    public static String getDog(){
        Connection con = Jsoup.connect("https://api.thedogapi.com/v1/images/search")
                .header("x-api-key", Tokens.DOG_API_KEY)
                .ignoreContentType(true);
        try {
            Document doc = con.get();
            return new ObjectMapper().readTree(doc.body().text()).get(0).get("url").asText();
        }catch (IOException ex){
            ex.printStackTrace();
            return null;
        }
    }

    public static String getCat(){
        Connection con = Jsoup.connect("https://api.thecatapi.com/v1/images/search")
                .header("x-api-key", Tokens.CAT_API_KEY)
                .ignoreContentType(true);
        try{
            Document doc = con.get();
            return new ObjectMapper().readTree(doc.body().text()).get(0).get("url").asText();
        }catch (IOException  ex){
            ex.printStackTrace();
            return null;
        }
    }

    public static String getCock(){
        return "";
    }

    public static String getRandomRedditPic(String subreddit){
        return "";
    }

}
