package Main;

import DataManager.DataManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateSpec;
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

    public static EmbedCreateSpec addEmbedFields(String lang, EmbedCreateSpec spec, String key, String... args){
        if(!languages.has(lang)) lang = "en";
        JsonNode el = getLanguageElement(lang, key);
        el.elements().forEachRemaining(field -> {
            JsonNode e = field.get("content");
            String content;
            if(e.isArray()){
                List<String> lines = new ArrayList<>();
                e.elements().forEachRemaining(line -> lines.add(BotUtils.formatString(line.asText(), args)));
                content = String.join("\n", lines);
            }else content = e.asText();
            spec.addField(BotUtils.formatString(field.get("title").asText(), args), content, field.has("inline") && field.get("inline").asBoolean());
        });
        return spec;
    }

    public static Consumer<EmbedCreateSpec> getLanguageMessage(String lang, String key, String... args){
        if(!languages.has(lang)) lang = "en";
        JsonNode curr = languages.get(lang);
        for(String k : key.split("\\."))
            curr = curr.get(k);
        JsonNode el = curr;
        return ecs -> {
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
            if(el.has("color")){
                if(el.get("color").isInt())
                    ecs.setColor(new Color(el.get("color").asInt()));
                else{
                    String col = el.get("color").asText();
                    if(col.equalsIgnoreCase("SUCCESS"))
                        ecs.setColor(BotUtils.lightGreen);
                    else if(col.equalsIgnoreCase("ERROR"))
                        ecs.setColor(BotUtils.lighterRed);
                    else
                        ecs.setColor(BotUtils.getColor(col, BotUtils.botColor));
                }
            }else ecs.setColor(BotUtils.botColor);
            if(el.has("author")) ecs.setAuthor(BotUtils.formatString(el.get("author").asText(), args), el.has("authorUrl") ? BotUtils.formatString(el.get("authorUrl").asText(), args) : null, el.has("authorIcon") ? el.get("authorIcon").asText() : null);
            if(el.has("footer")) ecs.setFooter(BotUtils.formatString(el.get("footer").asText(), args), el.has("footerIcon") ? el.get("footerIcon").asText() : null);
            if(el.has("fields")){
                el.get("fields").elements().forEachRemaining(field -> {
                    JsonNode e = field.get("content");
                    String val;
                    if(e.isArray()){
                        List<String> lines = new ArrayList<>();
                        e.elements().forEachRemaining(line -> lines.add(BotUtils.formatString(line.asText(), args)));
                        val = String.join("\n", lines);
                    }else val = e.asText();
                    ecs.addField(BotUtils.formatString(field.get("title").asText(), args), BotUtils.formatString(val, args), field.has("inline") && field.get("inline").asBoolean());
                });
            }
        };
    }

    public static String getGuildLanguage(Long gId){
        return DataManager.getGuild(gId).getLanguage();
    }

    public static String getGuildLanguage(@Nullable Guild g){
        if(g != null) return getGuildLanguage(g.getId().asLong());
        else return "en";
    }

    public static Map<String, String[]> getAvailableLanguages(){
        Map<String, String[]> langs = new HashMap<>();
        languages.fields().forEachRemaining(field -> langs.put(field.getKey(), new String[]{field.getValue().get("languageName").asText(), field.getValue().get("englishLanguageName").asText()}));
        return langs;
    }

    /**
     * Gives you the language of the language passed to the function. This function takes either the language code itself, the name of the language in the language itself or the English name of the language
     *
     * @param lang The language you want to get the language code of
     * @return The language code or {@code null} if the language is not found in the language file
     */
    public static String getLanguage(String lang){
        return getAvailableLanguages().entrySet().stream().filter(language -> language.getKey().equalsIgnoreCase(lang) || Arrays.stream(language.getValue()).anyMatch(ln -> ln.equalsIgnoreCase(lang))).findAny().map(Map.Entry::getKey).orElse(null);
    }

}
