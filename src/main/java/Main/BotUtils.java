package Main;

import DataManager.DataManager;
import DataManager.SQLGuild;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.*;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.core.util.EntityUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BotUtils {

    /**
     * Gets the {@link DiscordClient} by the token
     *
     * @param token The bot token
     * @return The {@link DiscordClient}
     */
    public static DiscordClient getClient(String token){
        return new DiscordClientBuilder(token).build();
    }

    /**
     * Gets all bot administrators
     *
     * @return A {@link List} of all bot administrators
     */
    public static List<Long> getBotAdmins(){return Arrays.asList(226677096091484160L);}

    /**
     * The main color of the bot
     */
    public static final Color botColor = new Color(124, 124, 255);

    public static final Color darkRed = new Color(0xFF0000);

    public static final Color lighterRed = new Color(0xFF5246);
    /**
     * Discord user of l0c4lh057
     */
    public static User l0c4lh057 = null;

    /**
     * Arrow left emoji
     */
    public static final ReactionEmoji arrowLeft = ReactionEmoji.unicode("\u2B05");
    /**
     * Arrow right emoji
     */
    public static final ReactionEmoji arrowRight = ReactionEmoji.unicode("\u27A1");
    /**
     * X emoji
     */
    public static final ReactionEmoji x = ReactionEmoji.unicode("\u274C");

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
     * Splits the content at spaces except they are in quotes. Both single and double quotes work, the beginning quote has to be the same as the ending quote.<br/>
     * To use quotes inside of a quoted text it has to be escaped ({@code \"} or {@code \'}).
     * Double quotes don't have to be escaped inside of single quotes and single quotes don't have to be escaped inside of double quotes.<br/>
     * Every {@code \} gets removed, to write one it has to be escaped too ({@code \\}). {@code \\"} will not escape the quote but result in the backslash character and a quote behind it.
     *
     * @param content The content you want to parse
     * @return The content split into arguments
     */
    public static Mono<List<String>> messageToArgs(String content){
        List<String> args = new ArrayList<>();
        boolean escaped = false;
        boolean inQuotes = false;
        char quoteChar = '-';
        boolean endedQuote = false;
        String currArg = "";
        for (char c : content.toCharArray()) {
            if (endedQuote) {
                endedQuote = false;
                if (c == ' ') continue;
            }
            if (c == ' ' && !inQuotes) {
                args.add(currArg);
                currArg = "";
                escaped = false;
                continue;
            }
            if (c == quoteChar && c != '-' && inQuotes && !escaped) {
                args.add(currArg);
                currArg = "";
                inQuotes = false;
                endedQuote = true;
                quoteChar = '-';
                continue;
            }
            if (c == '\\' && !escaped) {
                escaped = true;
                continue;
            }
            if ((c == '"' || c == '\'') && !escaped && !inQuotes) {
                if(currArg.length() > 0) args.add(currArg);
                currArg = "";
                inQuotes = true;
                quoteChar = c;
                continue;
            }
            escaped = false;
            currArg += "" + c;
        }
        if (!endedQuote) args.add(currArg);
        return Mono.just(args);
    }

    /**
     * Parses a {@link String} into the single arguments. Arguments are wrapped in quotes and separated by commas, both double and single quotes can be used.
     * Between the end or beginning of the argument and the comma there can any number of spaces.<br/>
     * To use quotes in an argument it has to be escaped by a backslash. Single quotes can only be escaped in arguments wrapped in single quotes, the same applies to double quotes.
     * To use backslashes they also have to be escaped with an backslash, new lines can be achieved by escaping an {@code n} character.<br/>
     * Example:<br/>
     * {@code "this is an argument", 'the second argument', "argument can contain quotes \", backslashes \\ and line breaks \n."}
     *
     * @param content
     * @return The parameters
     */
    public static List<String> contentToParameters(String content){
        List<String> args = new ArrayList<>();
        boolean escaped = false;
        boolean inQuotes = false;
        char quoteChar = '-';
        boolean afterComma = true;
        String currArg = "";
        for (char c : content.toCharArray()) {
            if(inQuotes){
                if(c == '\\' && !escaped){
                    escaped = true;
                    continue;
                }else if(c == quoteChar && !escaped){
                    args.add(currArg);
                    currArg = "";
                    quoteChar = '-';
                    afterComma = false;
                    inQuotes = false;
                    continue;
                }
                if(escaped && c == 'n')
                    currArg += '\n';
                else if(escaped)
                    // fuck you user this is invalid and i have to deal with it because you are too dumb
                    currArg += "\\" + c;
                else
                    currArg += c;
                escaped = false;
            }else{
                if(c == ' '){
                    continue;
                }else if(c == ',' && !afterComma){
                    afterComma = true;
                    continue;
                }else if((c == '"' || c == '\'') && afterComma){
                    quoteChar = c;
                    inQuotes = true;
                    continue;
                }
                return null;
            }
        }
        if(inQuotes) return null;
        return args;
    }

    /**
     * Returns whether the {@link String} is the command or not
     *
     * @param content The message content you want to check
     * @param cmd     The array of {@link String}s that are valid names for command you want to test for
     * @param prefix  The prefix the message should start with
     * @return Whether the {@link String} is the given command
     */
    public static boolean isCommand(String content, String[] cmd, String prefix){
        prefix = escapeRegex(prefix);
        String bId = BotMain.client.getSelfId().get().asString();
        return Pattern.compile("^(?:" + prefix + "|\\<@\\!?" + bId + "\\> ?)(?i)(?:" + String.join("|", cmd) + ")(?-i)(?: (.*))?$", Pattern.DOTALL).matcher(content).matches();
    }

    /**
     * Truncates a {@link String}
     *
     * @param content The message content you want to truncate
     * @param cmd     The array of {@link String}s that are valid names for the command
     * @param prefix  The prefix
     * @return The truncated {@link String}
     */
    public static Mono<String> truncateMessage(String content, String[] cmd, String prefix){
        prefix = escapeRegex(prefix);
        String bId = BotMain.client.getSelfId().get().asString();
        Matcher matcher = Pattern.compile("^(?:" + prefix + "|\\<@\\!?" + bId + "\\> ?)(?i)(?:" + String.join("|", cmd) + ")(?-i)(?: (.*))?$", Pattern.DOTALL).matcher(content);
        if(matcher.matches())
            return Mono.just(matcher.group(1) == null ? "" : matcher.group(1));
        else
            return Mono.empty();
    }

    /**
     * Escapes all RegEx characters in a {@link String}
     *
     * @param text The {@link String} you want to escape
     * @return The escaped {@link String}
     */
    public static String escapeRegex(String text){
        return text.replaceAll("[\\<\\(\\[\\{\\\\\\^\\-\\=\\$\\!\\|\\]\\}\\)\\?\\*\\+\\.\\>]", "\\\\$0");
    }

    /**
     * Gets the {@link Color} by a {@link String}<br/>
     * This can be a {@link String} like {@code #rrggbb}, {@code #rgb}, {@code 1234567} or {@code RED}
     *
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
     *
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
     *
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
                            ecs.setAuthor(message.get("author").asText(), message.has("authorUrl") ? message.get("authorUrl").asText() : null, message.has("authorIcon") ? message.get("authorIcon").asText() : null);
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
     *
     * @param guild The {@link Guild} the {@link Role} is in
     * @param role  The {@link String} representation of the {@link Role}
     * @return The {@link Role}
     */
    public static Mono<Role> getRoleFromArgument(Guild guild, String role){
        return guild.getRoles().filter(r -> r.getId().asString().equals(role) || r.getMention().equals(role) || r.getName().equals(role)).next();
    }
    /**
     * Gets a {@link Role} by a {@link String}
     *
     * @param guild The {@link Mono}<{@link Guild}> the {@link Role} is in
     * @param role  The {@link String} representation of the {@link Role}
     * @return The {@link Role}
     */
    public static Mono<Role> getRoleFromArgument(Mono<Guild> guild, String role){
        return guild.flatMap(g -> g.getRoles().filter(r -> r.getId().asString().equals(role) || r.getMention().equals(role) || r.getName().equals(role)).next());
    }

    /**
     * Gets a {@link Member} by a {@link String}
     *
     * @param guild  The {@link Guild} the {@link Member} is in
     * @param member The {@link String} representation of the {@link Member}
     * @return The {@link Member}
     */
    public static Mono<Member> getMemberFromArgument(Guild guild, String member){
        return guild.getMembers().filter(m -> m.getId().asString().equals(member) || ("<@" + m.getId().asString() + ">").equals(member) || ("<@!" + m.getId().asString() + ">").equals(member) || (m.getUsername() + "#" + m.getDiscriminator()).equals(member)).next();
    }
    /**
     * Gets a {@link Member} by a {@link String}
     *
     * @param guild  The {@link Mono}<{@link Guild}> the {@link Member} is in
     * @param member The {@link String} representation of the {@link Member}
     * @return The {@link Member}
     */
    public static Mono<Member> getMemberFromArgument(Mono<Guild> guild, String member){
        return guild.flatMap(g -> g.getMembers().filter(m -> m.getId().asString().equals(member) || ("<@" + m.getId().asString() + ">").equals(member) || ("<@!" + m.getId().asString() + ">").equals(member) || (m.getUsername() + "#" + m.getDiscriminator()).equals(member)).next());
    }

    /**
     * Gets a {@link GuildChannel} by a {@link String}
     *
     * @param guild   The {@link Guild} the {@link GuildChannel} is in
     * @param channel The {@link String} representation of the {@link GuildChannel}
     * @return The {@link GuildChannel}
     */
    public static Mono<GuildChannel> getChannelFromArgument(Guild guild, String channel){
        return guild.getChannels().filter(c -> c.getId().asString().equals(channel) || c.getMention().equals(channel) || c.getName().equals(channel)).next();
    }
    /**
     * Gets a {@link GuildChannel} by a {@link String}
     *
     * @param guild   The {@link Mono}<{@link Guild}> the {@link GuildChannel} is in
     * @param channel The {@link String} representation of the {@link GuildChannel}
     * @return The {@link GuildChannel}
     */
    public static Mono<GuildChannel> getChannelFromArgument(Mono<Guild> guild, String channel){
        return guild.flatMap(g -> g.getChannels().filter(c -> c.getId().asString().equals(channel) || c.getMention().equals(channel) || c.getName().equals(channel)).next());
    }

    /**
     * Gets a {@link User} by a {@link String}
     *
     * @param user The {@link String} representation of the {@link User}
     * @return The {@link User}
     */
    public static Mono<User> getUserFromArgument(String user){
        return BotMain.client.getUsers().filter(u -> u.getId().asString().equals(user) || ("<@" + u.getId().asString() + ">").equals(user) || ("<@!" + u.getId().asString() + ">").equals(user) || (u.getUsername() + "#" + u.getDiscriminator()).equals(user)).next()
                .switchIfEmpty(Mono.just(user)
                        .flatMap(u -> {
                            if(u.matches("^\\d+$"))
                                return BotMain.client.getUserById(Snowflake.of(u)).onErrorResume(err -> Mono.empty());
                            else if(u.matches("^<@\\d+>$"))
                                return BotMain.client.getUserById(Snowflake.of(u.substring(2, u.length() - 1))).onErrorResume(err -> Mono.empty());
                            else if(u.matches("^<@!\\d+>$"))
                                return BotMain.client.getUserById(Snowflake.of(u.substring(3, u.length() - 1))).onErrorResume(err -> Mono.empty());
                            return Mono.empty();
                        })
                );
    }

    /**
     * Gets the creation time base off the id
     *
     * @param id The id
     * @return The time of creation
     */
    public static Instant getSnowflakeCreationDate(Long id){
        //return Instant.ofEpochMilli(Math.round((double)id / 4194304d + 1420070400000d));
        return Instant.ofEpochMilli(EntityUtil.DISCORD_EPOCH + (id >>> 22));
    }

    /**
     * Sends a {@link Message} to the given {@link MessageChannel} that shows the usage of the {@link Command}
     *
     * @param channel The {@link MessageChannel}
     * @param cmd     The command
     * @param pref    The prefix of the guild
     * @param lang    The language of the guild
     */
    public static void sendHelpMessage(MessageChannel channel, String cmd, String pref, String lang){
        channel.createMessage(mcs -> mcs
                .setEmbed(ecs -> ecs
                        .setDescription(LocaleManager.getLanguageString(lang, "commands." + cmd + ".help", pref))
                        .setTitle(LocaleManager.getLanguageString(lang, "commandErrorTitle", cmd))
                        .setColor(darkRed)
                )
        ).subscribe();
    }

    /**
     * Sends a {@link Message} to the given channel saying that an error occurred while performing the action
     *
     * @param channel The {@link MessageChannel} the {@link Message} should be sent in
     * @return A {@link Mono} with the value true
     */
    public static Mono<Boolean> sendErrorMessage(MessageChannel channel){
        channel.createMessage("An error occurred while performing this action.").subscribe();
        return Mono.just(true);
    }

    /**
     * Sends a {@link Message} to the given channel saying the {@link Member} does not have the permissions to perform the {@link Command}
     *
     * @param channel The {@link MessageChannel} the {@link Message} should get sent in
     * @return A {@link Mono} with the value true
     */
    public static Mono<Boolean> sendNoPermissionsMessage(MessageChannel channel){
        channel.createMessage("You don't have the permissions to perform this action.").subscribe();
        return Mono.just(true);
    }

    /**
     * Parses a {@link String} to the duration in milliseconds
     *
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
     *
     * @param duration The {@link String} that represents the duration
     * @return The duration in seconds or {@code -1} if the duration could not be parsed
     */
    public static Long getPollDuration(String duration){
        long d = 0L;
        Matcher m = Pattern.compile("^(\\d+)$").matcher(duration);
        if(m.matches())
            d = Long.parseLong(m.group(1)) * 60;
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
     *
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
     *
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
     *
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

    /**
     * Merges both lists into one list
     *
     * @param a The first list
     * @param b The second list
     * @return The added lists
     */
    public static <T extends Object> List<T> addLists(List<T> a, List<T> b){
        List<T> newList = new ArrayList<>();
        newList.addAll(a);
        newList.addAll(b);
        return newList;
    }

    /**
     * Gives you the page number the item is displayed on.
     *
     * @param itemIndex    The index of the item (first item has index 1)
     * @param itemsPerPage The number of items which should be displayed on each page
     * @return The page number
     */
    public static long getPageNumber(long itemIndex, long itemsPerPage){
        return Math.round(Math.ceil((double)itemIndex / (double)itemsPerPage));
    }

    /**
     * Tells you if the message is a suggestion message
     *
     * @param message The message you want to check
     * @return {@code true} if the message is a suggestion message, otherwise {@code false}
     */
    public static boolean isBotSuggestionMessage(Message message){
        if(message.getAuthor().isEmpty()) return false;
        if(message.getAuthor().get().getId().asLong() != message.getClient().getSelfId().get().asLong()) return false;
        if(message.getEmbeds().size() != 1) return false;
        Embed embed = message.getEmbeds().get(0);
        if(!embed.getTitle().orElse("").equals("Bot Suggestions")) return false;
        if(embed.getFooter().isEmpty()) return false;
        if(!embed.getFooter().get().getText().matches("^Page \\d+/\\d+$")) return false;
        return true;
    }

    /**
     * Gets the number the suggestion message currently shows
     *
     * @param message The message to get the page number from
     * @return The page number (starting from 1) or {@code -1} when the message is no suggestion message
     */
    public static int getSuggestionPageNumber(Message message){
        if(message.getAuthor().isEmpty()) return -1;
        if(message.getAuthor().get().getId().asLong() != message.getClient().getSelfId().get().asLong()) return -1;
        if(message.getEmbeds().size() != 1) return -1;
        Embed embed = message.getEmbeds().get(0);
        if(!embed.getTitle().orElse("").equals("Suggestions")) return -1;
        if(embed.getFooter().isEmpty()) return -1;
        Matcher m = Pattern.compile("Page (\\d+)/\\d+").matcher(embed.getFooter().get().getText());
        if(m.matches())
            return Integer.parseInt(m.group(1));
        return -1;
    }

    /**
     * Gets the page the help message is currently showing
     *
     * @param message The message
     * @return The page number (starting from 0) the message is showing or {@code -1} if it is no help message
     */
    public static int getHelpPageNumber(Message message){
        if(message.getAuthor().isEmpty()) return -1;
        if(message.getAuthor().get().getId().asLong() != message.getClient().getSelfId().get().asLong()) return -1;
        if(message.getEmbeds().size() != 1) return -1;
        Embed embed = message.getEmbeds().get(0);
        String lang = LocaleManager.getGuildLanguage(message.getGuild().block());
        if(!embed.getTitle().orElse("").equals(LocaleManager.getLanguageString(lang,"help.title"))) return -1;
        if(embed.getFooter().isEmpty()) return -1;
        Matcher m = Pattern.compile(LocaleManager.getLanguageString(lang, "help.footer", "(\\d+)", "\\d+")).matcher(embed.getFooter().get().getText());
        if(m.matches())
            return Integer.parseInt(m.group(1))-1;
        return -1;
    }

    /**
     * Returns all help pages including the list of custom commands for the guild
     *
     * @param guild The guild
     * @return All help pages
     */
    public static Map<String, Map<String, String>> getHelpPages(@Nullable Guild guild){
        String lang = LocaleManager.getGuildLanguage(guild);
        Map<String, Map<String, String>> pages = new ObjectMapper().convertValue(LocaleManager.getLanguageElement(lang, "help.pages"), new TypeReference<Map<String, Map<String, String>>>(){});
        Map<String, String> customCommands = guild == null ? new HashMap<>() : DataManager.getCustomCommands(guild.getId().asLong());
        Set<String> cmds = customCommands.keySet();
        cmds.forEach(cmd -> customCommands.put(cmd, "`{0}" + cmd + "`"));
        if(customCommands.size() > 0) pages.put(LocaleManager.getLanguageString(lang, "help.customCommands"), customCommands);
        return pages;
    }

    /**
     * Checks if the channel is an NSFW channel. If it is marked as NSFW it return true, otherwise it sends a message to the channel, that an NSFW channel is required and false is returned.
     *
     * @param channel The channel you want to check
     * @return Whether the channel is marked as NSFW or not
     */
    public static boolean checkChannelForNSFW(MessageChannel channel){
        if(channel instanceof PrivateChannel)
            return true;
        if(!(channel instanceof GuildMessageChannel))
            return false;
        GuildMessageChannel c = (GuildMessageChannel)channel;
        if(c.isNsfw())
            return true;
        JsonNode node = LocaleManager.getLanguageElement(LocaleManager.getGuildLanguage(c.getGuildId().asLong()), "nsfwChannelRequired");
        c.createEmbed(ecs -> ecs
                .setTitle(node.get("title").asText())
                .setDescription(node.get("content").asText())
                .setColor(lighterRed)
        ).subscribe();
        return false;
    }

    public static boolean checkMemberForNSFWPerms(Guild g, Member m, GuildMessageChannel c){
        if(PermissionManager.hasPermission(g, m, "nsfw", true))
            return true;
        sendNoPermissionsMessage(c);
        return false;
    }

    public static boolean checkMemberForPerms(Guild g, Member m, GuildMessageChannel c, String cmd, boolean everyone, Permission... defaultPerms){
        if(PermissionManager.hasPermission(g, m, cmd, everyone, defaultPerms))
            return true;
        sendNoPermissionsMessage(c);
        return false;
    }

    /**
     * Checks if the bot has certain {@link Permission}s in the {@link Guild}. If it has all of the permissions {@code true} is returned, otherwise a message telling which permissions are missing is sent and {@code false} is returned.
     *
     * @param messageChannel The channel to send the message in
     * @param permissions    The needed permissions
     * @return True if the bot has all the permissions or false if it doesn't
     */
    public static Mono<Boolean> checkForPermissions(GuildMessageChannel messageChannel, Permission... permissions){
        return messageChannel.getGuild().flatMap(g -> g.getMemberById(messageChannel.getClient().getSelfId().get())).flatMap(m -> m.getBasePermissions().flatMap(perms -> {
            if(perms.containsAll(PermissionSet.of(permissions)))
                return Mono.just(true);
            messageChannel.getEffectivePermissions(messageChannel.getClient().getSelfId().get())
                    .filter(p -> perms.contains(Permission.SEND_MESSAGES))
                    .flatMap(p -> messageChannel.createMessage("Sorry, but I need following permissions to perform this action: " + Arrays.stream(permissions).map(perm -> perm.name()).collect(Collectors.joining(", "))))
                    .subscribe();
            return Mono.just(false);
        }));
    }

    /**
     * Checks if the bot has certain {@link Permission}s in the {@link Guild}.
     *
     * @param g           The guild
     * @param permissions The needed permissions
     * @return True if the bot has all permissions or false if it doesn't
     */
    public static Mono<Boolean> checkForPermissions(Guild g, Permission... permissions){
        return g.getMemberById(g.getClient().getSelfId().get()).flatMap(m -> m.getBasePermissions().flatMap(perms -> {
            if(perms.containsAll(PermissionSet.of(permissions)))
                return Mono.just(true);
            return Mono.just(false);
        }));
    }

    /**
     * Checks if the bot has certain {@link Permission}s in the {@link GuildMessageChannel}. If it has all of the permissions {@code true} is returned, otherwise a message telling which permissions are needed is sent and {@code false} is returned.
     *
     * @param messageChannel The channel to check the permissions and to send the message in
     * @param permissions    The needed permissions
     * @return True if the bot has all the permissions or false if it doesn't
     */
    public static Mono<Boolean> checkForChannelPermissions(GuildMessageChannel messageChannel, Permission... permissions){
        return checkForChannelPermissions(messageChannel, messageChannel, permissions);
    }

    /**
     * Checks if the bot has certain {@link Permission}s in the {@link GuildChannel}. If it has all of the permissions {@code true} is returned, otherwise a message telling which permissions are needed is sent to the {@link GuildMessageChannel} and {@code false} is returned.
     *
     * @param checkChannel   The channel to check the permissions in
     * @param messageChannel The channel to send the message in if some permission is missing
     * @param permissions    The needed permissions
     * @return
     */
    public static Mono<Boolean> checkForChannelPermissions(GuildChannel checkChannel, GuildMessageChannel messageChannel, Permission... permissions){
        return checkChannel.getEffectivePermissions(checkChannel.getClient().getSelfId().get()).flatMap(perms -> {
            if(perms.containsAll(PermissionSet.of(permissions)))
                return Mono.just(true);
            if(messageChannel != null)
                if(perms.contains(Permission.SEND_MESSAGES))
                    messageChannel.createMessage("Sorry, but I need following permissions in this channel to perform this action: " + Arrays.stream(permissions).map(p -> p.name()).collect(Collectors.joining(", "))).subscribe();
            return Mono.just(false);
        });
    }

    public static void startAutoBackups(DiscordClient client){
        Flux.interval(Duration.between(Instant.now(), getTomorrow()), Duration.of(1, ChronoUnit.DAYS))
                .flatMap(nbr -> Flux.fromIterable(BotCommands.autoBackupGuilds))
                .flatMap(gId -> client.getGuildById(Snowflake.of(gId)))
                .doOnNext(g -> {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
                    String bId = "Automated backup " + formatter.format(Instant.now()) + " " + createRandomString(6);
                    DataManager.createGuildBackup(g, bId, true);
                })
                .subscribe();
    }

    /**
     * Get tomorrows date at 00:00:00 UTC
     *
     * @return Tomorrows date
     */
    public static Instant getTomorrow(){
        return LocalDateTime.now().atZone(ZoneOffset.UTC).plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant();
    }

    /**
     * Creates a random string with a certain length containing only a-z, A-Z and 0-9
     *
     * @param length The length of the string
     * @return The random string
     */
    public static String createRandomString(int length){
        String available = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder sb = new StringBuilder();
        Random rn = new Random();
        while(length-- > 0)
            sb.append(available.charAt(rn.nextInt(available.length())));
        return sb.toString();
    }

}