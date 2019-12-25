package LyricsCrawler;

import Main.Tokens;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class LyricsCrawler {

    public Song getSong(String query){
        try {
            String url = "http://api.genius.com/search?q=" + URLEncoder.encode(query, "UTF-8");
            URL queryURL = new URL(url);
            Connection connection = Jsoup.connect(queryURL.toExternalForm())
                    .header("Authorization", Tokens.GENIUS_TOKEN)
                    .ignoreContentType(true);
            Document document = connection.get();
            JsonNode jn = new ObjectMapper().readTree(document.text()).get("response");
            JsonNode hits = jn.get("hits");
            if(hits.size() == 0) return null;
            JsonNode song = hits.get(0);
            int i = 1;
            while(i < hits.size() && !song.get("type").asText().equals("song")){
                song = hits.get(i);
                i++;
            }
            if(!song.get("type").asText().equals("song")) return null;
            jn = song.get("result");

            String title = jn.get("title").asText();
            String lyricsUrl = jn.get("url").asText();
            String artist = jn.get("primary_artist").get("name").asText();

            String website = getWebsiteContent(lyricsUrl);
            website = website.replaceAll("\u00a0", "");
            Document doc = Jsoup.parse(website);
            Element lyricsElement = doc.getElementsByClass("lyrics").first();
            String lyrics = br2nl(lyricsElement.html());
            while(lyrics.startsWith("\n")) lyrics = lyrics.substring(1);
            while(lyrics.endsWith("\n")) lyrics = lyrics.substring(0, lyrics.length() - 1);

            lyrics = lyrics.replace("&amp;", "&").trim();

            return new Song(title, artist, lyricsUrl, lyrics);
        }catch (Exception ex){
            return null;
        }
    }

    private static String getWebsiteContent(String website){
        try{
            URL url = new URL(website);
            URLConnection con = url.openConnection();
            con.setRequestProperty("User-Agent", "definitely not a bot");
            con.connect();
            BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        }catch (Exception ex){
            return null;
        }
    }

    private static String br2nl(String html){
        if(html==null) return null;
        Document document = Jsoup.parse(html);
        document.outputSettings(new Document.OutputSettings().prettyPrint(false));//makes html() preserve linebreaks and spacing
        document.select("br").append("\\n");
        document.select("p").prepend("\\n\\n");
        document.select("i").append("_").prepend("_");
        for(Element e : document.select("a")){
            if(e.attr("href").startsWith("http")) e.prepend("[").append("](" + e.attr("href") + ")");
        }
        //document.select("b").append("**").prepend("**");      // removed bc of own lyrics formatting
        String s = document.html().replaceAll("\\\\n", "\n");
        return Jsoup.clean(s, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
    }

}
