package Scripts;

import Main.BotUtils;
import Main.LocaleManager;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.*;
import discord4j.core.object.util.Image;
import discord4j.core.object.util.Snowflake;
import discord4j.core.util.EntityUtil;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.mariuszgromada.math.mxparser.Expression;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptExecutor {

    public enum ScriptEvent {
        onMessage("onMessage"),
        onJoin("onJoin");

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
                ).flatMap(x -> Mono.just(true))
        );
        actions.put("editMessage", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 3)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(Long.parseLong(args.get(1))))
                                .flatMap(m -> m.edit(BotUtils.jsonToMessageEdit(args.get(2))))
                        )
                )
                .flatMap(x -> Mono.just(true))
        );
        actions.put("deleteMessage", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(Long.parseLong(args.get(1))))
                                .flatMap(m -> m.delete("A script ran the delete command on this message."))
                        )
                )
                .flatMap(x -> Mono.just(true))
        );
        actions.put("sendDM", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("giveRole", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("removeRole", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("setNickname", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("resetNickname", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("kick", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("ban", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("unban", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("clearReactions", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(Long.parseLong(args.get(1))))
                                .flatMap(m -> m.removeAllReactions())
                        )
                )
                .flatMap(x -> Mono.just(true))
        );
        actions.put("createRole", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("deleteRole", (g, args, variables, varName) -> {return Mono.just(true);});
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
        actions.put("pinMessage", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("unpinMessage", (g, args, variables, varName) -> {return Mono.just(true);});




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
                .flatMap(x -> Mono.just(args.get(0).equals(args.get(1))))
        );
        actions.put("breakIfEquals", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(!args.get(0).equals(args.get(1))))
        );
        actions.put("continueIfEqualsIgnoreCase", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(args.get(0).equalsIgnoreCase(args.get(1))))
        );
        actions.put("breakIfEqualsIgnoreCase", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(!args.get(0).equalsIgnoreCase(args.get(1))))
        );
        actions.put("continueIfStartsWith", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(args.get(0).startsWith(args.get(1))))
        );
        actions.put("breakIfStartsWith", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(!args.get(0).startsWith(args.get(1))))
        );
        actions.put("continueIfEndsWith", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(args.get(0).endsWith(args.get(1))))
        );
        actions.put("breakIfEndsWith", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(!args.get(0).endsWith(args.get(1))))
        );
        actions.put("continueIfContains", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(args.get(0).contains(args.get(1))))
        );
        actions.put("breakIfContains", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(!args.get(0).contains(args.get(1))))
        );
        actions.put("continueIfMatches", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(Pattern.matches(args.get(1), args.get(0))))
        );
        actions.put("breakIfMatches", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(!Pattern.matches(args.get(1), args.get(0))))
        );
        actions.put("continueIfMentions", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 3)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(Long.parseLong(args.get(1))))
                                .flatMap(m -> {
                                    if(args.get(1).equals("everyone")) return Mono.just(m.mentionsEveryone());
                                    try {
                                        Snowflake sf = Snowflake.of(Long.parseLong(args.get(2)));
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
                        .flatMap(c -> c.getMessageById(Snowflake.of(Long.parseLong(args.get(1))))
                                .flatMap(m -> {
                                    if(args.get(1).equals("everyone")) return Mono.just(!m.mentionsEveryone());
                                    try {
                                        Snowflake sf = Snowflake.of(Long.parseLong(args.get(2)));
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
                        .flatMap(c -> c.getMessageById(Snowflake.of(Long.parseLong(args.get(1))))
                                .flatMap(m -> Mono.just(!m.getAttachments().isEmpty()))
                        )
                )
        );
        actions.put("breakIfHasAttachment", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(Long.parseLong(args.get(1))))
                                .flatMap(m -> Mono.just(m.getAttachments().isEmpty()))
                        )
                )
        );
        actions.put("continueIfHasEmbed", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(Long.parseLong(args.get(1))))
                                .flatMap(m -> Mono.just(!m.getEmbeds().isEmpty()))
                        )
                )
        );
        actions.put("breakIfHasEmbed", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(Long.parseLong(args.get(1))))
                                .flatMap(m -> Mono.just(m.getEmbeds().isEmpty()))
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
                            return Mono.just(cId.get().asLong() == BotUtils.getChannelFromArgument(g, args.get(1)).block().getId().asLong());
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
                            return Mono.just(cId.get().asLong() != BotUtils.getChannelFromArgument(g, args.get(1)).block().getId().asLong());
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
                .flatMap(x -> Mono.just(true))
        );
        actions.put("equalsIgnoreCase", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + args.get(0).equalsIgnoreCase(args.get(1)))))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("startsWith", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + args.get(0).startsWith(args.get(1)))))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("endsWith", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + args.get(0).endsWith(args.get(1)))))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("contains", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + args.get(0).contains(args.get(1)))))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("matches", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + args.get(0).matches(args.get(1)))))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("mentions", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 3)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(Long.parseLong(args.get(1))))
                                .flatMap(m -> {
                                    if(args.get(1).equals("everyone")) return Mono.justOrEmpty(variables.put(varName, "" + m.mentionsEveryone()));
                                    try {
                                        Snowflake sf = Snowflake.of(Long.parseLong(args.get(2)));
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
                .flatMap(x -> Mono.just(true))
        );
        actions.put("hasAttachment", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(Long.parseLong(args.get(1))))
                                .flatMap(m -> Mono.justOrEmpty(variables.put(varName, "" + !m.getAttachments().isEmpty())))
                        )
                )
                .flatMap(x -> Mono.just(true))
        );
        actions.put("hasEmbed", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> BotUtils.getChannelFromArgument(g, args.get(0)).ofType(TextChannel.class)
                        .flatMap(c -> c.getMessageById(Snowflake.of(Long.parseLong(args.get(1))))
                                .flatMap(m -> Mono.justOrEmpty(variables.put(varName, "" + !m.getEmbeds().isEmpty())))
                        )
                )
                .flatMap(x -> Mono.just(true))
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
                                else variables.put(varName, "" + (cid.get().asLong() == BotUtils.getChannelFromArgument(g, args.get(1)).block().getId().asLong()));
                            }
                        }
                    }catch (Exception ex){}
                    return Mono.just(true);
                })
        );
        actions.put("greaterThan", (g, args, variables, varName) -> Mono.just(true));
        actions.put("lessThan", (g, args, variables, varName) -> Mono.just(true));


        actions.put("upperCase", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 1)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, args.get(1).toUpperCase())))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("lowerCase", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 1)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, args.get(1).toLowerCase())))
                .flatMap(x -> Mono.just(true))
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
                .flatMap(x -> Mono.just(true))
        );
        actions.put("replace", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 3)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, args.get(0).replace(args.get(1), args.get(2)))))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("replaceFirst", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 3)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, args.get(0).replaceFirst(args.get(1), args.get(2)))))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("replaceRegex", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 3)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, args.get(0).replaceAll(args.get(1), args.get(2)))))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("length", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 1)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + args.get(1).length())))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("levenshteinDistance", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + LevenshteinDistance.getDefaultInstance().apply(args.get(0), args.get(1)))))
                .flatMap(x -> Mono.just(true))
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
                .flatMap(x -> Mono.just(true))
        );
        actions.put("and", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + Boolean.logicalAnd(Boolean.parseBoolean(args.get(0)), Boolean.parseBoolean(args.get(1))))))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("nand", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + !Boolean.logicalAnd(Boolean.parseBoolean(args.get(0)), Boolean.parseBoolean(args.get(1))))))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("or", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + Boolean.logicalOr(Boolean.parseBoolean(args.get(0)), Boolean.parseBoolean(args.get(1))))))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("nor", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + !Boolean.logicalOr(Boolean.parseBoolean(args.get(0)), Boolean.parseBoolean(args.get(1))))))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("xor", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + Boolean.logicalXor(Boolean.parseBoolean(args.get(0)), Boolean.parseBoolean(args.get(1))))))
                .flatMap(x -> Mono.just(true))
        );
        actions.put("xnor", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> varName != null)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.justOrEmpty(variables.put(varName, "" + !Boolean.logicalXor(Boolean.parseBoolean(args.get(0)), Boolean.parseBoolean(args.get(1))))))
                .flatMap(x -> Mono.just(true))
        );
    }

    public static void executeScript(Guild g, String script, Map<String, String> replace, @Nullable Instant timestamp){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
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
                for(String command : commands){
                    while(command.startsWith(" ") || command.startsWith("\t")) command = command.substring(1);
                    if(command.endsWith("\r")) command = command.substring(0, command.length() - 1);
                    if(command.startsWith("//")) continue;
                    List<String> args;
                    Matcher matcher31 = Pattern.compile("([^(\\n]*)\\( *(\"(?:%[a-zA-Z]+%|\\d+)\") *, *(\"(?:%[a-zA-Z]+%|\\d+)\") *, *(\".*\") *\\) *$").matcher(command); // number, number, something
                    Matcher matcher32 = Pattern.compile("([^(\\n]*)\\( *(\".*\") *, *(\"(?:%[a-zA-Z]+%|-?\\d+)\") *, *(\"(?:%[a-zA-Z]+%|-?\\d+)*\") *\\) *$").matcher(command); // something, number, number
                    Matcher matcher33 = Pattern.compile("([^(\\n]*)\\( *(\"(?:%[a-zA-Z]%|\\d+)*\") *, *(\".*\") *, *(\".*\") *\\) *$").matcher(command); // number something something
                    //Matcher matcher20 = Pattern.compile("([^(\\n]*)\\( *(\"\\d+\") *, *(\".*\") *\\)").matcher(command);
                    Matcher matcher21 = Pattern.compile("([^(\\n]*)\\( *(\"(?:%[a-zA-Z]+%|\\d+)\") *, *(\".*\") *\\) *$").matcher(command); // number, something
                    Matcher matcher22 = Pattern.compile("([^(\\n]*)\\( *(\".*\") *, *(\".*\") *\\) *$").matcher(command); // something, something
                    //Matcher matcher10 = Pattern.compile("([^(\\n]*)\\( *(\"\\d+\")(?: +)?\\)").matcher(command);
                    Matcher matcher11 = Pattern.compile("([^(\\n]*)\\( *(\"(?:%[a-zA-Z]+%|\\d+)\") *\\) *$").matcher(command); // number
                    Matcher matcher12 = Pattern.compile("([^(\\n]*)\\( *(\".*\")(?: +)?\\) *$").matcher(command); // everything
                    Matcher matcher0 = Pattern.compile("([^(\\n]*)\\( *\\) *$").matcher(command);
                    if(matcher31.matches()){
                        args = Arrays.asList(matcher31.group(2), matcher31.group(3), matcher31.group(4));
                        command = matcher31.group(1).trim();
                    }else if(matcher32.matches()){
                        args = Arrays.asList(matcher32.group(2), matcher32.group(3), matcher32.group(4));
                        command = matcher32.group(1).trim();
                    }else if(matcher33.matches()){
                        args = Arrays.asList(matcher33.group(2), matcher33.group(3), matcher33.group(4));
                        command = matcher33.group(1).trim();
                    }else /*if(matcher20.matches()){
                        args = Arrays.asList(matcher20.group(2), matcher20.group(3));
                        command = matcher20.group(1).trim();
                    }else */if(matcher21.matches()){
                        args = Arrays.asList(matcher21.group(2), matcher21.group(3));
                        command = matcher21.group(1).trim();
                    }else if(matcher22.matches()){
                        args = Arrays.asList(matcher22.group(2), matcher22.group(3));
                        command = matcher22.group(1).trim();
                    }else /*if(matcher10.matches()){
                        args = Arrays.asList(matcher10.group(2));
                        command = matcher10.group(1).trim();
                    }else */if(matcher11.matches()){
                        args = Arrays.asList(matcher11.group(2));
                        command = matcher11.group(1).trim();
                    }else if(matcher12.matches()){
                        args = Arrays.asList(matcher12.group(2));
                        command = matcher12.group(1).trim();
                    }else if(matcher0.matches()){
                        args = Arrays.asList();
                        command = matcher0.group(1).trim();
                    }else{
                        continue;
                    }
                    for(int i = 0; i < args.size(); i++){
                        String arg = args.get(i).replace("\\n", "\n");
                        arg = arg.substring(1, arg.length() - 1);
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
                    if(command.equals("wait")){
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
                    }else {
                        for (Map.Entry<String, Action> action : actions.entrySet()) {
                            if (action.getKey().equalsIgnoreCase(command)) {
                                for (int i = 0; i < args.size(); i++)
                                    for (Map.Entry<String, String> variable : replace.entrySet())
                                        args.set(i, args.get(i).replace("%" + variable.getKey() + "%", variable.getValue()));
                                if (!action.getValue().execute(g, args, replace, variableName).defaultIfEmpty(true).block())
                                    return;
                            }
                        }
                    }
                }
            }
        });
        thread.start();
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
        if(c instanceof TextChannel){
            TextChannel tc = (TextChannel)c;
            replace.put("channeltopic", tc.getTopic().orElse(""));
            replace.put("categoryid", tc.getCategoryId().orElse(Snowflake.of(0L)).asString());
            replace.put("nsfw", "" + tc.isNsfw());
            replace.put("slowmode", "" + tc.getRateLimitPerUser());
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

}