package Main;

import DataManager.DataManager;
import DataManager.SQLGuild;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.object.entity.*;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.core.util.EntityUtil;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotUtils {

    /**
     * Gets the {@link DiscordClient} by the token
     * @param token The bot token
     * @return The {@link DiscordClient}
     */
    public static DiscordClient getClient(String token){
        return new DiscordClientBuilder(token).build();
    }

    /**
     * Gets all bot administrators
     * @return A {@link List} of all bot administrators
     */
    public static List<Long> getBotAdmins(){return Arrays.asList(226677096091484160L);}

    /**
     * Gets the prefix of a {@link Guild}
     * @param gId The ID of the {@link Guild}
     * @return The prefix
     */
    public static Mono<String> getPrefix(long gId){
        SQLGuild sg = DataManager.getGuild(gId);
        if(sg == null) return Mono.just("=");
        String pref = sg.getBotPrefix();
        if(pref == null) return Mono.just("=");
        return Mono.just(pref);
    }

    /**
     * Splits the content into single arguments
     * @param content The content you want to parse
     * @return The content split into arguments
     */
    public static Mono<List<String>> messageToArgs(String content){
        /*if(content.trim().length() == 0) return Mono.just(Arrays.asList());
        String[] args = content.split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        for(int i = 0; i < args.length; i++){
            if(args[i].startsWith("\"") && args[i].endsWith("\""))
                args[i] = args[i].substring(1, args[i].length() - 1);
        }
        return Mono.just(new ArrayList<>(Arrays.asList(args)));*/
        List<String> args = new ArrayList<>();
        boolean escaped = false;
        boolean inQuotes = false;
        boolean endedQuote = false;
        List<Integer> toDelete = new ArrayList<>();
        String currArg = "";
        for(char c : content.toCharArray()){
            if(endedQuote){
                endedQuote = false;
                if(c == ' ') continue;
            }
            if(c == ' ' && !inQuotes){
                args.add(currArg);
                currArg = "";
                escaped = false;
                continue;
            }
            if(c == '"' && inQuotes && !escaped){
                args.add(currArg);
                currArg = "";
                inQuotes = false;
                endedQuote = true;
                continue;
            }
            if(c == '\\' && !escaped){
                escaped = true;
                continue;
            }
            if(c == '"' && !escaped && !inQuotes){
                inQuotes = true;
                continue;
            }
            escaped = false;
            currArg += "" + c;
        }
        if(!endedQuote) args.add(currArg);
        return Mono.just(args);
    }

    /**
     * Returns whether the {@link String} is the command or not
     * @param content The message content you want to check
     * @param cmd     The array of {@link String}s that are valid names for command you want to test for
     * @param prefix  The prefix the message should start with
     * @return Whether the {@link String} is the given command
     */
    public static boolean isCommand(String content, String[] cmd, String prefix){
        prefix = escapeRegex(prefix);
        String bId = BotMain.client.getSelfId().get().asString();
        return Pattern.compile("^(?:" + prefix + "|\\<@\\!?" + bId + "\\> ?)(?:" + String.join("|", cmd) + ")(?: +(.*))?$", Pattern.CASE_INSENSITIVE).matcher(content).matches();
    }

    /**
     * Truncates a {@link String}
     * @param content The message content you want to truncate
     * @param cmd     The array of {@link String}s that are valid names for the command
     * @param prefix  The prefix
     * @return The truncated {@link String}
     */
    public static Mono<String> truncateMessage(String content, String[] cmd, String prefix){
        prefix = escapeRegex(prefix);
        String bId = BotMain.client.getSelfId().get().asString();
        Matcher matcher = Pattern.compile("^(?:" + prefix + "|\\<@\\!?" + bId + "\\> ?)(?:" + String.join("|", cmd) + ")(?: +(.*))?$", Pattern.CASE_INSENSITIVE).matcher(content);
        if(matcher.matches())
            return Mono.just(matcher.group(1) == null ? "" : matcher.group(1));
        else
            return Mono.just("");
    }

    /**
     * Escapes all RegEx characters in a {@link String}
     * @param text The {@link String} you want to escape
     * @return The escaped {@link String}
     */
    public static String escapeRegex(String text){
        return text.replaceAll("[\\<\\(\\[\\{\\\\\\^\\-\\=\\$\\!\\|\\]\\}\\)\\?\\*\\+\\.\\>]", "\\\\$0");
    }

    /**
     * Gets the {@link Color} by a {@link String}<br/>
     * This can be a {@link String} like {@code #rrggbb}, {@code #rgb}, {@code 1234567} or {@code RED}
     * @param color        The {@link String} representation of the {@link Color}
     * @param defaultColor The {@link Color} that should be returned if the {@link String} could not be converted
     * @return The parsed color or the {@code defaultColor}
     */
    public static Color getColor(String color, @Nullable Color defaultColor){
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

    /**
     * Gets a {@link MessageCreateSpec} by a {@link String}
     * @param text The {@link String} representation of the {@link MessageCreateSpec}
     * @return The text parsed to a {@link MessageCreateSpec}
     */
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
                        if (message.has("timestamp")) ecs.setTimestamp(Instant.ofEpochMilli(message.get("timestamp").asLong()));
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

    /**
     * Gets a {@link MessageEditSpec} by a {@link String}
     * @param text The {@link String} representation of the {@link MessageEditSpec}
     * @return The text parsed to a {@link MessageEditSpec}
     */
    public static Consumer<MessageEditSpec> jsonToMessageEdit(String text){
        return mes -> {
            try {
                if(text.trim().startsWith("{") && text.trim().endsWith("}")) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode message = mapper.readTree(text);
                    System.out.println(message);
                    mes.setContent(message.has("content") ? message.get("content").asText() : "");
                    mes.setEmbed(ecs -> {
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
                    mes.setContent(text);
            }catch (Exception ex){
                mes.setContent(text);
            }
        };
    }

    /**
     * Gets a {@link Role} by a {@link String}
     * @param guild The {@link Guild} the {@link Role} is in
     * @param role  The {@link String} representation of the {@link Role}
     * @return The {@link Role}
     */
    public static Mono<Role> getRoleFromArgument(Guild guild, String role){
        /*if(Pattern.matches("\\d+", role))
            return g.getRoleById(Snowflake.of(Long.parseLong(role))).onErrorResume(x -> Mono.empty());
        else{
            Matcher m = Pattern.compile("\\<@&(\\d+)\\>").matcher(role);
            if(m.matches())
                return g.getRoleById(Snowflake.of(Long.parseLong(m.group(1)))).onErrorResume(x -> Mono.empty());
        }
        return Mono.empty();*/
        return guild.getRoles().filter(r -> r.getId().asString().equals(role) || r.getMention().equals(role) || r.getName().equals(role)).next();
    }

    /**
     * Gets a {@link Member} by a {@link String}
     * @param guild  The {@link Guild} the {@link Member} is in
     * @param member The {@link String} representation of the {@link Member}
     * @return The {@link Member}
     */
    public static Mono<Member> getMemberFromArgument(Guild guild, String member){
        /*if(Pattern.matches("\\d+", member))
            return g.getMemberById(Snowflake.of(Long.parseLong(member))).onErrorResume(x -> Mono.empty());
        else{
            Matcher m = Pattern.compile("\\<@\\!?(\\d+)\\>").matcher(member);
            if(m.matches())
                return g.getMemberById(Snowflake.of(Long.parseLong(m.group(1)))).onErrorResume(x -> Mono.empty());
        }
        return Mono.empty();*/
        return guild.getMembers().filter(m -> m.getId().asString().equals(member) || ("<@" + m.getId().asString() + ">").equals(member) || ("<@!" + m.getId().asString() + ">").equals(member) || (m.getUsername() + "#" + m.getDiscriminator()).equals(member)).next();
    }

    /**
     * Gets a {@link GuildChannel} by a {@link String}
     * @param guild   The {@link Guild} the {@link GuildChannel} is in
     * @param channel The {@link String} representation of the {@link GuildChannel}
     * @return The {@link GuildChannel}
     */
    public static Mono<GuildChannel> getChannelFromArgument(Guild guild, String channel){
        /*Matcher m = Pattern.compile("^\\<#(\\d+)\\>$").matcher(channel);
        if(m.matches())
            return guild.getChannelById(Snowflake.of(Long.parseLong(m.group(1)))).onErrorResume(x -> Mono.empty())
                    .switchIfEmpty(guild.getChannels().filter(c -> c.getName().equals(channel)).next());
        return guild.getChannelById(Snowflake.of(Long.parseLong(channel))).onErrorResume(x -> Mono.empty())
                .switchIfEmpty(guild.getChannels().filter(c -> c.getName().equals(channel)).next());*/
        return guild.getChannels().filter(c -> c.getId().asString().equals(channel) || c.getMention().equals(channel) || c.getName().equals(channel)).next();
    }

    /**
     * Gets a {@link User} by a {@link String}
     * @param user The {@link String} representation of the {@link User}
     * @return The {@link User}
     */
    public static Mono<User> getUserFromArgument(String user){
        /*if(Pattern.matches("\\d+", user))
            return BotMain.client.getUserById(Snowflake.of(Long.parseLong(user))).onErrorResume(x -> Mono.empty());
        else{
            Matcher m = Pattern.compile("\\<@\\!?(\\d+)\\>").matcher(user);
            if(m.matches())
                return BotMain.client.getUserById(Snowflake.of(Long.parseLong(m.group(1)))).onErrorResume(x -> Mono.empty());
        }
        return Mono.empty();*/
        return BotMain.client.getUsers().filter(u -> u.getId().asString().equals(user) || ("<@" + u.getId().asString() + ">").equals(user) || ("<@!" + u.getId().asString() + ">").equals(user) || (u.getUsername() + "#" + u.getDiscriminator()).equals(user)).next();
    }

    /**
     * Gets the creation time base off the id
     * @param id The id
     * @return The time of creation
     */
    public static Instant getSnowflakeCreationDate(Long id){
        //return Instant.ofEpochMilli(Math.round((double)id / 4194304d + 1420070400000d));
        return Instant.ofEpochMilli(EntityUtil.DISCORD_EPOCH + (id >>> 22));
    }

    /**
     * Sends a {@link Message} to the given {@link TextChannel} that shows the usage of the {@link Command}
     * @param channel The {@link TextChannel}
     * @param cmd     The command
     * @param pref    The prefix of the guild
     * @param lang    The language of the guild
     */
    public static void sendHelpMessage(TextChannel channel, String cmd, String pref, String lang){
        //channel.createMessage(LocaleManager.getLanguageString(lang, cmd + ".help.content", pref)).subscribe();
        channel.createMessage(LocaleManager.getLanguageMessage(lang, "commands." + cmd + ".help", pref)).subscribe();
        //return channel.createMessage("Error in command " + cmd);
    }

    /**
     * Sends a {@link Message} to the given channel saying that an error occurred while performing the action
     * @param channel The {@link MessageChannel} the {@link Message} should be sent in
     */
    public static void sendErrorMessage(MessageChannel channel){
        channel.createMessage("An error occurred while performing this action.").subscribe();
    }

    /**
     * Sends a {@link Message} to the given channel saying the {@link Member} does not have the permissions to perform the {@link Command}
     * @param channel The {@link MessageChannel} the {@link Message} should get sent in
     */
    public static void sendNoPermissionsMessage(MessageChannel channel){
        channel.createMessage("You don't have the permissions to perform this action.").subscribe();
    }

    /**
     * Parses a {@link String} to the duration in milliseconds
     * @param duration The {@link String} that represents the duration
     * @return The duration in milliseconds or {@code -1} if the duration could not be parsed
     */
    public static Long getDuration(String duration){
        long d = 0L;
        Matcher m = Pattern.compile("^(?:(?:(\\d+):)?(?:(\\d+):))?(\\d+)(?:\\.(\\d+))?$").matcher(duration);
        if(m.matches()){
            if(m.group(1) != null) d += Long.parseLong(m.group(1)) * 3600000;
            if(m.group(2) != null) d += Long.parseLong(m.group(2)) * 60000;
            if(m.group(3) != null) d += Long.parseLong(m.group(3)) * 1000;
            if(m.group(4) != null) d += Long.parseLong(m.group(4));
        }else{
            m = Pattern.compile("(?: *(\\d+) *h| *(\\d+) *min| *(\\d+) *s| *(\\d+) *ms)+").matcher(duration);
            if(m.matches()){
                if(m.group(1) != null) d += Long.parseLong(m.group(1)) * 3600000;
                if(m.group(2) != null) d += Long.parseLong(m.group(2)) * 60000;
                if(m.group(3) != null) d += Long.parseLong(m.group(3)) * 1000;
                if(m.group(4) != null) d += Long.parseLong(m.group(4));
            }else{
                d = -1L;
            }
        }
        return d;
    }

    /**
     * Parses a {@link String} to the duration in seconds
     * @param duration The {@link String} that represents the duration
     * @return The duration in seconds or {@code -1} if the duration could not be parsed
     */
    public static Long getPollDuration(String duration){
        long d = 0L;
        Matcher m = Pattern.compile("^\\d+$").matcher(duration);
        if(m.matches())
            d = Long.parseLong(m.group(1));
        else{
            m = Pattern.compile("(?: *(\\d+) *d| *(\\d+) *h| *(\\d+) *min| *(\\d+) *s)+").matcher(duration);
            if(m.matches()){
                if(m.group(1) != null) d += Long.parseLong(m.group(1)) * 86400;
                if(m.group(2) != null) d += Long.parseLong(m.group(2)) * 3600;
                if(m.group(3) != null) d += Long.parseLong(m.group(3)) * 60;
                if(m.group(4) != null) d += Long.parseLong(m.group(4));
            }else{
                d = -1L;
            }
        }
        return d;
    }

    /**
     * Parses a duration in seconds to the {@link String} representation. Time units that have the value {@code 0} get ignored.<br/>
     * Example: 921658 will be parsed to {@code 10d 16h 58s}
     * @param duration The duration in milliseconds
     * @return The {@link String} representation
     */
    public static String getDuration(Long duration){
        long days = Math.floorDiv(duration, 86400);
        duration -= 86400 * days;
        long hours = Math.floorDiv(duration, 3600);
        duration -= 3600 * hours;
        long minutes = Math.floorDiv(duration, 60);
        duration -= 60 * minutes;
        long seconds = duration;
        return ((days > 0 ? days + "d " : "") + (hours > 0 ? hours + "h " : "") + (minutes > 0 ? minutes + "min " : "") + (seconds > 0 ? seconds + "s" : "")).trim();
    }

    /**
     * Replaces {@code {i}} for every {@code i} from {@code 0} to {@code args.length - 1} with the value in {@code args} at position {@code i}
     * @param input The string you want to format
     * @param args  The arguments you want to have as replaced text
     * @return The formatted {@link String}
     */
    public static String formatString(String input, String... args){
        for(int i = 0; i < args.length; i++)
            input = input.replace("{" + i + "}", args[i]);
        return input;
    }

    /**
     * Clamps a value
     * @param value The value you want to clamp
     * @param min   The minimal value
     * @param max   The maximal value
     * @return The clamped value
     */
    public static <T extends Comparable<T>> T clamp(T value, T min, T max){
        if(value.compareTo(min) < 0) return min;
        if(value.compareTo(max) > 0) return max;
        return value;
    }

    /**
     * The regular expression to match invites. Group 0 is the invite code without the discord url in front of it.
     */
    public static final String INVITE_MATCHER = "(?<=discord.gg\\/|discordapp.com\\/invite\\/)[a-zA-Z0-9-]{1,50}"; // [^1lIO0\W_]+

}