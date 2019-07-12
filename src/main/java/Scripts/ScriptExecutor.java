package Scripts;

import Main.BotMain;
import Main.BotUtils;
import Main.LocaleManager;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.*;
import discord4j.core.object.util.Image;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import discord4j.core.util.EntityUtil;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.mariuszgromada.math.mxparser.Expression;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.awt.*;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptExecutor {

    public enum ScriptEvent {
        onMessage("onMessage"),
        onJoin("onJoin"),
        onLeave("onLeave"),
        onBan("onBan"),
        onUnban("onUnban"),
        onPin("onPin"),
        onUnpin("onUnpin"),
        onNick("onNick"),
        onVoiceJoin("onVoiceJoin"),
        onVoiceMove("onVoiceMove"),
        onVoiceLeave("onVoiceLeave"),
        onReactionAdded("onReactionAdded"),
        onReactionRemoved("onReactionRemoved"),
        onRoleAdded("onRoleAdded"),
        onRoleRemoved("onRoleRemoved"),
        onRoleCreated("onRoleCreated"),
        onChannelCreated("onChannelCreated"),
        onCategoryCreated("onCategoryCreated"),
        onVoiceChannelCreated("onVoiceChannelCreated"),
        onTextChannelCreated("onTextChannelCreated"),
        onRoleDeleted("onRoleDeleted"),
        onChannelDeleted("onChannelDeleted"),
        onCategoryDeleted("onCategoryDeleted"),
        onVoiceChannelDeleted("onVoiceChannelDeleted"),
        onTextChannelDeleted("onTextChannelDeleted"),
        onCommand("onCommand"),
        onCustomCommand("onCustomCommand"),
        onUnknownCommand("onUnknownCommand");

        private String event;
        ScriptEvent(String event){
            this.event = event;
        }
        public String getEventName(){
            return event;
        }
        public static ScriptEvent getEvent(String name){
            for(ScriptEvent ev : ScriptEvent.values())
                if(ev.getEventName().equalsIgnoreCase(name))
                    return ev;
            return null;
        }
    }

    private static HashMap<String, Action> actions = new HashMap<>();
    static {
        actions.put("sendMessage", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.createMessage(BotUtils.jsonToMessage(args.get(1)))
                                .filter(m -> varName != null)
                                .flatMap(m -> Mono.justOrEmpty(variables.put(varName, m.getId().asString())))
                        )
                ).map(x -> true)
        );
        actions.put("editMessage", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 3)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(args.get(1)))
                                .flatMap(m -> m.edit(BotUtils.jsonToMessageEdit(args.get(2))))
                        )
                )
                .map(x -> true)
        );
        actions.put("deleteMessage", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(args.get(1)))
                                .flatMap(m -> m.delete("A script ran the delete command on this message."))
                        )
                )
                .map(x -> true)
        );
        actions.put("sendDM", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getMemberFromArgument(g, args.get(0))
                        .flatMap(m -> m.getPrivateChannel()
                                .flatMap(c -> c.createMessage(BotUtils.jsonToMessage(args.get(1))))
                        )
                )
                .map(x -> true)
        );
        actions.put("giveRole", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getMemberFromArgument(g, args.get(0))
                        .flatMap(m -> BotUtils.getRoleFromArgument(g, args.get(1))
                                .flatMap(r -> m.addRole(r.getId(), "Role added by the giveRole function in a script"))
                        )
                )
                .map(x -> true)
        );
        actions.put("removeRole", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getMemberFromArgument(g, args.get(0))
                        .flatMap(m -> BotUtils.getRoleFromArgument(g, args.get(1))
                                .flatMap(r -> m.removeRole(r.getId(), "Role removed by the removeRole function in a script"))
                        )
                )
                .map(x -> true)
        );
        actions.put("setNickname", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getMemberFromArgument(g, args.get(0))
                        .flatMap(m -> m.edit(gmes -> gmes.setNickname(args.get(1).length() > 32 ? args.get(1).substring(0, 32) : args.get(1))))
                )
                .map(x -> true)
        );
        actions.put("resetNickname", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 1)
                .flatMap(x -> BotUtils.getMemberFromArgument(g, args.get(0))
                        .flatMap(m -> m.edit(gmes -> gmes.setNickname(null)))
                )
                .map(x -> true)
        );
        actions.put("kick", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 1 || args.size() == 2)
                .flatMap(x -> BotUtils.getMemberFromArgument(g, args.get(0))
                        .flatMap(m -> m.kick(args.size() == 2 ? args.get(1) : null))
                )
                .map(x -> true)
        );
        actions.put("ban", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 1 || args.size() == 2)
                .flatMap(x -> BotUtils.getUserFromArgument(args.get(0)))
                .map(x -> true)
        );
        actions.put("unban", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 1 || args.size() == 2)
                .flatMap(x -> BotUtils.getUserFromArgument(args.get(0))
                        .flatMap(u -> g.unban(u.getId(), args.size() == 2 ? args.get(1) : null))
                )
                .map(x -> true)
        );
        actions.put("clearReactions", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(args.get(1)))
                                .flatMap(m -> m.removeAllReactions())
                        )
                )
                .map(x -> true)
        );
        actions.put("createRole", (g, args, variables, varName) -> Mono.just(g)
                .flatMap(x -> g.createRole(rcs -> {
                    if(args.size() > 0) rcs.setName(args.get(0));
                    if(args.size() > 1) rcs.setColor(BotUtils.getColor(args.get(1), Color.BLACK));
                    if(args.size() > 2) rcs.setHoist(Boolean.parseBoolean(args.get(2)));
                    if(args.size() > 3) rcs.setMentionable(Boolean.parseBoolean(args.get(3)));
                    if(args.size() > 4){
                        List<Permission> perms = new ArrayList<>();
                        if(args.size() == 5 && args.get(4).matches("^\\d+$")){
                            rcs.setPermissions(PermissionSet.of(Long.parseLong(args.get(4))));
                        }else {
                            for (String s : args.subList(4, args.size())) {
                                if (s.matches("^\\d+$")) {
                                    for (Permission p : Permission.values())
                                        if (p.getValue() == Long.parseLong(s)) {
                                            perms.add(p);
                                            break;
                                        }
                                } else
                                    try {
                                        perms.add(Permission.valueOf(s.toUpperCase()));
                                    } catch (IllegalArgumentException ex) {
                                        ex.printStackTrace();
                                    }
                            }
                            rcs.setPermissions(PermissionSet.of(perms.toArray(new Permission[0])));
                        }
                    }
                    rcs.setReason("Role created by the createRole function in a script");
                }))
                .filter(r -> varName != null)
                .flatMap(r -> Mono.justOrEmpty(variables.put(varName, r.getId().asString())))
                .map(x -> true)
        );
        actions.put("deleteRole", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 1)
                .flatMap(x -> BotUtils.getRoleFromArgument(g, args.get(0)))
                .filter(r -> !r.isManaged())
                .flatMap(r -> r.delete("Deleted by the deleteRole function in a script"))
                .map(x -> true)
        );
        actions.put("createTextChannel", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("createVoiceChannel", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("createCategory", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("deleteChannel", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("moveChannel", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("joinVoiceChannel", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("leaveVoiceChannel", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("moveUser", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("clearQueue", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("playSong", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("pauseSong", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("continueSong", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("skipSong", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("enqueueSong", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("pinMessage", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class))
                .flatMap(c -> c.getMessageById(Snowflake.of(args.get(1))))
                .flatMap(m -> m.pin())
                .map(x -> true)
        );
        actions.put("unpinMessage", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class))
                .flatMap(c -> c.getMessageById(Snowflake.of(args.get(1))))
                .flatMap(m -> m.unpin())
                .map(x -> true)
        );




        actions.put("match", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> {
                    Matcher m = Pattern.compile(args.get(1)).matcher(args.get(0));
                    if(m.matches()){
                        for(int i = 0; i < m.groupCount(); i++)
                            variables.put("group" + i, m.group(i) == null ? "" : m.group(i));
                    }
                    if(varName != null) variables.put(varName, "" + m.matches());
                    return Mono.just(true);
                })
        );



        actions.put("continueIfEquals", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .map(x -> args.get(0).equals(args.get(1)))
        );
        actions.put("breakIfEquals", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .map(x -> !args.get(0).equals(args.get(1)))
        );
        actions.put("continueIfEqualsIgnoreCase", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .map(x -> args.get(0).equalsIgnoreCase(args.get(1)))
        );
        actions.put("breakIfEqualsIgnoreCase", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .map(x -> !args.get(0).equalsIgnoreCase(args.get(1)))
        );
        actions.put("continueIfStartsWith", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .map(x -> args.get(0).startsWith(args.get(1)))
        );
        actions.put("breakIfStartsWith", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .map(x -> !args.get(0).startsWith(args.get(1)))
        );
        actions.put("continueIfEndsWith", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .map(x -> args.get(0).endsWith(args.get(1)))
        );
        actions.put("breakIfEndsWith", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .map(x -> !args.get(0).endsWith(args.get(1)))
        );
        actions.put("continueIfContains", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .map(x -> args.get(0).contains(args.get(1)))
        );
        actions.put("breakIfContains", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .map(x -> !args.get(0).contains(args.get(1)))
        );
        actions.put("continueIfMatches", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .map(x -> Pattern.matches(args.get(1), args.get(0)))
        );
        actions.put("breakIfMatches", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .map(x -> !Pattern.matches(args.get(1), args.get(0)))
        );
        actions.put("continueIfMentions", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 3)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(args.get(1)))
                                .flatMap(m -> {
                                    if(args.get(1).equals("everyone")) return Mono.just(m.mentionsEveryone());
                                    try {
                                        Snowflake sf = Snowflake.of(args.get(2));
                                        return Mono.just(
                                                m.getRoleMentionIds().contains(sf) ||
                                                m.getUserMentionIds().contains(sf)
                                        );
                                    } catch (Exception e) {
                                        return Mono.just(false);
                                    }
                                })
                        )
                )
        );
        actions.put("breakIfMentions", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 3)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(args.get(1)))
                                .flatMap(m -> {
                                    if(args.get(1).equals("everyone")) return Mono.just(!m.mentionsEveryone());
                                    try {
                                        Snowflake sf = Snowflake.of(args.get(2));
                                        return Mono.just(!(
                                                m.getRoleMentionIds().contains(sf) ||
                                                m.getUserMentionIds().contains(sf)
                                        ));
                                    } catch (Exception e) {
                                        return Mono.just(true);
                                    }
                                })
                        )
                )
        );
        actions.put("continueIfHasAttachment", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(args.get(1)))
                                .map(m -> !m.getAttachments().isEmpty())
                        )
                )
        );
        actions.put("breakIfHasAttachment", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(args.get(1)))
                                .map(m -> m.getAttachments().isEmpty())
                        )
                )
        );
        actions.put("continueIfHasEmbed", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(args.get(1)))
                                .map(m -> !m.getEmbeds().isEmpty())
                        )
                )
        );
        actions.put("breakIfHasEmbed", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(args.get(1)))
                                .map(m -> m.getEmbeds().isEmpty())
                        )
                )
        );
        actions.put("continueIfHasRole", (g, args, variables, varName) -> Mono.just(true));
        actions.put("breakIfHasRole", (g, args, variables, varName) -> Mono.just(true));
        actions.put("continueIfIsInVoiceChannel", (g, args, variables, varName) -> Mono.just(g)
                .flatMap(x -> {
                    try {
                        if(args.size() == 1){
                            Member m = BotUtils.getMemberFromArgument(g, args.get(0)).block();
                            VoiceState vs = m.getVoiceState().block();
                            if(vs == null) return Mono.just(false);
                            return Mono.just(vs.getChannelId().isPresent());
                        }else if(args.size() == 2){
                            Member m = BotUtils.getMemberFromArgument(g, args.get(0)).block();
                            VoiceState vs = m.getVoiceState().block();
                            if(vs == null) return Mono.just(false);
                            Optional<Snowflake> cId = vs.getChannelId();
                            if(!cId.isPresent()) return Mono.just(false);
                            GuildChannel c = BotUtils.getChannelFromArgument(g, args.get(1)).block();
                            if(c == null) return Mono.just(false);
                            return Mono.just(cId.get().asLong() == c.getId().asLong());
                        }
                    }catch (Exception ex){}
                    return Mono.just(true);
                })
        );
        actions.put("breakIfIsInVoiceChannel", (g, args, variables, varName) -> Mono.just(g)
                .flatMap(x -> {
                    try {
                        if(args.size() == 1){
                            Member m = BotUtils.getMemberFromArgument(g, args.get(0)).block();
                            VoiceState vs = m.getVoiceState().block();
                            if(vs == null) return Mono.just(true);
                            return Mono.just(!vs.getChannelId().isPresent());
                        }else if(args.size() == 2){
                            Member m = BotUtils.getMemberFromArgument(g, args.get(0)).block();
                            VoiceState vs = m.getVoiceState().block();
                            if(vs == null) return Mono.just(true);
                            Optional<Snowflake> cId = vs.getChannelId();
                            if(!cId.isPresent()) return Mono.just(true);
                            GuildChannel c = BotUtils.getChannelFromArgument(g, args.get(1)).block();
                            if(c == null) return Mono.just(true);
                            return Mono.just(cId.get().asLong() != c.getId().asLong());
                        }
                    }catch (Exception ex){}
                    return Mono.just(true);
                })
        );
        actions.put("continueIfGreaterThan", (g, args, variables, varName) -> Mono.just(true));
        actions.put("breakIfGreaterThan", (g, args, variables, varName) -> Mono.just(true));
        actions.put("continueIfLessThan", (g, args, variables, varName) -> Mono.just(true));
        actions.put("breakIfLessThan", (g, args, variables, varName) -> Mono.just(true));





        actions.put("equals", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + args.get(0).equals(args.get(1)))))
                .map(x -> true)
        );
        actions.put("equalsIgnoreCase", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + args.get(0).equalsIgnoreCase(args.get(1)))))
                .map(x -> true)
        );
        actions.put("startsWith", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + args.get(0).startsWith(args.get(1)))))
                .map(x -> true)
        );
        actions.put("endsWith", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + args.get(0).endsWith(args.get(1)))))
                .map(x -> true)
        );
        actions.put("contains", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + args.get(0).contains(args.get(1)))))
                .map(x -> true)
        );
        actions.put("matches", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + args.get(0).matches(args.get(1)))))
                .map(x -> true)
        );
        actions.put("mentions", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 3)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(args.get(1)))
                                .flatMap(m -> {
                                    if(args.get(1).equals("everyone")) return Mono.justOrEmpty(variables.put(varName, "" + m.mentionsEveryone()));
                                    try {
                                        Snowflake sf = Snowflake.of(args.get(2));
                                        variables.put(varName, "" + (
                                                m.getRoleMentionIds().contains(sf) ||
                                                m.getUserMentionIds().contains(sf)
                                        ));
                                    } catch (Exception e) {
                                    }
                                    return Mono.just(true);
                                })
                        )
                )
                .map(x -> true)
        );
        actions.put("hasAttachment", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(args.get(1)))
                                .flatMap(m -> Mono.justOrEmpty(variables.put(varName, "" + !m.getAttachments().isEmpty())))
                        )
                )
                .map(x -> true)
        );
        actions.put("hasEmbed", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(args.get(1)))
                                .flatMap(m -> Mono.justOrEmpty(variables.put(varName, "" + !m.getEmbeds().isEmpty())))
                        )
                )
                .map(x -> true)
        );
        actions.put("isInVoiceChannel", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 1 || args.size() == 2)
                .flatMap(x -> {
                    try {
                        if(args.size() == 1){
                            Member m = BotUtils.getMemberFromArgument(g, args.get(0)).block();
                            VoiceState vs = m.getVoiceState().block();
                            if(vs == null) variables.put(varName, "" + false);
                            else variables.put(varName, "" + vs.getChannelId().isPresent());
                        }else if(args.size() == 2){
                            Member m = BotUtils.getMemberFromArgument(g, args.get(0)).block();
                            VoiceState vs = m.getVoiceState().block();
                            if(vs == null) variables.put(varName, "" + false);
                            else {
                                Optional<Snowflake> cid = m.getVoiceState().block().getChannelId();
                                if (!cid.isPresent()) variables.put(varName, "" + false);
                                else{
                                    GuildChannel c = BotUtils.getChannelFromArgument(g, args.get(1)).block();
                                    if(c == null) return Mono.just(false);
                                    variables.put(varName, "" + (cid.get().asLong() == c.getId().asLong()));
                                }
                            }
                        }
                    }catch (Exception ex){}
                    return Mono.just(true);
                })
        );
        actions.put("isNicked", (g, args, variables, varName) -> Mono.just(true));
        actions.put("getNickname", (g, args, variables, varName) -> Mono.just(true));
        actions.put("hasDiscordPermission", (g, args, variables, varName) -> Mono.just(true));
        actions.put("hasBotPermission", (g, args, variables, varName) -> Mono.just(true));


        actions.put("greaterThan", (g, args, variables, varName) -> Mono.just(true));
        actions.put("lessThan", (g, args, variables, varName) -> Mono.just(true));


        actions.put("upperCase", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 1)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, args.get(1).toUpperCase())))
                .map(x -> true)
        );
        actions.put("lowerCase", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 1)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, args.get(1).toLowerCase())))
                .map(x -> true)
        );
        actions.put("substring", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2 || args.size() == 3)
                .flatMap(x -> {
                    try {
                        String s = args.get(0);
                        if(args.size() == 2) {
                            int start = Integer.parseInt(args.get(1));
                            if(start < 0) start = s.length() + start;
                            start = BotUtils.clamp(start, 0, s.length());
                            variables.put(varName, s.substring(start));
                        }else{
                            int start = Integer.parseInt(args.get(1));
                            int end = Integer.parseInt(args.get(2));
                            if(start < 0) start = s.length() + start;
                            if(end < 0) end = s.length() + end;
                            start = BotUtils.clamp(start, 0, s.length());
                            end = BotUtils.clamp(end, 0, s.length());
                            if(start > end){
                                int trash = start;
                                start = end;
                                end = trash;
                            }
                            variables.put(varName, s.substring(start, end));
                        }
                    }catch (Exception ex){}
                    return Mono.just(true);
                })
        );
        actions.put("substr", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2 || args.size() == 3)
                .flatMap(x -> {
                    try {
                        String s = args.get(0);
                        if(args.size() == 2) {
                            int start = Integer.parseInt(args.get(1));
                            if(start < 0) start = s.length() + start;
                            start = BotUtils.clamp(start, 0, s.length());
                            variables.put(varName, s.substring(start));
                        }else{
                            int start = Integer.parseInt(args.get(1));
                            if(start < 0) start = s.length() + start;
                            start = BotUtils.clamp(start, 0, s.length());
                            int end = start + Integer.parseInt(args.get(2));
                            if(end < start) return Mono.just(true);
                            end = BotUtils.clamp(end, 0, s.length());
                            variables.put(varName, s.substring(start, end));
                        }
                    }catch (Exception ex){}
                    return Mono.just(true);
                })
        );
        actions.put("escapeRegex", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 1)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, BotUtils.escapeRegex(args.get(0)))))
                .map(x -> true)
        );
        actions.put("replace", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 3)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, args.get(0).replace(args.get(1), args.get(2)))))
                .map(x -> true)
        );
        actions.put("replaceFirst", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 3)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, args.get(0).replaceFirst(args.get(1), args.get(2)))))
                .map(x -> true)
        );
        actions.put("replaceRegex", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 3)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, args.get(0).replaceAll(args.get(1), args.get(2)))))
                .map(x -> true)
        );
        actions.put("length", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 1)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + args.get(1).length())))
                .map(x -> true)
        );
        actions.put("levenshteinDistance", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + LevenshteinDistance.getDefaultInstance().apply(args.get(0), args.get(1)))))
                .map(x -> true)
        );
        actions.put("calc", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 1)
                .flatMap(x -> {
                    Expression exp = new Expression(args.get(0));
                    double result = exp.calculate();
                    if(Double.isNaN(result))
                        variables.put(varName, exp.getErrorMessage());
                    else
                        variables.put(varName, "" + result);
                    return Mono.just(true);
                })
        );
        actions.put("formatTimestamp", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2 || args.size() == 3)
                .flatMap(x -> {
                    try {
                        Date date = new Date(Long.parseLong(args.get(0)));
                        SimpleDateFormat format = new SimpleDateFormat(args.get(1));
                        format.setTimeZone(TimeZone.getTimeZone(args.size() == 3 ? args.get(2) : "GMT"));
                        variables.put(varName, format.format(date));
                    }catch (IllegalArgumentException ex){
                        variables.put(varName, "Could not format timestamp: " + ex.getMessage());
                    }catch (Exception ex){}
                    return Mono.just(true);
                })
        );




        actions.put("not", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 1)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + !Boolean.parseBoolean(args.get(0)))))
                .map(x -> true)
        );
        actions.put("and", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + Boolean.logicalAnd(Boolean.parseBoolean(args.get(0)), Boolean.parseBoolean(args.get(1))))))
                .map(x -> true)
        );
        actions.put("nand", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + !Boolean.logicalAnd(Boolean.parseBoolean(args.get(0)), Boolean.parseBoolean(args.get(1))))))
                .map(x -> true)
        );
        actions.put("or", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + Boolean.logicalOr(Boolean.parseBoolean(args.get(0)), Boolean.parseBoolean(args.get(1))))))
                .map(x -> true)
        );
        actions.put("nor", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + !Boolean.logicalOr(Boolean.parseBoolean(args.get(0)), Boolean.parseBoolean(args.get(1))))))
                .map(x -> true)
        );
        actions.put("xor", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + Boolean.logicalXor(Boolean.parseBoolean(args.get(0)), Boolean.parseBoolean(args.get(1))))))
                .map(x -> true)
        );
        actions.put("xnor", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + !Boolean.logicalXor(Boolean.parseBoolean(args.get(0)), Boolean.parseBoolean(args.get(1))))))
                .map(x -> true)
        );

        actions.put("ifTrue", (g, args, variables, varName) -> Mono.just(true));
        actions.put("ifFalse", (g, args, variables, varName) -> Mono.just(false));
    }

    public static boolean executeScript(Guild g, String script, Map<String, String> replace, @Nullable Instant timestamp){
        Thread thread = new Thread(()->{
            replace.put("guildid", g.getId().asString());
            replace.put("guildname", g.getName());
            replace.put("guildicon", g.getIconUrl(Image.Format.PNG).orElse("No icon set"));
            replace.put("botid", g.getClient().getSelfId().get().asString());

            if(timestamp != null) {
                replace.put("timestamp", "" + timestamp.toEpochMilli());
                Matcher timestampMatcher = Pattern.compile("%(timestamp_([^%_]+)(?:_([^%]+))?)%").matcher(script);
                Date time = Date.from(timestamp);
                DateFormatSymbols dfs = DateFormatSymbols.getInstance(Locale.ENGLISH);
                try {dfs = DateFormatSymbols.getInstance(Locale.forLanguageTag(LocaleManager.getGuildLanguage(g.getId().asLong())));}catch (Exception ex){}
                while (timestampMatcher.find()) {
                    try {
                        SimpleDateFormat format = new SimpleDateFormat(timestampMatcher.group(2));
                        format.setDateFormatSymbols(dfs);
                        format.setTimeZone(TimeZone.getTimeZone(timestampMatcher.group(3) != null ? timestampMatcher.group(3) : "GMT"));
                        replace.put(timestampMatcher.group(1), format.format(time));
                    }catch (Exception ex){
                        replace.put(timestampMatcher.group(1), "Error in pattern `" + timestampMatcher.group(2) + "`: " + ex.getMessage());
                    }
                }
            }

            String[] commands = script.split("\n");
            int waited = 0;
            int requiredIndention = 0;
            boolean execute = true;
            for(String command : commands){
                int indention = 0;
                while(command.startsWith(" ") || command.startsWith("\t")){
                    command = command.substring(1);
                    indention++;
                }
                if(command.startsWith("//")) continue;
                if(command.endsWith("\r")) command = command.substring(0, command.length() - 1);
                Matcher m = Pattern.compile("^([^\\(]+)\\((.*)\\) *$").matcher(command);
                if(!m.matches()) continue;
                List<String> args = BotUtils.contentToParameters(m.group(2));
                command = m.group(1).trim();
                if(args == null) continue;

                for(int i = 0; i < args.size(); i++){
                    String arg = args.get(i);
                    for(String toReplace : replace.keySet()){
                        String newValue = replace.get(toReplace);
                        if(newValue != null) arg = arg.replace("%" + toReplace + "%", newValue);
                    }
                    args.set(i, arg);
                }
                String variableName = null;
                if(command.contains("=")){
                    variableName = command.split("=")[0];
                    command = command.substring(variableName.length() + 1).trim();
                    variableName = variableName.trim();
                }
                if(command.equalsIgnoreCase("else") && (indention == requiredIndention-1)){
                    execute = !execute;
                    requiredIndention = indention + 1;
                }else if(indention < requiredIndention){
                    requiredIndention = indention;
                    execute = true;
                }
                if(!execute) continue;
                if(command.equalsIgnoreCase("wait")){
                    if(waited < 60000 && args.size() == 1){
                        try{
                            int toWait = Integer.parseInt(args.get(0));
                            if(waited + toWait > 60000){
                                toWait = 60000 - waited;
                            }
                            waited += toWait;
                            synchronized (TimeUnit.MILLISECONDS){
                                TimeUnit.MILLISECONDS.wait(toWait);
                            }
                        }catch (Exception ex){}
                    }
                }else{
                    for (Map.Entry<String, Action> action : actions.entrySet()) {
                        if (action.getKey().equalsIgnoreCase(command)) {
                            for (int i = 0; i < args.size(); i++)
                                for (Map.Entry<String, String> variable : replace.entrySet())
                                    args.set(i, args.get(i).replace("%" + variable.getKey() + "%", variable.getValue()));
                                boolean con = action.getValue().execute(g, args, replace, variableName).defaultIfEmpty(true).block();
                                if(action.getKey().toLowerCase().startsWith("if")){
                                    execute = con;
                                    requiredIndention = indention + 1;
                                }else if(!con)
                                    return;
                        }
                    }
                }
            }
        });
        thread.start();
        return true;
    }

    public static void addMemberVariables(Map<String, String> replace, Member m){
        replace.put("userid", m.getId().asString());
        replace.put("username", m.getUsername());
        replace.put("usernick", m.getDisplayName());
        replace.put("userpfp", m.getAvatarUrl(Image.Format.PNG).orElse(m.getDefaultAvatarUrl()));
        replace.put("discriminator", m.getDiscriminator());
        replace.put("usermention", m.getMention());
    }
    public static void addChannelVariables(Map<String, String> replace, GuildChannel c){
        replace.put("channelid", c.getId().asString());
        replace.put("channelmention", c.getMention());
        replace.put("channelname", c.getName());
        replace.put("channeltype", "" + c.getType().getValue());
        if(c instanceof GuildMessageChannel){
            GuildMessageChannel tc = (GuildMessageChannel)c;
            replace.put("channeltopic", tc.getTopic().orElse(""));
            replace.put("categoryid", tc.getCategoryId().orElse(Snowflake.of(0L)).asString());
            replace.put("nsfw", "" + tc.isNsfw());
            if(c instanceof TextChannel) replace.put("slowmode", "" + ((TextChannel)tc).getRateLimitPerUser());
        }else if(c instanceof VoiceChannel){
            VoiceChannel vc = (VoiceChannel)c;
            replace.put("categoryid", vc.getCategoryId().orElse(Snowflake.of(0L)).asString());
            replace.put("bitrate", "" + vc.getBitrate());
            replace.put("userlimit", "" + vc.getUserLimit());
            replace.put("connectedusercount", "" + vc.getVoiceStates().count().block());
        }else if(c instanceof Category){
            Category cg = (Category)c;
            replace.put("channelcount", "" + cg.getChannels().count().block());
        }
    }
    public static void addMessageVariables(Map<String, String> replace, Message m){
        replace.put("content", m.getContent().orElse(""));
        replace.put("messageid", m.getId().asString());
        replace.put("messageurl", "https://discordapp.com/channels/" + m.getGuild().block().getId().asString() + "/" + m.getChannelId().asString() + "/" + m.getId().asString());
        replace.put("embedcount", "" + m.getEmbeds().size());
        replace.put("attachmentcount", "" + m.getAttachments().size());
        replace.put("pinned", "" + m.isPinned());
        replace.put("tts", "" + m.isTts());
        replace.put("mentionseveryone", "" + m.mentionsEveryone());
    }

    public static void onCommandEvent(MessageCreateEvent e, String[] command, List<String> args, boolean success){
        Map<String, String> replace = new HashMap<>();
        addChannelVariables(replace, e.getMessage().getChannel().ofType(GuildMessageChannel.class).block());
        addMessageVariables(replace, e.getMessage());
        addMemberVariables(replace, e.getMember().get());
        replace.put("argcount", "" + args.size());
        for(int i = 0; i < args.size(); i++)
            replace.put("arg" + (i+1), args.get(i));
    }

    public static void onCustomCommandEvent(MessageCreateEvent e, String command){
        Map<String, String> replace = new HashMap<>();
        addChannelVariables(replace, e.getMessage().getChannel().ofType(GuildMessageChannel.class).block());
        addMessageVariables(replace, e.getMessage());
        addMemberVariables(replace, e.getMember().get());
    }

    public static void onUnknownCommand(MessageCreateEvent e){
        Map<String, String> replace = new HashMap<>();
        addChannelVariables(replace, e.getMessage().getChannel().ofType(GuildMessageChannel.class).block());
        addMessageVariables(replace, e.getMessage());
        addMemberVariables(replace, e.getMember().get());
    }

}