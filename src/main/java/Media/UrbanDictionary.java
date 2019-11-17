package Media;

import Main.WeightedRandomBag;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrbanDictionary {

    private List<UrbanDefinition> definitions = new ArrayList<>();
    private String url = "";
    public UrbanDictionary(String term){
        term = URLEncoder.encode(term, StandardCharsets.UTF_8);
        url = "https://urbandictionary.com/define.php?term=" + term;
        Connection con = Jsoup.connect("https://api.urbandictionary.com/v0/define?term=" + term).ignoreContentType(true);
        try{
            Document doc = con.get();
            JsonNode definitions = new ObjectMapper().readTree(doc.body().text()).get("list");
            for(int i = 0; i < definitions.size(); i++){
                this.definitions.add(new UrbanDefinition(definitions.get(i)));
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public String getUrl(){
        return url;
    }

    public UrbanDefinition getRandomDefinition(){
        if(definitions.isEmpty()) return null;
        return definitions.get(new Random().nextInt(definitions.size()));
    }
    public UrbanDefinition getVoteBasedDefinition(){
        WeightedRandomBag<UrbanDefinition> bag = new WeightedRandomBag<>();
        for(UrbanDefinition definition : definitions)
            bag.addEntry(definition, (double)definition.thumbsUp / (double)(definition.thumbsDown == 0 ? 0.8 : definition.thumbsDown));
        return bag.getRandom();
    }
    public List<UrbanDefinition> getDefinitions(){
        return definitions;
    }

    public static class UrbanDefinition {
        private List<String> soundUrls = new ArrayList<>();
        private String url;
        private int thumbsUp;
        private int thumbsDown;
        private String example;
        private Instant writtenOn;
        private int id;
        private String word;
        private String author;
        private String definition;
        public UrbanDefinition(JsonNode node){
            this.definition = node.get("definition").asText();
            this.url = node.get("permalink").asText();
            this.thumbsUp = node.get("thumbs_up").asInt();
            this.thumbsDown = node.get("thumbs_down").asInt();
            this.author = node.get("author").asText();
            this.word = node.get("word").asText();
            this.id = node.get("defid").asInt();
            this.writtenOn = Instant.parse(node.get("written_on").asText());
            this.example = node.get("example").asText();
            JsonNode sounds = node.get("sound_urls");
            for(int i = 0; i < sounds.size(); i++)
                this.soundUrls.add(sounds.get(i).asText());
        }
        public String getRawDefinition(){
            return definition;
        }
        public String getFormattedDefinition(){
            Matcher m = Pattern.compile("\\[([^\\]]*)\\]").matcher(definition);
            String formatted = definition;
            while(m.find()){
                String found = m.group();
                found = found.substring(1, found.length()-1);
                formatted = formatted.replace("[" + found + "]", "[" + found + "](https://urbandictionary.com/define.php?term=" + URLEncoder.encode(found, StandardCharsets.UTF_8) + ")");
            }
            return formatted;
        }
        public String getRawExample(){
            return example;
        }
        public String getFormattedExample(){
            Matcher m = Pattern.compile("\\[([^\\]]*)\\]").matcher(example);
            String formatted = example;
            while(m.find()){
                String found = m.group();
                found = found.substring(1, found.length()-1);
                formatted = formatted.replace("[" + found + "]", "[" + found + "](https://urbandictionary.com/define.php?term=" + URLEncoder.encode(found, StandardCharsets.UTF_8) + ")");
            }
            return formatted;
        }
        public String getAuthorName(){
            return author;
        }
        public String getAuthorUrl(){
            return "https://urbandictionary.com/author.php?author=" + URLEncoder.encode(author, StandardCharsets.UTF_8);
        }
        public String getWord(){
            return word;
        }
        public int getId(){
            return id;
        }
        public Instant getTime(){
            return writtenOn;
        }
        public String getUrl(){
            return url;
        }
        public int getUpvotes(){
            return thumbsUp;
        }
        public int getDownvotes(){
            return thumbsDown;
        }
        public String getRandomSoundUrl(){
            if(soundUrls.isEmpty()) return null;
            return soundUrls.get(new Random().nextInt(soundUrls.size()));
        }
    }

}
