package Main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.object.entity.*;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotUtils {

    public static DiscordClient getClient(String token){
        return new DiscordClientBuilder(token).build();
    }

    public static List<Long> getBotAdmins(){
        return Arrays.asList(226677096091484160L);
    }

    public static Mono<String> getPrefix(long gId){
        try {
            ResultSet rs = DataManager.getGuild(gId);
            if(rs == null) return Mono.just("=");
            String pref = rs.getString("bot_prefix");
            if(pref == null) return Mono.just("=");
            return Mono.just(pref);
        } catch (Exception e) {
            e.printStackTrace();
            return Mono.just("=");
        }
    }

    public static Mono<List<String>> messageToArgs(String content){
        if(content.trim().length() == 0) return Mono.just(Arrays.asList());
        String[] args = content.split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        for(int i = 0; i < args.length; i++){
            if(args[i].startsWith("\"") && args[i].endsWith("\""))
                args[i] = args[i].substring(1, args[i].length() - 1);
        }
        return Mono.just(Arrays.asList(args));
    }

    public static boolean isCommand(String content, String[] cmd, String prefix){
        prefix = escapeRegex(prefix);
        String bId = BotMain.client.getSelfId().get().asString();
        return Pattern.compile("^(?:" + prefix + "|\\<@\\!?" + bId + "\\> ?)(?:" + String.join("|", cmd) + ")(?: +(.*))?$").matcher(content).matches();
    }

    public static Mono<String> truncateMessage(String content, String[] cmd, String prefix){
        prefix = escapeRegex(prefix);
        String bId = BotMain.client.getSelfId().get().asString();
        Matcher matcher = Pattern.compile("^(?:" + prefix + "|\\<@\\!?" + bId + "\\> ?)(?:" + String.join("|", cmd) + ")(?: +(.*))?$").matcher(content);
        if(matcher.matches())
            return Mono.just(matcher.group(1) == null ? "" : matcher.group(1));
        else
            return Mono.just("");
    }

    public static String escapeRegex(String text){
        return text.replaceAll("[\\<\\(\\[\\{\\\\\\^\\-\\=\\$\\!\\|\\]\\}\\)\\?\\*\\+\\.\\>]", "\\\\$0");
    }

    public static Color getColor(String color, Color defaultColor){
        if(Pattern.matches("^#[0-9a-fA-F]+$", color)){
            switch (color.length()){
                case 4:
                    return new Color(
                            Integer.valueOf(color.substring(1, 2), 16) * 17,
                            Integer.valueOf(color.substring(2, 3), 16) * 17,
                            Integer.valueOf(color.substring(3, 4), 16) * 17
                    );
                case 7:
                    return new Color(
                            Integer.valueOf(color.substring(1, 3), 16),
                            Integer.valueOf(color.substring(3, 5), 16),
                            Integer.valueOf(color.substring(5, 7), 16)
                    );
            }
        }else if(Pattern.matches("^\\d+$", color)){
            try {
                return new Color(Integer.parseInt(color));
            }catch (Exception ex){}
        }
        try {
            return (Color)Color.class.getField(color.toLowerCase()).get(null);
        }catch (Exception ex){
            return defaultColor;
        }
    }

    public static Consumer<MessageCreateSpec> jsonToMessage(String text){
        return mcs -> {
            try {
                if(text.trim().startsWith("{") && text.trim().endsWith("}")) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode message = mapper.readTree(text);
                    System.out.println(message);
                    mcs.setContent(message.has("content") ? message.get("content").asText() : "");
                    mcs.setEmbed(ecs -> {
                        if (message.has("title")) ecs.setTitle(message.get("title").asText());
                        if (message.has("description")) ecs.setDescription(message.get("description").asText());
                        if (message.has("color")) ecs.setColor(getColor(message.get("color").asText(), Color.BLACK));
                        if (message.has("footer"))
                            ecs.setFooter(message.get("footer").asText(), message.has("footerIcon") ? message.get("footerIcon").asText() : null);
                        if (message.has("author"))
                            ecs.setAuthor(message.get("author").asText(), message.has("footerUrl") ? message.get("footerUrl").asText() : null, message.has("footerIcon") ? message.get("footerIcon").asText() : null);
                        if (message.has("image")) ecs.setImage(message.get("image").asText());
                        if (message.has("thumbnail")) ecs.setThumbnail(message.get("thumbnail").asText());
                        if (message.has("url") && message.has("title")) ecs.setUrl(message.get("url").asText());
                        if (message.has("fields")) {
                            if (message.get("fields").isArray()) {
                                message.get("fields").elements().forEachRemaining(field -> {
                                    if (field.has("name") && field.has("value"))
                                        ecs.addField(field.get("name").asText(), field.get("value").asText(), field.has("inline") ? field.get("inline").asBoolean() : false);
                                });
                            }
                        }
                    });
                }else
                    mcs.setContent(text);
            }catch (Exception ex){
                mcs.setContent(text);
            }
        };
    }

    public static Mono<Role> getRoleFromArgument(Guild g, String role){
        if(Pattern.matches("\\d+", role))
            return g.getRoleById(Snowflake.of(Long.parseLong(role))).onErrorResume(x -> Mono.empty());
        else{
            Matcher m = Pattern.compile("\\<@&(\\d+)\\>").matcher(role);
            if(m.matches())
                return g.getRoleById(Snowflake.of(Long.parseLong(m.group(1)))).onErrorResume(x -> Mono.empty());
        }
        return Mono.empty();
    }

    public static Mono<Member> getMemberFromArgument(Guild g, String member){
        if(Pattern.matches("\\d+", member))
            return g.getMemberById(Snowflake.of(Long.parseLong(member))).onErrorResume(x -> Mono.empty());
        else{
            Matcher m = Pattern.compile("\\<@\\!?(\\d+)\\>").matcher(member);
            if(m.matches())
                return g.getMemberById(Snowflake.of(Long.parseLong(m.group(1)))).onErrorResume(x -> Mono.empty());
        }
        return Mono.empty();
    }

    public static Mono<GuildChannel> getChannelFromArgument(Guild g, String channel){
        if(Pattern.matches("\\d+", channel))
            return g.getChannelById(Snowflake.of(Long.parseLong(channel))).onErrorResume(x -> Mono.empty());
        else{
            Matcher m = Pattern.compile("\\<#(\\d+)\\>").matcher(channel);
            if(m.matches())
                return g.getChannelById(Snowflake.of(Long.parseLong(m.group(1)))).onErrorResume(x -> Mono.empty());
        }
        return Mono.empty();
    }

    public static Mono<User> getUserFromArgument(String user){
        if(Pattern.matches("\\d+", user))
            return BotMain.client.getUserById(Snowflake.of(Long.parseLong(user))).onErrorResume(x -> Mono.empty());
        else{
            Matcher m = Pattern.compile("\\<@\\!?(\\d+)\\>").matcher(user);
            if(m.matches())
                return BotMain.client.getUserById(Snowflake.of(Long.parseLong(m.group(1)))).onErrorResume(x -> Mono.empty());
        }
        return Mono.empty();
    }


    public static Instant getSnowflakeCreationDate(Long id){
        return Instant.ofEpochMilli(Math.round((double)id / 4194304d + 1420070400000d));
    }


    public static Mono<Message> sendHelpMessage(MessageChannel c, String cmd, String pref){
        return c.createMessage("Error in command " + cmd);
    }

    public static Mono<Message> sendErrorMessage(MessageChannel c){
        return c.createMessage("An error occured while performing this action.");
    }

    /**
     * Parses a string to the duration in milliseconds
     *
     * @param dur The string that represents the duration
     * @return The duration in milliseconds or <code>-1</code> if the duration could not be parsed
     */
    public static Long getDuration(String dur){
        long d = 0L;
        Matcher m = Pattern.compile("^(?:(?:(\\d+):)?(?:(\\d+):))?(\\d+)(?:\\.(\\d+))?$").matcher(dur);
        if(m.matches()){
            System.out.println("\n");
            System.out.println(m.group(1));
            System.out.println(m.group(2));
            System.out.println(m.group(3));
            System.out.println(m.group(4));
        }else{
            m = Pattern.compile("(?: *(\\d+) *h| *(\\d+) *min| *(\\d+) *s| *(\\d+) *ms)+").matcher(dur);
            if(m.matches()){
                System.out.println("\n");
                System.out.println(m.group(1));
                System.out.println(m.group(2));
                System.out.println(m.group(3));
                System.out.println(m.group(4));
            }else{
                return -1L;
            }
        }
        return d;
    }


}