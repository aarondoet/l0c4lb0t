package Main;

import DataManager.DataManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.MessageCreateSpec;
import reactor.util.annotation.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class LocaleManager {

    private static JsonNode languages;
    public static void initialize(){
        try {
            languages = new ObjectMapper().readTree(BotMain.class.getResource("/languages.json"));
        } catch (Exception e) {
            e.printStackTrace();
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    initialize();
                }
            }, 100);
        }
    }

    public static String getLanguageString(String lang, String key, String... args){
        JsonNode el = getLanguageElement(lang, key);
        if(el.isArray()){
            List<String> lines = new ArrayList<>();
            el.elements().forEachRemaining(line -> lines.add(BotUtils.formatString(line.asText(), args)));
            return String.join("\n", lines);
        }
        return BotUtils.formatString(el.asText(), args);
    }
    public static JsonNode getLanguageElement(String lang, String key){
        if(!languages.has(lang)) lang = "en";
        JsonNode curr = languages.get(lang);
        for(String k : key.split("\\."))
            curr = curr.get(k);
        return curr;
    }

    public static Consumer<MessageCreateSpec> getLanguageMessage(String lang, String key, String... args){
        if(!languages.has(lang)) lang = "en";
        JsonNode curr = languages.get(lang);
        for(String k : key.split("\\."))
            curr = curr.get(k);
        JsonNode el = curr;
        return mcs -> {
            mcs.setEmbed(ecs -> {
                ecs.setTitle(BotUtils.formatString(el.get("title").asText(), args));
                JsonNode node = el.get("content");
                String content = "";
                if(node.isArray()){
                    for(int i = 0; i < node.size(); i++)
                        content += "\n" + node.get(i).asText();
                    content = content.substring(1);
                }else
                    content = node.asText();
                ecs.setDescription(BotUtils.formatString(content, args));
                if(el.has("color")) ecs.setColor(new Color(el.get("color").asInt())); else ecs.setColor(BotUtils.botColor);
                if(el.has("author")) ecs.setAuthor(el.get("author").asText(), el.has("authorUrl") ? el.get("authorUrl").asText() : null, el.has("authorIcon") ? el.get("authorIcon").asText() : null);
                if(el.has("footer")) ecs.setFooter(el.get("footer").asText(), el.has("footerIcon") ? el.get("footerIcon").asText() : null);
            });
        };
    }

    public static String getGuildLanguage(Long gId){
        return DataManager.getGuild(gId).getLanguage();
    }

    public static String getGuildLanguage(@Nullable Guild g){
        if(g != null) return getGuildLanguage(g.getId().asLong());
        else return "en";
    }

    public static Map<String, String> getAvailableLanguages(){
        Map<String, String> langs = new HashMap<>();
        languages.fields().forEachRemaining(field -> langs.put(field.getKey(), field.getValue().get("languageName").asText()));
        return langs;
    }

}
