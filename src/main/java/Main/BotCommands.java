package Main;

import DataManager.*;
import DataManager.DataManager.*;
import Media.NSFWUtils;
import Media.SFWUtils;
import Media.UrbanDictionary;
import Music.MusicManager;
import Patreon.PatreonManager;
import com.fasterxml.jackson.databind.JsonNode;
import discord4j.core.object.entity.*;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.JDBCType;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BotCommands {

    /**
     * The id list of the guilds that have automatic backups enabled
     */
    public static List<Long> autoBackupGuilds = new ArrayList<>();

    /**
     * The list of all {@link Command}s. The key is an array of all valid command names, the first one is the main name. The value is the {@link Command}
     */
    public static final Map<String[], Command> commands = new HashMap<>();
    static {
        commands.put(new String[]{"ping"}, (e, pref, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> c.createMessage("pong"))
                .map(c -> true)
        );
        commands.put(new String[]{"test2"}, (e, pref, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> c.createMessage("Args: " + args.toString()))
                .map(c -> true)
        );
        commands.put(new String[]{"prefix", "pref"}, (e, pref, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "prefix", false, Permission.ADMINISTRATOR))
                        return BotUtils.sendNoPermissionsMessage(c);
                    if(args.size() < 2){
                        return Mono.just(false);
                    }
                    if(args.get(0).equalsIgnoreCase("set")){
                        String newPref = String.join(" ", args.subList(1, args.size())).trim();
                        if(newPref.length() == 0 || newPref.length() > 20){
                            return Mono.just(false);
                        }
                        if(DataManager.setGuild(e.getGuildId().get().asLong(), "bot_prefix", newPref, JDBCType.VARCHAR))
                            c.createMessage(LocaleManager.getLanguageMessage(lang, "commands.prefix.set", newPref)).subscribe();
                        else
                            return BotUtils.sendErrorMessage(c);
                        return Mono.just(true);
                    }
                    return Mono.just(false);
                })
        );
        commands.put(new String[]{"language", "lang"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c-> {
                    if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "language", false, Permission.ADMINISTRATOR))
                        return BotUtils.sendNoPermissionsMessage(c);
                    if(args.size() > 1){
                        return Mono.just(false);
                    }else if(args.isEmpty()){
                        try {
                            c.createMessage("Current language: " + lang).subscribe();
                        }catch (Exception ex){
                            BotUtils.sendErrorMessage(c);
                        }
                    }else{
                        if(DataManager.setGuild(e.getGuildId().get().asLong(), "language", args.get(0), JDBCType.VARCHAR))
                            c.createMessage("Language changed to " + args.get(0)).subscribe();
                        else
                            BotUtils.sendErrorMessage(c);
                    }
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"choose", "c"}, (e, pref, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "choose", true))
                        BotUtils.sendNoPermissionsMessage(c);
                    if(args.size() < 2)
                        return Mono.just(false);
                    Random rn = new Random();
                    c.createMessage(LocaleManager.getLanguageMessage(lang, "commands.choose.chosen", args.get(rn.nextInt(args.size())))).subscribe();
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"userlimit"}, (e, pref, args, lang) -> Mono.just(true));
        commands.put(new String[]{"token"}, (e, pref, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(e.getMember().get().getId().asLong() != e.getGuild().block().getOwnerId().asLong()) return BotUtils.sendNoPermissionsMessage(c);
                    if(args.size() != 1 && args.size() != 2)
                        return Mono.just(false);
                    if(args.get(0).equalsIgnoreCase("get") && args.size() == 1) {
                        try {
                            SQLGuild g = DataManager.getGuild(e.getGuildId().get().asLong());
                            e.getMessage().getAuthorAsMember().flatMap(u -> u.getPrivateChannel())
                                    .flatMap(pc -> pc.createMessage(LocaleManager.getLanguageMessage(lang, "commands.token.get.dm", g.getToken(), g.getReadonlyToken())))
                                    .subscribe();
                            c.createMessage(LocaleManager.getLanguageMessage(lang, "commands.token.get.guild")).subscribe();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            BotUtils.sendErrorMessage(c);
                        }
                        return Mono.just(true);
                    }else if((args.get(0).equalsIgnoreCase("new") || args.get(0).equalsIgnoreCase("renew")) && args.size() == 2){
                        String newToken = "";
                        boolean edit = false;
                        if(args.get(1).equalsIgnoreCase("edit")){
                            newToken = DataManager.renewToken(e.getGuildId().get().asLong());
                            edit = true;
                        }else if(args.get(1).equalsIgnoreCase("readonly")){
                            newToken = DataManager.renewReadonlyToken(e.getGuildId().get().asLong());
                        }
                        if(newToken == null)
                            BotUtils.sendErrorMessage(c);
                        else if(newToken.length() == 0)
                            return Mono.just(false);
                        else {
                            c.createMessage(LocaleManager.getLanguageMessage(lang, "commands.token.new." + (edit ? "edit" : "readonly") + ".guild")).subscribe();
                            Consumer<MessageCreateSpec> mcs = LocaleManager.getLanguageMessage(lang, "commands.token.new." + (edit ? "edit" : "readonly") + ".dm", newToken);
                            e.getMessage().getAuthorAsMember().flatMap(u -> u.getPrivateChannel())
                                    .flatMap(pc -> pc.createMessage(mcs))
                                    .subscribe();
                        }
                        return Mono.just(true);
                    }
                    return Mono.just(false);
                })
        );
        commands.put(new String[]{"weather"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(args.isEmpty())
                        return Mono.just(false);
                    String query = String.join(" ", args);
                    boolean metric = true;
                    JsonNode weather = Weather.getWeather(query, lang, metric);
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    if(weather == null || query.toLowerCase().contains("bielefeld")){
                        c.createMessage("Could not find a city with the name `" + query + "`").subscribe();
                    }else {
                        c.createMessage(mcs -> mcs.setEmbed(ecs -> {
                            ecs.setTitle(weather.get("name").asText() + ", " + weather.get("sys").get("country").asText());
                            ecs.setThumbnail(Weather.getImageUrl(weather.get("weather").get(0).get("icon").asText()));
                            ecs.setDescription(weather.get("weather").get(0).get("main").asText() + ": " + weather.get("weather").get(0).get("description").asText());
                            ecs.addField("Main", "Temperature: " + weather.get("main").get("temp").asDouble() + "°" + (metric ? "C" : "F") + "\n- min: " + weather.get("main").get("temp_min").asDouble() + "°" + (metric ? "C" : "F") + "\n- max: " + weather.get("main").get("temp_max").asDouble() + "°" + (metric ? "C" : "F") + "\nAir pressure: " + weather.get("main").get("pressure").asInt() + "hPa\nHumidity: " + weather.get("main").get("humidity").asInt() + "%", false);
                            ecs.addField("Sun", "Sunrise: " + format.format(new Date(weather.get("sys").get("sunrise").asLong() * 1000)) + "\nSunset: " + format.format(new Date(weather.get("sys").get("sunset").asLong() * 1000)), false);
                            ecs.addField("Clouds", "Cloudiness: " + weather.get("clouds").get("all").asInt() + "%", false);
                            ecs.addField("Wind", "Speed: " + weather.get("wind").get("speed").asDouble() + (metric ? "m/s" : "mph") + "\nAngle: " + weather.get("wind").get("deg").asInt() + "°" + (weather.get("wind").has("gust") ? "\nGust: " + weather.get("wind").get("gust").asDouble() + (metric ? "m/s" : "mph") : ""), false);
                            ecs.setFooter("Lon: " + weather.get("coord").get("lon").asDouble() + ", Lat: " + weather.get("coord").get("lat").asDouble(), null);
                        })).subscribe();
                    }
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"dynamicvoicechannel", "dvc"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "dynamicvoicechannel", false, Permission.MANAGE_CHANNELS))
                        return BotUtils.sendNoPermissionsMessage(c);
                    if(args.size() == 1){
                        if(args.get(0).equalsIgnoreCase("list")){
                            c.createMessage("" + DataManager.getDVCs(e.getGuildId().get().asLong())).subscribe();
                            return Mono.just(true);
                        }
                    }else if(args.size() > 1){
                        if(args.get(0).equalsIgnoreCase("add")){
                            String name = String.join(" ", args.subList(1, args.size()));
                            if(DataManager.isDVC(e.getGuildId().get().asLong(), name, null))
                                c.createMessage("`" + name + "` is already a dvc").subscribe();
                            else if(DataManager.addDVC(e.getGuildId().get().asLong(), name))
                                c.createMessage("`" + name + "` is now a dvc").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return Mono.just(true);
                        }else if(args.get(0).equalsIgnoreCase("remove")){
                            String name = String.join(" ", args.subList(1, args.size()));
                            if(!DataManager.isDVC(e.getGuildId().get().asLong(), name, null))
                                c.createMessage("`" + name + "` is not a dvc").subscribe();
                            else if(DataManager.removeDVC(e.getGuildId().get().asLong(), name))
                                c.createMessage("`" + name + "` is no longer a dvc").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return Mono.just(true);
                        }
                    }
                    return Mono.just(false);
                })
        );
        commands.put(new String[]{"poll"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "createPoll", true))
                        return BotUtils.sendNoPermissionsMessage(c);
                    if(args.size() < 4)
                        return Mono.just(false);
                    long duration = BotUtils.getPollDuration(args.get(0));
                    if(duration < 0)
                        return Mono.just(false);
                    args.remove(0);
                    AtomicBoolean multiVote = new AtomicBoolean(false);
                    List<ReactionEmoji> emojis = new ArrayList<>();
                    for(int i = 1; i < 10; i++) emojis.add(ReactionEmoji.unicode(i + "\u20E3"));
                    emojis.add(ReactionEmoji.unicode("\uD83D\uDD1F"));
                    if(args.get(0).equalsIgnoreCase("yn")){
                        if(args.size() > 5)
                            return Mono.just(false);
                        args.remove(0);
                        emojis.clear();
                        emojis.addAll(new ArrayList<>(Arrays.asList(ReactionEmoji.unicode("\u2705"), BotUtils.x, ReactionEmoji.unicode("*\u20E3"))));
                    }else if(args.get(0).equalsIgnoreCase("multi")){
                        multiVote.set(true);
                        args.remove(0);
                    }
                    String title = args.get(0);
                    args.remove(0);
                    String description = args.get(0);
                    args.remove(0);
                    if(args.size() > 20)
                        return Mono.just(false);
                    if(args.size() > 10){
                        emojis.clear();
                        for(int i = 0; i < 20; i++)
                            emojis.add(ReactionEmoji.unicode("\uD83C" + (char)(i + 56806)));
                    }

                    StringBuilder values = new StringBuilder();
                    for(int i = 0; i < args.size(); i++)
                        values.append("\n" + emojis.get(i).asUnicodeEmoji().get().getRaw() + " " + args.get(i));

                    /*Message m = c.createMessage(mcs -> mcs.setEmbed(ecs -> {
                        ecs.setTitle(BotUtils.formatString("**Poll** _({0})_", BotUtils.getDuration(duration)));
                        ecs.setDescription("**" + title.replace("*", "\\*") + "**" + values.toString());
                        ecs.setFooter(multiVote.get() ? "Multiple votes per user" : "One vote per user", null);
                        ecs.setTimestamp(Instant.now().plusSeconds(duration));
                    })).block();
                    for(int i = 0; i < args.size(); i++)
                        m.addReaction(emojis.get(i)).subscribe();*/
                    AtomicInteger cnt = new AtomicInteger(0);
                    c.createMessage(mcs ->
                            mcs.setEmbed(ecs -> {
                                ecs.setTitle(BotUtils.formatString("**Poll** _({0})_", BotUtils.getDuration(duration)));
                                if(description.replace("*", "\\*").length() + values.toString().length() <= 1024)
                                    ecs.addField(title, "**" + description.replace("*", "\\*") + "**" + values.toString(), false);
                                else
                                    ecs.setDescription("**" + title.replace("*", "\\*") + "**\n" + description + values.toString());
                                ecs.setFooter(multiVote.get() ? "Multiple votes per user" : "One vote per user", null);
                                ecs.setTimestamp(Instant.now().plusSeconds(duration));
                            }))
                            .flatMap(m -> Flux.fromIterable(emojis)
                                    .filter(emoji -> cnt.getAndIncrement() < args.size())
                                    .flatMap(emoji -> m.addReaction(emoji))
                                    .next()
                            )
                            .subscribe();
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"invites", "invite"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "blockInvites", false, Permission.MANAGE_MESSAGES))
                        return BotUtils.sendNoPermissionsMessage(c);
                    if(args.size() == 1){
                        if(args.get(0).equalsIgnoreCase("get")){
                            SQLGuild sg = DataManager.getGuild(e.getGuildId().get().asLong());
                            if(sg != null)
                                c.createMessage("delete invites: " + sg.getDeleteInvites() + "\nwarning: " + sg.getInviteWarning()).subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return Mono.just(true);
                        }else if(args.get(0).equalsIgnoreCase("whitelist")){
                            List<String> allowed = DataManager.getAllowedInvites(e.getGuildId().get().asLong());
                            c.createMessage("Allowed invites: " + allowed).subscribe();
                        }
                    }else if(args.size() == 2){
                        if(args.get(0).equalsIgnoreCase("delete")){
                            boolean block = Boolean.parseBoolean(args.get(1));
                            if(DataManager.setGuild(e.getGuildId().get().asLong(), "delete_invites", block, JDBCType.BOOLEAN)){
                                c.createMessage("set to " + block).subscribe();
                            }else{
                                BotUtils.sendErrorMessage(c);
                            }
                            return Mono.just(true);
                        }else if(args.get(0).equalsIgnoreCase("allow")){
                            String invite = args.get(1);
                            Matcher m = Pattern.compile(BotUtils.INVITE_MATCHER).matcher(invite);
                            if(m.find()) invite = m.group();
                            if(DataManager.isInviteAllowed(e.getGuildId().get().asLong(), invite))
                                c.createMessage("this invite is already allowed").subscribe();
                            else if(DataManager.allowInvite(e.getGuildId().get().asLong(), invite))
                                c.createMessage("invite " + invite + " is now allowed").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return Mono.just(true);
                        }else if(args.get(0).equalsIgnoreCase("disallow")){
                            String invite = args.get(1);
                            Matcher m = Pattern.compile(BotUtils.INVITE_MATCHER).matcher(invite);
                            if(m.find()) invite = m.group();
                            if(!DataManager.isInviteAllowed(e.getGuildId().get().asLong(), invite))
                                c.createMessage("this invite is not even allowed...").subscribe();
                            else if(DataManager.disallowInvite(e.getGuildId().get().asLong(), invite))
                                c.createMessage("invite " + invite + " is not allowed anymore").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return Mono.just(true);
                        }
                    }
                    if(args.size() > 1){
                        if(args.get(0).equalsIgnoreCase("warning")){
                            if(args.size() == 2 && (args.get(1).equalsIgnoreCase("disable") || args.get(1).equalsIgnoreCase("remove"))){
                                if(!DataManager.setGuild(e.getGuildId().get().asLong(), "invite_warning", "", JDBCType.VARCHAR))
                                    BotUtils.sendErrorMessage(c);
                                else
                                    c.createMessage("removed warning").subscribe();
                            }else if(args.size() > 2 && args.get(1).equalsIgnoreCase("set")){
                                String warning = String.join(" ", args.subList(2, args.size()));
                                if(!DataManager.setGuild(e.getGuildId().get().asLong(), "invite_warning", warning, JDBCType.VARCHAR))
                                    BotUtils.sendErrorMessage(c);
                                else
                                    c.createMessage("new warning: " + warning).subscribe();
                            }
                            return Mono.just(true);
                        }
                    }
                    return Mono.just(false);
                })
        );
        commands.put(new String[]{"permissions", "perms"}, (e, prefix, args, lang) -> Mono.just(false));
        commands.put(new String[]{"script", "scripts"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    /*if(args.size() == 1){
                        if(args.get(0).equalsIgnoreCase("upload") || args.get(0).equalsIgnoreCase("save")){
                            if(e.getMessage().getAttachments().size() == 1){
                                Attachment at = e.getMessage().getAttachments().iterator().next();
                                if(at.getFilename().endsWith(".lbs") && at.getFilename().contains("-")){
                                    List<String> tiles = new ArrayList<>(Arrays.asList(at.getFilename().split("-")));
                                    String eventType = tiles.get(0);
                                    String scriptName = String.join("-", tiles.subList(1, tiles.size()));
                                    ScriptExecutor.ScriptEvent event = ScriptExecutor.ScriptEvent.getEvent(scriptName);
                                    if(event != null){
                                        try {
                                            Connection con = Jsoup.connect(at.getUrl()).header("Authorization", "Bot " + Tokens.BOT_TOKEN).header("User-Agent", "l0c4lb0t (Discord4J) made by l0c4lh057 - Downloading script");
                                            Document doc = con.get();
                                            doc.outputSettings().prettyPrint(false);
                                            String scriptContent = doc.body().html();
                                            if(DataManager.addScript(e.getGuildId().get().asLong(), event, scriptName, scriptContent))
                                                c.createMessage("successfully uploaded script").subscribe();
                                            else
                                                return BotUtils.sendErrorMessage(c);
                                        }catch (IOException ex){
                                            ex.printStackTrace();
                                            return BotUtils.sendErrorMessage(c);
                                        }
                                        return Mono.just(true);
                                    }else{
                                        c.createMessage("unknown event " + eventType).subscribe();
                                        return Mono.just(true);
                                    }
                                }
                            }
                        }
                    }*/
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"command", "commands", "cmd", "cmds"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "customCommand", false, Permission.MANAGE_MESSAGES))
                        return BotUtils.sendNoPermissionsMessage(c);
                    if(args.size() == 1){
                        if(args.get(0).equalsIgnoreCase("list")){
                            Map<String, String> cc = DataManager.getCustomCommands(e.getGuildId().get().asLong());
                            if(cc.isEmpty())
                                c.createMessage("no custom commands here").subscribe();
                            else
                                c.createMessage("custom commands: " + String.join(", ", cc.keySet())).subscribe();
                            return Mono.just(true);
                        }
                    }else if(args.size() == 2){
                        if (args.get(0).equalsIgnoreCase("get")) {
                            String cmd = args.get(1);
                            Map<String, String> cc = DataManager.getCustomCommands(e.getGuildId().get().asLong());
                            if (cc.containsKey(cmd))
                                c.createMessage("Command: `" + cmd + "`\nResponse: ```\n" + cc.get(cmd) + "```").subscribe();
                            else
                                c.createMessage("command `" + cmd + "` does not exist").subscribe();
                            return Mono.just(true);
                        } else if (args.get(0).equalsIgnoreCase("remove") || args.get(0).equalsIgnoreCase("delete")) {
                            String cmd = args.get(1);
                            Map<String, String> cc = DataManager.getCustomCommands(e.getGuildId().get().asLong());
                            if (cc.containsKey(cmd)) {
                                if (DataManager.removeCustomCommand(e.getGuildId().get().asLong(), cmd))
                                    c.createMessage("removed custom command").subscribe();
                                else
                                    BotUtils.sendErrorMessage(c);
                            } else
                                c.createMessage("this custom command does not exist").subscribe();
                            return Mono.just(true);
                        }
                    }else if(args.size() > 2){
                        if(args.get(0).equalsIgnoreCase("add") || args.get(0).equalsIgnoreCase("create")){
                            String cmd = args.get(1);
                            String response = String.join(" ", args.subList(2, args.size()));
                            Map<String, String> cc = DataManager.getCustomCommands(e.getGuildId().get().asLong());
                            if(cc.containsKey(cmd))
                                c.createMessage("command already exists").subscribe();
                            else if(DataManager.addCustomCommand(e.getGuildId().get().asLong(), cmd, response))
                                c.createMessage("added custom command").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return Mono.just(true);
                        }
                    }
                    return Mono.just(false);
                })
        );
        commands.put(new String[]{"blockchannel", "bc"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "blockChannel", false, Permission.MANAGE_CHANNELS))
                        return BotUtils.sendNoPermissionsMessage(c);
                    if(args.size() == 1){
                        if(args.get(0).equalsIgnoreCase("list")){
                            List<Long> blocked = DataManager.getBlockedChannels(e.getGuildId().get().asLong());
                            c.createMessage("Blocked channels: " + blocked.stream().map(cId -> "<#" + cId + ">").collect(Collectors.joining(", "))).subscribe();
                            return Mono.just(true);
                        }
                    }else if(args.size() == 2){
                        if(args.get(0).equalsIgnoreCase("add")){
                            GuildMessageChannel bc = BotUtils.getChannelFromArgument(e.getGuild(), args.get(1)).ofType(GuildMessageChannel.class).block();
                            if(bc == null) return Mono.just(false);
                            List<Long> blocked = DataManager.getBlockedChannels(e.getGuildId().get().asLong());
                            if(blocked.contains(bc.getId().asLong()))
                                c.createMessage("already blocked").subscribe();
                            else if(DataManager.addBlockedChannel(e.getGuildId().get().asLong(), bc.getId().asLong()))
                                c.createMessage("blocked channel").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return Mono.just(true);
                        }else if(args.get(0).equalsIgnoreCase("remove")){
                            List<Long> blocked = DataManager.getBlockedChannels(e.getGuildId().get().asLong());
                            GuildMessageChannel bc = BotUtils.getChannelFromArgument(e.getGuild(), args.get(1)).ofType(GuildMessageChannel.class).block();
                            if(bc == null) return Mono.just(false);
                            if(!blocked.contains(bc.getId().asLong()))
                                c.createMessage("not blocked").subscribe();
                            else if(DataManager.removeBlockedChannel(e.getGuildId().get().asLong(), bc.getId().asLong()))
                                c.createMessage("unblocked channel").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return Mono.just(true);
                        }
                    }
                    return Mono.just(false);
                })
        );
        commands.put(new String[]{"unknowncommandmessage", "ucm"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "unknowncommandmessage", false, Permission.MANAGE_MESSAGES))
                        return BotUtils.sendNoPermissionsMessage(c);
                    if(args.size() == 1){
                        if(args.get(0).equalsIgnoreCase("get")){
                            String ucm = DataManager.getGuild(e.getGuildId().get().asLong()).getUnknownCommandMessage();
                            if(ucm.length() == 0)
                                c.createMessage("no ucm").subscribe();
                            else
                                c.createMessage("ucm: " + ucm).subscribe();
                            return Mono.just(true);
                        }else if(args.get(0).equalsIgnoreCase("remove") || args.get(0).equalsIgnoreCase("delete")){
                            if(DataManager.setGuild(e.getGuildId().get().asLong(), "unknown_command_message", "", JDBCType.VARCHAR))
                                c.createMessage("removed ucm").subscribe();
                            else
                                return BotUtils.sendErrorMessage(c);
                            return Mono.just(true);
                        }
                    }else if(args.size() > 1){
                        if(args.get(0).equalsIgnoreCase("set")){
                            String newUcm = String.join(" ", args.subList(1, args.size()));
                            if(DataManager.setGuild(e.getGuildId().get().asLong(), "unknown_command_message", newUcm, JDBCType.VARCHAR))
                                c.createMessage("Changed ucm to " + newUcm).subscribe();
                            else
                                return BotUtils.sendErrorMessage(c);
                            return Mono.just(true);
                        }
                    }
                    return Mono.just(false);
                })
        );
        commands.put(new String[]{"joinrole", "jr"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"joinmessage", "jm"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"leavemessage", "lm"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"banmessage", "bm"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"publicchannel", "pc"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"report"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"calc", "calculate", "math"}, (e, prefix, args, lang) -> Mono.just(true));

        /* GENERAL */

        commands.put(new String[]{"help"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    AtomicInteger pageNbr = new AtomicInteger(0);
                    if(args.size() > 0) try{
                        if(args.get(0).matches("^\\d+$"))
                            pageNbr.set(Integer.parseInt(args.get(0)) - 1);
                        else{
                            if(LocaleManager.getLanguageElement(lang, "commands").has(args.get(0))) {
                                BotUtils.sendHelpMessage(c, args.get(0), prefix, lang);
                                return Mono.just(true);
                            }
                            for(String[] aliases : commands.keySet())
                                for(String alias : aliases)
                                    if(alias.equalsIgnoreCase(args.get(0))){
                                        BotUtils.sendHelpMessage(c, aliases[0], prefix, lang);
                                        return Mono.just(true);
                                    }
                        }
                    }catch (NumberFormatException ex){}
                    Map<String, Map<String, String>> pages = BotUtils.getHelpPages(e.getGuild().block());
                    pageNbr.set(BotUtils.clamp(pageNbr.get(), 0, pages.size() - 1));
                    int currPage = -1;
                    for(String pageName : pages.keySet()){
                        currPage++;
                        if(pageNbr.get() != currPage) continue;
                        Map<String, String> page = pages.get(pageName);
                        c.createEmbed(ecs -> ecs
                                .setTitle(LocaleManager.getLanguageString(lang, "help.title"))
                                .addField(pageName, page.values().stream().map(cmd -> BotUtils.formatString(cmd, prefix)).collect(Collectors.joining("\n")), false)
                                .setFooter(LocaleManager.getLanguageString(lang, "help.footer", "" + (pageNbr.get()+1), "" + pages.size()), null)
                                .setColor(BotUtils.botColor)
                        ).flatMap(m -> Flux.fromIterable(new ArrayList<>(Arrays.asList(BotUtils.arrowLeft, BotUtils.arrowRight, BotUtils.x)))
                                .flatMap(emoji -> m.addReaction(emoji))
                                .next()
                        ).subscribe();
                        break;
                    }
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"about", "info", "l0c4lb0t"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"stats"}, (e, prefix, args, lang) -> Mono.just(true));

        /* MODERATION */

        commands.put(new String[]{"resetnicks", "resetnicknames", "nickreset"}, (e, pref, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "resetnicks", false, Permission.MANAGE_NICKNAMES))
                        return BotUtils.sendNoPermissionsMessage(c);
                    AtomicInteger cnt = new AtomicInteger(0);
                    e.getGuild().flatMap(g -> g.getMembers()
                            .filter(m -> m.getNickname().isPresent())
                            .doOnNext(m -> cnt.incrementAndGet())
                            .flatMap(m -> m.edit(gmes -> gmes.setNickname(null)))
                            .next()
                    ).subscribe();
                    c.createMessage("Reset " + cnt.get() + " nicknames").subscribe();
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"reactionroles", "reactionrole", "rr"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"ban"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"kick"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"anticaps", "nocaps"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "anticaps", false, Permission.MANAGE_MESSAGES))
                        return BotUtils.sendNoPermissionsMessage(c);
                    if(args.size() == 1){

                    }else if(args.size() == 2){

                    }else if(args.size() > 2){

                    }
                    return Mono.just(false);
                })
        );

        /* FUN */

        commands.put(new String[]{"minesweeper"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"randomnumber", "rn"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"remind"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"cat"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> c.createMessage(SFWUtils.getCat()))
                .map(x -> true)
        );
        commands.put(new String[]{"dog", "doggo", "goodboi"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> c.createMessage(SFWUtils.getDog()))
                .map(x -> true)
        );

        /* MUSIC COMMANDS */

        commands.put(new String[]{"join"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> Mono.justOrEmpty(e.getMember())
                        .flatMap(m -> m.getVoiceState()
                                .flatMap(vs -> vs.getChannel()
                                        .flatMap(vc -> vc.join(vcjs -> vcjs.setProvider(MusicManager.getProvider()))
                                                .flatMap(vcon -> MusicManager.setGuildConnection(e.getGuildId().get().asLong(), vcon))
                                        )
                                )
                        )
                )
                .map(x -> true)
        );
        commands.put(new String[]{"stop"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> Mono.justOrEmpty(MusicManager.getGuildConnection(e.getGuildId().get().asLong()))
                        .flatMap(vcon -> {
                            vcon.disconnect();
                            return Mono.just(vcon);
                        })
                )
                .map(x -> true)
        );
        commands.put(new String[]{"play"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"skip"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"search"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"queue"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"remove"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"nowplaying", "np"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"volume", "vol"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"pause"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"resume"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"seek"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"playlist", "playlists"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"loop"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"playingmessages", "playingmessage", "pm"}, (e, prefix, args, lang) -> Mono.just(true));

        /* BOT STAFF ONLY */

        commands.put(new String[]{"shutdown", "exit"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(!BotUtils.getBotAdmins().contains(e.getMember().get().getId().asLong()))
                        return BotUtils.sendNoPermissionsMessage(c);
                    e.getClient().logout().block();
                    System.exit(0);
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"getid"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"delpm"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"pcban"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"botban"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"guildban"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"disablepc"}, (e, prefix, args, lang) -> Mono.just(true));
        commands.put(new String[]{"bcall"}, (e, prefix, args, lang) -> Mono.just(true));



        commands.put(new String[]{"botsuggest", "botsuggestion", "botsuggestions"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> args.size() > 0)
                .flatMap(c -> {
                    if((args.size() == 1 || args.size() == 2) && args.get(0).equalsIgnoreCase("list")){
                        AtomicLong pageNumber = new AtomicLong(1);
                        long itemsPerPage = 5;
                        if(args.size() == 2){
                            try{
                                pageNumber.set(Long.parseLong(args.get(1)));
                                if(pageNumber.get() < 2)
                                    pageNumber.set(1);
                            }catch (NumberFormatException ex){}
                        }
                        long maxPageNumber = DataManager.getBotSuggestionPageCount(itemsPerPage);
                        pageNumber.set(BotUtils.clamp(pageNumber.get(), 1L, maxPageNumber));
                        List<SQLBotSuggestion> suggestions = DataManager.getBotSuggestions(pageNumber.get(), itemsPerPage);
                        String s = suggestions.stream().map(suggestion -> suggestion.getStatus().getEmoji().asUnicodeEmoji().get().getRaw() + " #" + suggestion.getId() + ": " + suggestion.getTitle()).collect(Collectors.joining("\n"));
                        c.createEmbed(ecs -> ecs
                                .setTitle("Bot Suggestions")
                                .setDescription(s)
                                .setFooter("Page " + pageNumber.get() + "/" + maxPageNumber, null)
                        ).flatMap(m -> Flux.fromIterable(new ArrayList<>(Arrays.asList(BotUtils.arrowLeft, BotUtils.arrowRight)))
                                .flatMap(emoji -> m.addReaction(emoji))
                                .then()
                        ).subscribe();
                        return Mono.just(true);
                    }else if(args.size() == 2 && args.get(0).equalsIgnoreCase("get")){
                        try {
                            int sId = Integer.parseInt(args.get(1));
                            SQLBotSuggestion suggestion = DataManager.getBotSuggestion(sId);
                            if(suggestion == null){
                                c.createMessage("Could not find a suggestion with the id " + sId).subscribe();
                                return Mono.just(true);
                            }
                            User u = e.getClient().getUserById(Snowflake.of(suggestion.getCreatorId())).block();
                            c.createEmbed(ecs -> ecs
                                    .setAuthor(u == null ? "user not found" : u.getUsername() + "#" + u.getDiscriminator(), null, u == null ? null : u.getAvatarUrl())
                                    .setTitle("Suggestion #" + sId + ": " + suggestion.getTitle())
                                    .setDescription(suggestion.getContent())
                                    .addField("Status: " + suggestion.getStatus().getName(), suggestion.getDetailedStatus().orElse("No description set"), false)
                                    .addField("Last update", BotUtils.getDuration(e.getMessage().getTimestamp().minusMillis(suggestion.getLastUpdate().toEpochMilli()).getEpochSecond()) + " ago", false)
                                    .setColor(suggestion.getStatus().getColor())
                                    .setFooter("Created at", null)
                                    .setTimestamp(suggestion.getCreatedAt())
                            ).subscribe();
                        }catch (NumberFormatException ex){
                            return Mono.just(false);
                        }
                        return Mono.just(true);
                    }else if(args.size() == 2 && args.get(0).equalsIgnoreCase("notify")){
                        // set notification status for given suggestion
                        try {
                            int sId = Integer.parseInt(args.get(1));
                            SQLBotSuggestion suggestion = DataManager.getBotSuggestion(sId);
                            if(suggestion == null){
                                c.createMessage("could not find a suggestion with the id " + sId).subscribe();
                                return Mono.just(true);
                            }
                            boolean notif = DataManager.getBotSuggestionNotifications(sId).contains(e.getMessage().getAuthor().get().getId().asLong());
                            if(DataManager.setBotSuggestionNotification(e.getMessage().getAuthor().get().getId().asLong(), sId, !notif))
                                c.createMessage(notif ? "You no longer get notified on suggestion " + sId : "You now get notified on suggestion " + sId).subscribe();
                            else
                                return BotUtils.sendErrorMessage(c);
                        }catch (NumberFormatException ex){
                            c.createMessage("no valid id").subscribe();
                            return Mono.just(true);
                        }
                        return Mono.just(true);
                    }else if(args.size() > 2 && (args.get(0).equalsIgnoreCase("add") || args.get(0).equalsIgnoreCase("create") || args.get(0).equalsIgnoreCase("suggest"))){
                        String title = args.get(1);
                        String content = String.join(" ", args.subList(2, args.size()));
                        int suggestionId = DataManager.addBotSuggestion(e.getMessage().getAuthor().get().getId().asLong(), title, content, e.getMessage().getTimestamp());
                        if(suggestionId > -1) {
                            DataManager.setBotSuggestionNotification(e.getMessage().getAuthor().get().getId().asLong(), suggestionId, true);
                            c.createMessage("created suggestion with id " + suggestionId).subscribe();
                            e.getClient().getChannelById(Snowflake.of(551801738223419392L)).ofType(GuildMessageChannel.class).flatMap(tc -> tc.createEmbed(ecs -> ecs
                                    .setTitle("New Suggestion #" + suggestionId)
                                    .addField(title, content, false)
                                    .setAuthor(e.getMessage().getAuthor().get().getUsername() + "#" + e.getMessage().getAuthor().get().getDiscriminator(), null, e.getMessage().getAuthor().get().getAvatarUrl())
                                    .setFooter("Created at", null)
                                    .setTimestamp(e.getMessage().getTimestamp())
                                    .setColor(BotUtils.botColor)
                            )).subscribe();
                        }else
                            return BotUtils.sendErrorMessage(c);
                        return Mono.just(true);
                    }else if(args.size() > 3 && args.get(0).equalsIgnoreCase("update")){
                        if(!BotUtils.getBotAdmins().contains(e.getMessage().getAuthor().get().getId().asLong()))
                            return BotUtils.sendNoPermissionsMessage(c);
                        try{
                            int sId = Integer.parseInt(args.get(1));
                            SQLBotSuggestion suggestion = DataManager.getBotSuggestion(sId);
                            if(suggestion == null){
                                c.createMessage("could not find suggestion with id " + sId).subscribe();
                                return Mono.just(true);
                            }
                            if(args.get(2).equalsIgnoreCase("title")){
                                String newTitle = String.join(" ", args.subList(3, args.size()));
                                if(DataManager.setBotSuggestion(sId, "title", newTitle, JDBCType.VARCHAR))
                                    c.createMessage("changed title of suggestion #" + sId + " to " + newTitle).subscribe();
                                else
                                    return BotUtils.sendErrorMessage(c);
                                return Mono.just(true);
                            }else if(args.get(2).equalsIgnoreCase("content") || args.get(2).equalsIgnoreCase("description")){
                                String newContent = String.join(" ", args.subList(3, args.size()));
                                if(DataManager.setBotSuggestion(sId, "content", newContent, JDBCType.VARCHAR))
                                    c.createMessage("changed description of suggestion #" + sId + " to " + newContent).subscribe();
                                else
                                    return BotUtils.sendErrorMessage(c);
                                return Mono.just(true);
                            }else if(args.size() > 3 && args.get(2).equalsIgnoreCase("status")){
                                SuggestionStatus newStatus = SuggestionStatus.getSuggestionStatus(args.get(3));
                                if(newStatus == null) return Mono.just(false);
                                String newDetailedStatus = args.size() == 4 ? null : String.join(" ", args.subList(4, args.size()));
                                if(DataManager.setBotSuggestionStatus(sId, newStatus.getStatus(), newDetailedStatus, e.getMessage().getTimestamp())) {
                                    c.createMessage("changed status of suggestion #" + sId + " to " + newStatus.getName() + ": " + newDetailedStatus).subscribe();
                                    Flux.fromIterable(DataManager.getBotSuggestionNotifications(sId))
                                            .flatMap(uId -> e.getClient().getUserById(Snowflake.of(uId)))
                                            .flatMap(u -> u.getPrivateChannel())
                                            .flatMap(pc -> pc.createEmbed(ecs -> ecs
                                                    .setTitle("Updated suggestion #" + sId + ": " + suggestion.getTitle())
                                                    .setDescription("**New status**: " + newStatus.getName() + "\n\n" + newDetailedStatus)
                                                    .setFooter("Updated", null)
                                                    .setTimestamp(e.getMessage().getTimestamp())
                                                    .setColor(BotUtils.botColor)
                                            ))
                                            .subscribe();
                                }else
                                    return BotUtils.sendErrorMessage(c);
                                return Mono.just(true);
                            }
                        }catch (NumberFormatException ex){}
                        return Mono.just(false);
                    }
                    return Mono.just(false);
                })
        );



        commands.put(new String[]{"feedback"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> args.size() > 0)
                .flatMap(c -> {
                    if((args.size() == 1 || args.size() == 2) && args.get(0).equalsIgnoreCase("list")){
                        AtomicLong pageNumber = new AtomicLong(1);
                        long itemsPerPage = 5;
                        if(args.size() == 2){
                            try{
                                pageNumber.set(Long.parseLong(args.get(1)));
                                if(pageNumber.get() < 2)
                                    pageNumber.set(1);
                            }catch (NumberFormatException ex){}
                        }
                        long maxPageNumber = DataManager.getSuggestionPageCount(e.getGuildId().get().asLong(), itemsPerPage);
                        pageNumber.set(BotUtils.clamp(pageNumber.get(), 1L, maxPageNumber));
                        List<SQLFeedback> suggestions = DataManager.getSuggestions(e.getGuildId().get().asLong(), pageNumber.get(), itemsPerPage);
                        String s = suggestions.stream().map(suggestion -> suggestion.getStatus().getEmoji().asUnicodeEmoji().get().getRaw() + " #" + suggestion.getId() + ": " + suggestion.getTitle()).collect(Collectors.joining("\n"));
                        c.createEmbed(ecs -> ecs
                                .setTitle("Feedback")
                                .setDescription(s)
                                .setFooter("Page " + pageNumber.get() + "/" + maxPageNumber, null)
                        ).flatMap(m -> Flux.fromIterable(new ArrayList<>(Arrays.asList(BotUtils.arrowLeft, BotUtils.arrowRight)))
                                .flatMap(emoji -> m.addReaction(emoji))
                                .then()
                        ).subscribe();
                        return Mono.just(true);
                    }else if(args.size() == 2 && args.get(0).equalsIgnoreCase("get")){
                        try {
                            int sId = Integer.parseInt(args.get(1));
                            SQLFeedback suggestion = DataManager.getSuggestion(e.getGuildId().get().asLong(), sId);
                            if(suggestion == null){
                                c.createMessage("Could not find feedback with the id " + sId).subscribe();
                                return Mono.just(true);
                            }
                            User u = e.getClient().getUserById(Snowflake.of(suggestion.getCreatorId())).block();
                            c.createEmbed(ecs -> ecs
                                    .setAuthor(u == null ? "user not found" : u.getUsername() + "#" + u.getDiscriminator(), null, u == null ? null : u.getAvatarUrl())
                                    .setTitle("Feedback #" + sId + ": " + suggestion.getTitle())
                                    .setDescription(suggestion.getContent())
                                    .addField("Type", suggestion.getType().getName(), false)
                                    .addField("Status: " + suggestion.getStatus().getName(), suggestion.getDetailedStatus().orElse("No description set"), false)
                                    .addField("Last update", BotUtils.getDuration(e.getMessage().getTimestamp().minusMillis(suggestion.getLastUpdate().toEpochMilli()).getEpochSecond()) + " ago", false)
                                    .setColor(suggestion.getStatus().getColor())
                                    .setFooter("Created at", null)
                                    .setTimestamp(suggestion.getCreatedAt())
                            ).subscribe();
                        }catch (NumberFormatException ex){
                            return Mono.just(false);
                        }
                        return Mono.just(true);
                    }else if(args.size() == 2 && args.get(0).equalsIgnoreCase("notify")){
                        // set notification status for given suggestion
                        try {
                            int sId = Integer.parseInt(args.get(1));
                            SQLFeedback suggestion = DataManager.getSuggestion(e.getGuildId().get().asLong(), sId);
                            if(suggestion == null){
                                c.createMessage("could not find a suggestion with the id " + sId).subscribe();
                                return Mono.just(true);
                            }
                            boolean notif = DataManager.getSuggestionNotifications(e.getGuildId().get().asLong(), sId).contains(e.getMessage().getAuthor().get().getId().asLong());
                            if(DataManager.setSuggestionNotification(e.getGuildId().get().asLong(), e.getMessage().getAuthor().get().getId().asLong(), sId, !notif))
                                c.createMessage(notif ? "You no longer get notified on suggestion " + sId : "You now get notified on suggestion " + sId).subscribe();
                            else
                                return BotUtils.sendErrorMessage(c);
                        }catch (NumberFormatException ex){
                            c.createMessage("no valid id").subscribe();
                            return Mono.just(true);
                        }
                        return Mono.just(true);
                    }else if(args.size() > 3 && (args.get(0).equalsIgnoreCase("add") || args.get(0).equalsIgnoreCase("create") || args.get(0).equalsIgnoreCase("suggest"))){
                        SQLFeedback.FeedbackType type = SQLFeedback.FeedbackType.getFeedbackType(args.get(1));
                        if(type == null) return Mono.just(false);
                        String title = args.get(2);
                        String content = String.join(" ", args.subList(3, args.size()));
                        SQLFeedback suggestion = DataManager.addSuggestion(e.getGuildId().get().asLong(), e.getMessage().getAuthor().get().getId().asLong(), title, content, e.getMessage().getTimestamp(), type);
                        if(suggestion != null) {
                            DataManager.setSuggestionNotification(e.getGuildId().get().asLong(), e.getMessage().getAuthor().get().getId().asLong(), suggestion.getId(), true);
                            c.createMessage("created suggestion with id " + suggestion.getId()).subscribe();
                            e.getClient().getChannelById(Snowflake.of(DataManager.getGuild(e.getGuildId().get().asLong()).getSuggestionChannelId())).ofType(GuildMessageChannel.class).flatMap(tc -> tc.createEmbed(ecs -> ecs
                                    .setTitle("New Suggestion #" + suggestion.getId())
                                    .addField(title, content, false)
                                    .setAuthor(e.getMessage().getAuthor().get().getUsername() + "#" + e.getMessage().getAuthor().get().getDiscriminator(), null, e.getMessage().getAuthor().get().getAvatarUrl())
                                    .setFooter("Created at", null)
                                    .setTimestamp(e.getMessage().getTimestamp())
                                    .setColor(BotUtils.botColor)
                            )).subscribe();
                        }else
                            return BotUtils.sendErrorMessage(c);
                        return Mono.just(true);
                    }else if(args.size() > 3 && args.get(0).equalsIgnoreCase("update")){
                        if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "updateSuggestions", false, Permission.MANAGE_GUILD))
                            return BotUtils.sendNoPermissionsMessage(c);
                        try{
                            int sId = Integer.parseInt(args.get(1));
                            SQLFeedback suggestion = DataManager.getSuggestion(e.getGuildId().get().asLong(), sId);
                            if(suggestion == null){
                                c.createMessage("could not find suggestion with id " + sId).subscribe();
                                return Mono.just(true);
                            }
                            if(args.get(2).equalsIgnoreCase("title")){
                                String newTitle = String.join(" ", args.subList(3, args.size()));
                                if(DataManager.setSuggestion(e.getGuildId().get().asLong(), sId, "title", newTitle, JDBCType.VARCHAR))
                                    c.createMessage("changed title of suggestion #" + sId + " to " + newTitle).subscribe();
                                else
                                    return BotUtils.sendErrorMessage(c);
                                return Mono.just(true);
                            }else if(args.get(2).equalsIgnoreCase("content") || args.get(2).equalsIgnoreCase("description")){
                                String newContent = String.join(" ", args.subList(3, args.size()));
                                if(DataManager.setSuggestion(e.getGuildId().get().asLong(), sId, "content", newContent, JDBCType.VARCHAR))
                                    c.createMessage("changed description of suggestion #" + sId + " to " + newContent).subscribe();
                                else
                                    return BotUtils.sendErrorMessage(c);
                                return Mono.just(true);
                            }else if(args.size() > 3 && args.get(2).equalsIgnoreCase("status")){
                                SuggestionStatus newStatus = SuggestionStatus.getSuggestionStatus(args.get(3));
                                if(newStatus == null) return Mono.just(false);
                                String newDetailedStatus = args.size() == 4 ? null : String.join(" ", args.subList(4, args.size()));
                                if(DataManager.setSuggestionStatus(e.getGuildId().get().asLong(), sId, newStatus.getStatus(), newDetailedStatus, e.getMessage().getTimestamp())) {
                                    c.createMessage("changed status of suggestion #" + sId + " to " + newStatus.getName() + ": " + newDetailedStatus).subscribe();
                                    Flux.fromIterable(DataManager.getSuggestionNotifications(e.getGuildId().get().asLong(), sId))
                                            .flatMap(uId -> e.getClient().getUserById(Snowflake.of(uId)))
                                            .flatMap(u -> u.getPrivateChannel())
                                            .flatMap(pc -> pc.createEmbed(ecs -> ecs
                                                    .setTitle("Updated suggestion #" + sId + ": " + suggestion.getTitle())
                                                    .setDescription("**New status**: " + newStatus.getName() + "\n\n" + newDetailedStatus)
                                                    .setFooter("Updated", null)
                                                    .setTimestamp(e.getMessage().getTimestamp())
                                                    .setColor(BotUtils.botColor)
                                            ))
                                            .subscribe();
                                }else
                                    return BotUtils.sendErrorMessage(c);
                                return Mono.just(true);
                            }
                        }catch (NumberFormatException ex){}
                        return Mono.just(false);
                    }
                    return Mono.just(false);
                })
        );

        /* NSFW */

        commands.put(new String[]{"boobs", "boob"}, (e, prefix, args, lang) -> e.getMessage().getChannel().ofType(GuildMessageChannel.class)
                .filter(BotUtils::checkChannelForNSFW)
                .flatMap(c -> c.createMessage(NSFWUtils.getBoobs()))
                .map(x -> true)
        );
        commands.put(new String[]{"ass", "arse"}, (e, prefix, args, lang) -> e.getMessage().getChannel().ofType(GuildMessageChannel.class)
                .filter(BotUtils::checkChannelForNSFW)
                .flatMap(c -> c.createMessage(NSFWUtils.getAss()))
                .map(x -> true)
        );
        commands.put(new String[]{"asian"}, (e, prefix, args, lang) -> e.getMessage().getChannel().ofType(GuildMessageChannel.class)
                .filter(BotUtils::checkChannelForNSFW)
                .flatMap(c -> c.createMessage(NSFWUtils.getAsian()))
                .map(x -> true)
        );

        commands.put(new String[]{"pussy"}, (e, prefix, args, lang) -> e.getMessage().getChannel().ofType(GuildMessageChannel.class)
                .filter(BotUtils::checkChannelForNSFW)
                .flatMap(c -> c.createMessage(SFWUtils.getCat()))
                .map(x -> true)
        );
        commands.put(new String[]{"cock"}, (e, prefix, args, lang) -> e.getMessage().getChannel().ofType(GuildMessageChannel.class)
                .filter(BotUtils::checkChannelForNSFW)
                .flatMap(c -> c.createMessage(SFWUtils.getCock()))
                .map(x -> true)
        );

        commands.put(new String[]{"backup", "backups"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> args.size() > 0)
                .flatMap(c -> {
                    if(args.size() == 2 && (args.get(0).equalsIgnoreCase("auutomated") || args.get(0).equalsIgnoreCase("automation") || args.get(0).equalsIgnoreCase("automate") || args.get(0).equalsIgnoreCase("automatic"))) {
                        if(PatreonManager.isPatronGuild(e.getGuildId().get().asLong())){
                            if(args.get(1).equalsIgnoreCase("enable")){
                                if(autoBackupGuilds.contains(e.getGuildId().get().asLong())){
                                    // already enabled
                                }else{
                                    autoBackupGuilds.add(e.getGuildId().get().asLong());
                                    // added
                                }
                            }else if(args.get(1).equalsIgnoreCase("disable")){
                                if(autoBackupGuilds.remove(e.getGuildId().get().asLong())){
                                    // removed
                                }else{
                                    // was not enabled
                                }
                            }else if(args.get(1).equalsIgnoreCase("get")){
                                // tell whether is is enabled or not
                                boolean enabled = autoBackupGuilds.contains(e.getGuildId().get().asLong());
                            }
                        }else{
                            // can't use automated backups
                        }
                    }
                    if(args.size() > 1) {
                        if (args.get(0).equalsIgnoreCase("create")) {
                            String bId = String.join(" ", args.subList(1, args.size()));
                            if(DataManager.guildBackupExists(e.getGuildId().get().asLong(), bId)){
                                // already exists
                                return Mono.just(true);
                            }
                            boolean isPatronGuild = PatreonManager.isPatronGuild(e.getGuildId().get().asLong());
                            int backupCount = DataManager.getGuildBackupCount(e.getGuildId().get().asLong(), false);
                            if(backupCount > 2 && !isPatronGuild){
                                // max 3 non patreon backups
                                return Mono.just(true);
                            }else if(backupCount > 14){
                                // max 15 patreon backups
                                return Mono.just(true);
                            }
                            if(!DataManager.createGuildBackup(e.getGuild().block(), args.get(1), false))
                                return BotUtils.sendErrorMessage(c);
                            // backup created
                            return Mono.just(true);
                        }else if (args.get(0).equalsIgnoreCase("restore")) {
                            DataManager.restoreGuildBackup(e.getGuild().block(), args.get(1));
                        }else if(args.get(0).equalsIgnoreCase("info") || args.get(0).equalsIgnoreCase("information")){

                        }
                    }
                    return Mono.just(false);
                })
        );

        commands.put(new String[]{"urbandictionary", "urban", "define"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .filter(c -> BotUtils.checkChannelForNSFW(c))
                .filter(c -> !args.isEmpty())
                .flatMap(c -> {
                    UrbanDictionary dictionary = new UrbanDictionary(String.join(" ", args));
                    UrbanDictionary.UrbanDefinition definition = dictionary.getRandomDefinition();
                    if(definition == null){
                        c.createMessage("not found").subscribe();
                        return Mono.just(true);
                    }
                    c.createEmbed(ecs -> ecs
                            .setAuthor(definition.getAuthorName(), definition.getAuthorUrl(), null)
                            .setTimestamp(definition.getTime())
                            .setFooter("Thumbs up: " + definition.getUpvotes() + ", Thumbs down: " + definition.getDownvotes(), null)
                            .setUrl(definition.getUrl())
                            .setDescription(definition.getFormattedDefinition())
                            .addField("Example", definition.getFormattedExample(), false)
                            .setTitle("Definition of \"" + definition.getWord() + "\"")
                    ).subscribe();
                    return Mono.just(true);
                })
        );

    }

}