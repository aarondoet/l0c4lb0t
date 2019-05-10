package Main;

import DataManager.DataManager;
import DataManager.SQLGuild;
import Scripts.ScriptExecutor;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotEvents {

    /**
     * Registers all regular {@link discord4j.core.event.domain.Event}s
     * @param client The {@link DiscordClient} the {@link discord4j.core.event.domain.Event}s should get registered to
     */
    public static void registerEvents(DiscordClient client){
        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(e -> System.out.println("Logged in as " + e.getSelf().getUsername()));
        // initialize nonexistent guilds
        client.getEventDispatcher().on(GuildCreateEvent.class)
                .filter(e -> !DataManager.guildIsRegistered(e.getGuild().getId().asLong()))
                .flatMap(e -> DataManager.initializeGuild(e.getGuild()))
                .subscribe();
        // commands
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> e.getGuildId().isPresent())
                .filter(e -> e.getMessage().getAuthor().get().getId().asLong() != e.getClient().getSelfId().get().asLong())
                .filter(e -> {
                    List<Long> blocked = DataManager.getBlockedChannels(e.getGuildId().get().asLong());
                    return !(blocked.contains(e.getMessage().getChannelId().asLong()) || blocked.contains(e.getMessage().getChannel().ofType(TextChannel.class).block().getCategoryId().orElse(Snowflake.of(0)).asLong()));
                })
                .flatMap(e -> Mono.justOrEmpty(e.getMessage().getContent())
                        .flatMap(content -> BotUtils.getPrefix(e.getGuildId().get().asLong())
                                .flatMap(pref -> Flux.fromIterable(BotCommands.commands.entrySet())
                                        .filter(cmd -> BotUtils.isCommand(content, cmd.getKey(), pref))
                                        .flatMap(cmd -> BotUtils.truncateMessage(content, cmd.getKey(), pref)
                                                .flatMap(truncated -> BotUtils.messageToArgs(truncated)
                                                        .flatMap(args -> Mono.just(LocaleManager.getGuildLanguage(e.getGuildId().get().asLong()))
                                                                .flatMap(lang -> cmd.getValue().execute(e, pref, args, lang).onErrorReturn(false)
                                                                        .filter(success -> !success)
                                                                        .flatMap(success -> {
                                                                            BotUtils.sendHelpMessage(e.getMessage().getChannel().ofType(TextChannel.class).block(), cmd.getKey()[0], pref, lang);
                                                                            return Mono.empty();
                                                                        })
                                                                )
                                                        )
                                                )
                                        ).next()
                                )
                        )
                ).subscribe();
        // custom commands
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> e.getGuildId().isPresent())
                .filter(e -> e.getMessage().getAuthor().get().getId().asLong() != e.getClient().getSelfId().get().asLong())
                .filter(e -> !DataManager.getBlockedChannels(e.getGuildId().get().asLong()).contains(e.getMessage().getChannelId().asLong()))
                .flatMap(e -> Mono.justOrEmpty(e.getMessage().getContent())
                        .flatMap(content -> BotUtils.getPrefix(e.getGuildId().get().asLong())
                                .flatMap(pref -> Flux.fromIterable(DataManager.getCustomCommands(e.getGuildId().get().asLong()).entrySet())
                                        .filter(cmd -> BotUtils.isCommand(content, new String[]{cmd.getKey()}, pref))
                                        .flatMap(cmd -> e.getMessage().getChannel()
                                                .flatMap(c -> c.createMessage(cmd.getValue()))
                                        ).next()
                                )
                        )
                ).subscribe();
        // poll manager
        client.getEventDispatcher().on(ReactionAddEvent.class)
                .filter(e -> e.getGuildId().isPresent())
                .flatMap(e -> e.getMessage()
                        .filter(m -> m.getAuthor().isPresent())
                        .filter(m -> m.getAuthor().get().getId().asLong() == e.getClient().getSelfId().get().asLong())
                        .filter(m -> m.getEmbeds().size() == 1)
                        .filter(m -> m.getEmbeds().get(0).getFooter().isPresent() && m.getEmbeds().get(0).getTitle().isPresent() && m.getEmbeds().get(0).getDescription().isPresent() && m.getEmbeds().get(0).getTimestamp().isPresent() && m.getEmbeds().get(0).getFields().isEmpty())
                        .filter(m -> m.getEmbeds().get(0).getTitle().get().matches("\\*\\*Poll\\*\\* _\\((\\d+d ?)?(\\d+h ?)?(\\d+min ?)?(\\d+s)?\\)_"))
                        .filter(m -> m.getEmbeds().get(0).getFooter().get().getText().matches("(One vote per user|Multiple votes per user)"))
                        .flatMap(m -> {
                            if(m.getReactors(e.getEmoji()).filter(u -> u.getId().asLong() == e.getClient().getSelfId().get().asLong()).count().block() != 1){
                                m.removeReaction(e.getEmoji(), e.getUserId()).subscribe();
                                return Mono.empty();
                            }
                            if(!PermissionManager.hasPermission(e.getGuild().block(), e.getUser().block().asMember(e.getGuildId().get()).block(), "vote", true)){
                                m.removeReaction(e.getEmoji(), e.getUserId()).subscribe();
                                return Mono.empty();
                            }
                            boolean multiVote = m.getEmbeds().get(0).getFooter().get().getText().equals("Multiple votes per user") || PermissionManager.hasPermission(e.getGuild().block(), e.getUser().block().asMember(e.getGuildId().get()).block(), "multiVote", false);
                            if(!multiVote){
                                Flux.fromIterable(m.getReactions())
                                        .flatMap(r -> m.getReactors(r.getEmoji())
                                                .filter(ra -> ra.getId().asLong() == e.getUserId().asLong())
                                        )
                                        .count()
                                        .filter(cnt -> {
                                            if(cnt > 1)
                                                m.removeReaction(e.getEmoji(), e.getUserId()).subscribe();
                                            return cnt > 1;
                                        }).subscribe();
                            }
                            if(!PermissionManager.hasPermission(e.getGuild().block(), e.getUser().block().asMember(e.getGuildId().get()).block(), "voteWhenEnded", false)){
                                if(m.getEmbeds().get(0).getTimestamp().get().isBefore(Instant.now())){
                                    m.removeReaction(e.getEmoji(), e.getUserId()).subscribe();
                                }
                            }
                            return Mono.empty();
                        })
                ).subscribe();
        // reaction roles
        client.getEventDispatcher().on(ReactionAddEvent.class)
                .filter(e -> e.getGuildId().isPresent())
                .flatMap(e -> e.getMessage()
                        .flatMap(m -> {
                            String emoji;
                            if(e.getEmoji().asUnicodeEmoji().isPresent()){
                                emoji = e.getEmoji().asUnicodeEmoji().get().getRaw();
                            }else{
                                emoji = e.getEmoji().asCustomEmoji().get().getId().asString();
                            }

                            return Mono.empty();
                        })
                ).subscribe();
        // delete server invites
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> e.getGuildId().isPresent())
                .filter(e -> e.getMessage().getContent().isPresent())
                .filter(e -> !PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "sendInvites", false))
                .flatMap(e -> {
                    boolean delete = false;
                    String warning = "";
                    SQLGuild sg = DataManager.getGuild(e.getGuildId().get().asLong());
                    if(sg != null){
                        delete = sg.getDeleteInvites();
                        warning = sg.getInviteWarning();
                    }
                    if(!delete && warning.length() == 0) return Mono.empty();

                    Matcher matcher = Pattern.compile(BotUtils.INVITE_MATCHER).matcher(e.getMessage().getContent().get());
                    List<String> usedInvites = new ArrayList<>();
                    while (matcher.find())
                        usedInvites.add(matcher.group());
                    List<String> allowedInvites = DataManager.getAllowedInvites(e.getGuildId().get().asLong());
                    Member m = e.getMember().get();
                    for(String invite : usedInvites)
                        if(!allowedInvites.contains(invite)){
                            if(delete) e.getMessage().delete("Used invite " + invite + " which is not allowed").subscribe();
                            if(warning.length() > 0) e.getMessage().getChannel().block().createMessage(warning.replace("%mention%", m.getMention()).replace("%username%", m.getUsername()).replace("%nickname%", m.getDisplayName()).replace("%discriminator%", m.getDiscriminator()).replace("%code%", invite)).subscribe();
                            break;
                        }
                    return Mono.empty();
                }).subscribe();
    }

    /**
     * Registers all {@link discord4j.core.event.domain.Event}s that are used for custom scripts
     * @param client The {@link DiscordClient} the {@link discord4j.core.event.domain.Event}s should get registered to
     */
    public static void registerScriptEvents(DiscordClient client){
        HashMap<String, String> replace = new HashMap<>();
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> e.getGuildId().isPresent())
                .filter(e -> e.getMessage().getAuthor().isPresent())
                .filter(e -> {
                    ScriptExecutor.addMemberVariables(replace, e.getMember().get());
                    ScriptExecutor.addChannelVariables(replace, e.getMessage().getChannel().ofType(TextChannel.class).block());
                    ScriptExecutor.addMessageVariables(replace, e.getMessage());

                    return e.getMessage().getAuthor().get().getId().asLong() != e.getClient().getSelfId().get().asLong();
                })
                .filter(e -> e.getMessage().getAuthor().get().getId().asLong() == 226677096091484160l)
                .flatMap(e -> {ScriptExecutor.executeScript(e.getGuild().block(), e.getMessage().getContent().get(), replace, e.getMessage().getTimestamp()); return Mono.empty();})
                .subscribe();
    }

}