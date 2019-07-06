package Media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Random;

public class ImageUtils{

    public static String getRandomRedditPic(String[] subreddits){
        if(subreddits.length == 1) return getRandomRedditPic(subreddits[0]);
        return getRandomRedditPic(subreddits[new Random().nextInt(subreddits.length)]);
    }

    public static String getRandomRedditPic(String subreddit){
        try{
            Connection con = Jsoup.connect("https://reddit.com/r/" + subreddit + "/hot.json?limit=100").ignoreContentType(true);
            Document document = con.get();
            JsonNode node = new ObjectMapper().readTree(document.body().text());
            node = node.get("data").get("children");
            for(int i = 0; i < 100; i++){
                JsonNode post = node.get(new Random().nextInt(node.size())).get("data");
                if(post.has("url"))
                    if(!post.get("url").isNull())
                        return post.get("url").asText();
            }
            return "";
        }catch(Exception ex){
            ex.printStackTrace();
            return "";
        }
    }

}
