package Main;

import DataManager.DataManager;
import DataManager.SQLGuild;
import com.fasterxml.jackson.databind.JsonNode;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Permission;
import reactor.core.publisher.Mono;

import java.sql.JDBCType;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotCommands {

    /**
     * The list of all {@link Command}s. The key is an array of all valid command names, the first one is the main name. The value is the {@link Command}
     */
    public static final Map<String[], Command> commands = new HashMap<>();
    static {
        commands.put(new String[]{"ping"}, (e, pref, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> c.createMessage("pong"))
                .flatMap(c -> Mono.just(true))
        );
        commands.put(new String[]{"test2"}, (e, pref, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> c.createMessage("Args: " + args.toString()))
                .flatMap(c -> Mono.just(true))
        );
        commands.put(new String[]{"prefix", "pref"}, (e, pref, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(args.size() != 1){
                        return Mono.just(false);
                    }
                    String newPref = args.get(0);
                    if(newPref.length() == 0 || newPref.length() > 10){
                        return Mono.just(false);
                    }
                    if(DataManager.setGuild(e.getGuildId().get().asLong(), "bot_prefix", args.get(0), JDBCType.VARCHAR))
                        c.createMessage("Changed prefix to `" + newPref + "`").subscribe();
                    else
                        BotUtils.sendErrorMessage(c);
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"language", "lang"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c-> {
                    if(args.size() > 1){
                        return Mono.just(false);
                    }else if(args.isEmpty()){
                        try {
                            c.createMessage("Current language: " + DataManager.getGuild(e.getGuildId().get().asLong()).getLanguage()).subscribe();
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
                    if(args.size() < 2)
                        return Mono.just(false);
                    Random rn = new Random();
                    c.createMessage("I choose `" + args.get(rn.nextInt(args.size())) + "`").subscribe();
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"userlimit"}, (e, pref, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"resetnicks"}, (e, pref, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> Mono.just(e.getMember().get())
                        .flatMap(u -> {
                            int cnt = 0;
                            for(Member m : e.getGuild().block().getMembers().toIterable()){
                                if(m.getNickname().isPresent()){
                                    cnt++;
                                    m.edit(gmes -> gmes.setNickname(null)).subscribe();
                                }
                            }
                            c.createMessage("Reset " + cnt + " nicknames").subscribe();
                            return Mono.just(true);
                        })
                )
        );
        commands.put(new String[]{"token"}, (e, pref, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(e.getMessage().getAuthorAsMember().block().getId().asLong() == e.getGuild().block().getOwnerId().asLong()){
                        if(args.size() != 1)
                            return Mono.just(false);
                        if(args.get(0).equals("get")) {
                            try {
                                String token = DataManager.getGuild(e.getGuildId().get().asLong()).getToken();
                                e.getMessage().getAuthorAsMember().block().getPrivateChannel().block().createMessage("Your token for the website is `" + token + "`").subscribe();
                                c.createMessage("I've sent you the token via DM").subscribe();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                BotUtils.sendErrorMessage(c);
                            }
                            return Mono.just(true);
                        }else if(args.get(0).equals("new")){
                            String newToken = DataManager.renewToken(e.getGuildId().get().asLong());
                            if(newToken.length() == 0)
                                BotUtils.sendErrorMessage(c);
                            else {
                                c.createMessage("I've sent you the new token via DM").subscribe();
                                e.getMessage().getAuthorAsMember().block().getPrivateChannel().block().createMessage("Your new token for the website is `" + newToken + "`").subscribe();
                            }
                            return Mono.just(true);
                        }
                    }else{
                        BotUtils.sendNoPermissionsMessage(c);
                    }
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"weather"}, (e, prefix, args, lang) -> {
            if(args.isEmpty())
                return Mono.just(false);
            String query = String.join(" ", args);
            String language = "en";
            boolean metric = true;
            try{language = DataManager.getGuild(e.getGuildId().get().asLong()).getLanguage();}catch (Exception ex){}
            JsonNode weather = Weather.getWeather(query, language, metric);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if(weather == null || query.toLowerCase().contains("bielefeld")){
                e.getMessage().getChannel().block().createMessage("Could not find a city with the name `" + query + "`").subscribe();
            }else {
                e.getMessage().getChannel().block().createMessage(mcs -> mcs.setEmbed(ecs -> {
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
        });
        commands.put(new String[]{"dynamicvoicechannel", "dvc"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(args.size() == 1){
                        if(args.get(0).equalsIgnoreCase("list")){
                            c.createMessage("" + DataManager.getDVCs(e.getGuildId().get().asLong())).subscribe();
                            return Mono.just(true);
                        }
                    }else if(args.size() > 1){
                        if(args.get(0).equalsIgnoreCase("add")){
                            String name = String.join(" ", args.subList(1, args.size()));
                            if(DataManager.isDVC(e.getGuildId().get().asLong(), name))
                                c.createMessage("`" + name + "` is already a dvc").subscribe();
                            else if(DataManager.addDVC(e.getGuildId().get().asLong(), name))
                                c.createMessage("`" + name + "` is now a dvc").subscribe();
                            else
                                BotUtils.sendErrorMessage(c);
                            return Mono.just(true);
                        }else if(args.get(0).equalsIgnoreCase("remove")){
                            String name = String.join(" ", args.subList(1, args.size()));
                            if(!DataManager.isDVC(e.getGuildId().get().asLong(), name))
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
                    if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "createPoll", true)){
                        BotUtils.sendNoPermissionsMessage(c);
                        return Mono.just(true);
                    }
                    if(args.size() < 3)
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
                        emojis = new ArrayList<>(Arrays.asList(ReactionEmoji.unicode("\u2705"), ReactionEmoji.unicode("\u274C"), ReactionEmoji.unicode("*\u20E3")));
                    }else if(args.get(0).equalsIgnoreCase("multi")){
                        multiVote.set(true);
                        args.remove(0);
                    }
                    String title = args.get(0);
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

                    Message m = c.createMessage(mcs -> mcs.setEmbed(ecs -> {
                        ecs.setTitle(BotUtils.formatString("**Poll** _({0})_", BotUtils.getDuration(duration)));
                        ecs.setDescription("**" + title.replace("*", "\\*") + "**" + values.toString());
                        ecs.setFooter(multiVote.get() ? "Multiple votes per user" : "One vote per user", null);
                        ecs.setTimestamp(Instant.now().plusSeconds(duration));
                    })).block();
                    for(int i = 0; i < args.size(); i++)
                        m.addReaction(emojis.get(i)).subscribe();
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"invites"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(!PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "blockInvites", false, Permission.MANAGE_MESSAGES)){
                        BotUtils.sendNoPermissionsMessage(c);
                        return Mono.just(true);
                    }
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
        commands.put(new String[]{"permissions", "perms"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {

                    return Mono.just(false);
                })
        );
        commands.put(new String[]{"script", "scripts"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(args.size() == 1){

                    }
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"command", "commands", "cmd", "cmds"}, (e, prefix, args, lang) -> e.getMessage().getChannel()
                .flatMap(c -> {
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
                    if(args.size() == 1){
                        if(args.get(0).equalsIgnoreCase("list")){
                            List<Long> blocked = DataManager.getBlockedChannels(e.getGuildId().get().asLong());

                            return Mono.just(true);
                        }
                    }else if(args.size() == 2){
                        if(args.get(0).equalsIgnoreCase("add")){

                            return Mono.just(true);
                        }else if(args.get(0).equalsIgnoreCase("remove")){

                            return Mono.just(true);
                        }
                    }
                    return Mono.just(false);
                })
        );
    }

}