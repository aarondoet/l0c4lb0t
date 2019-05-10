package Main;

import DataManager.DataManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.core.spec.MessageCreateSpec;

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
        if(!languages.has(lang)) lang = "en";
        JsonNode curr = languages.get(lang);
        for(String k : key.split("\\."))
            curr = curr.get(k);
        return BotUtils.formatString(curr.asText(), args);
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
                ecs.setDescription(BotUtils.formatString(el.get("content").asText(), args));
                ecs.setColor(new Color(el.get("color").asInt()));
                if(el.has("author")) ecs.setAuthor(el.get("author").asText(), el.has("authorUrl") ? el.get("authorUrl").asText() : null, el.has("authorIcon") ? el.get("authorIcon").asText() : null);
                if(el.has("footer")) ecs.setFooter(el.get("footer").asText(), el.has("footerIcon") ? el.get("footerIcon").asText() : null);
            });
        };
    }

    public static String getGuildLanguage(Long gId){
        String lang = "en";
        try{lang = DataManager.getGuild(gId).getLanguage();}catch(Exception ex){}
        return lang;
    }

    public static Map<String, String> getAvailableLanguages(){
        Map<String, String> langs = new HashMap<>();
        languages.fields().forEachRemaining(field -> langs.put(field.getKey(), field.getValue().get("languageName").asText()));
        return langs;
    }

}
