package Main;

import DataManager.*;
import DataManager.SQLGuild;
import Scripts.ScriptExecutor;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.channel.CategoryCreateEvent;
import discord4j.core.event.domain.channel.TextChannelCreateEvent;
import discord4j.core.event.domain.channel.VoiceChannelCreateEvent;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BotEvents {

    /**
     * Registers all regular {@link discord4j.core.event.domain.Event}s
     * @param client The {@link DiscordClient} the {@link discord4j.core.event.domain.Event}s should get registered to
     */
    public static void registerEvents(DiscordClient client){
        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(e -> System.out.println("Logged in as " + e.getSelf().getUsername()));
        // initialize nonexistent guilds and update existing ones
        client.getEventDispatcher().on(GuildCreateEvent.class)
                .doOnNext(e -> {
                    if(DataManager.guildIsRegistered(e.getGuild().getId().asLong()))
                        DataManager.updateGuild(e.getGuild());
                    else
                        DataManager.initializeGuild(e.getGuild());
                })
                .subscribe();
        // commands
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> e.getGuildId().isPresent())
                .filter(e -> e.getMessage().getAuthor().get().getId().asLong() != e.getClient().getSelfId().get().asLong())
                .filter(e -> !e.getMessage().getAuthor().get().isBot())
                .filter(e -> {
                    List<Long> blocked = DataManager.getBlockedChannels(e.getGuildId().get().asLong());
                    return !(blocked.contains(e.getMessage().getChannelId().asLong()) || blocked.contains(e.getMessage().getChannel().ofType(TextChannel.class).block().getCategoryId().orElse(Snowflake.of(0)).asLong()));
                })
                .flatMap(e -> Mono.justOrEmpty(e.getMessage().getContent())
                        .flatMap(content -> BotUtils.getPrefix(e.getGuildId().get().asLong())
                                // commands
                                .flatMap(pref -> Flux.fromIterable(BotCommands.commands.entrySet())
                                        .filter(cmd -> BotUtils.isCommand(content, cmd.getKey(), pref))
                                        .flatMap(cmd -> BotUtils.truncateMessage(content, cmd.getKey(), pref)
                                                .flatMap(truncated -> BotUtils.messageToArgs(truncated)
                                                        .flatMap(args -> Mono.just(LocaleManager.getGuildLanguage(e.getGuildId().get().asLong()))
                                                                .flatMap(lang -> cmd.getValue().execute(e, pref, args, lang).onErrorReturn(false)
                                                                        .filter(success -> !success)
                                                                        .flatMap(success -> {
                                                                            BotUtils.sendHelpMessage(e.getMessage().getChannel().ofType(TextChannel.class).block(), cmd.getKey()[0], pref, lang);
                                                                            return Mono.just(true);
                                                                        })
                                                                        .switchIfEmpty(Mono.just(true))
                                                                )
                                                        )
                                                )
                                        )
                                        // custom commands
                                        .switchIfEmpty(Flux.fromIterable(DataManager.getCustomCommands(e.getGuildId().get().asLong()).entrySet())
                                                .filter(cmd -> BotUtils.isCommand(content, new String[]{cmd.getKey()}, pref))
                                                .flatMap(cmd -> e.getMessage().getChannel()
                                                        .flatMap(c -> c.createMessage(mcs -> mcs.setContent(cmd.getValue())))
                                                        .flatMap(x -> Mono.just(true))
                                                )
                                        )
                                        // no command found
                                        .switchIfEmpty(Mono.justOrEmpty(DataManager.getGuild(e.getGuildId().get().asLong()).getUnknownCommandMessage())
                                                .filter(em -> em.trim().length() > 0)
                                                .filter(em -> BotUtils.isCommand(content, new String[]{".*"}, pref))
                                                .flatMap(em -> e.getMessage().getChannel()
                                                        .flatMap(c -> c.createMessage(mcs -> mcs.setContent(em)))
                                                        .flatMap(x -> Mono.just(true))
                                                )
                                        )
                                        .next()
                                )
                        ).onErrorResume(ex -> {ex.printStackTrace(); return Mono.just(true);})
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
        // suggestions
        client.getEventDispatcher().on(ReactionAddEvent.class)
                .flatMap(e -> e.getMessage()
                        .filter(m -> e.getUserId().asLong() != e.getClient().getSelfId().get().asLong())
                        .filter(m -> BotUtils.isSuggestionMessage(m))
                        .doOnNext(m -> {
                            long itemsPerPage = 5;
                            AtomicLong currentPage = new AtomicLong(Long.parseLong(m.getEmbeds().get(0).getFooter().get().getText().substring(5, m.getEmbeds().get(0).getFooter().get().getText().indexOf('/'))));
                            if(e.getEmoji().asUnicodeEmoji().get().equals(BotUtils.arrowLeft))
                                currentPage.decrementAndGet();
                            else if(e.getEmoji().asUnicodeEmoji().get().equals(BotUtils.arrowRight))
                                currentPage.incrementAndGet();
                            long maxPages = DataManager.getBotSuggestionPageCount(itemsPerPage);
                            currentPage.set(BotUtils.clamp(currentPage.get(), 1L, maxPages));
                            List<SQLBotSuggestion> suggestions = DataManager.getBotSuggestions(currentPage.get(), itemsPerPage);
                            String s = suggestions.stream().map(suggestion -> suggestion.getStatus().getEmoji().asUnicodeEmoji().get().getRaw() + " #" + suggestion.getId() + ": " + suggestion.getTitle()).collect(Collectors.joining("\n"));
                            m.edit(mes -> mes.setEmbed(ecs -> ecs
                                    .setTitle("Suggestions")
                                    .setDescription(s)
                                    .setFooter("Page " + currentPage.get() + "/" + maxPages, null)
                            )).subscribe();
                            m.removeReaction(e.getEmoji(), e.getUserId()).subscribe();
                        })
                ).subscribe();
        // help manager
        client.getEventDispatcher().on(ReactionAddEvent.class)
                .filter(e -> e.getGuildId().isPresent())
                .flatMap(e -> e.getMessage()
                        .filter(m -> e.getUserId().asLong() != e.getClient().getSelfId().get().asLong())
                        .flatMap(m -> Mono.just(BotUtils.getHelpPageNumber(m))
                                .filter(page -> page > -1)
                                .doOnNext(page -> m.removeReaction(e.getEmoji(), e.getUserId()).subscribe())
                                .filter(page -> e.getEmoji().asUnicodeEmoji().isPresent())
                                .doOnNext(pageNumber -> {
                                    String lang = LocaleManager.getGuildLanguage(e.getGuild().block());
                                    String prefix = e.getGuildId().isPresent() ? DataManager.getGuild(e.getGuildId().get().asLong()).getBotPrefix() : "=";
                                    Map<String, Map<String, String>> pages = BotUtils.getHelpPages(e.getGuild().block());
                                    AtomicInteger pageNbr = new AtomicInteger(BotUtils.clamp(pageNumber, 0, pages.size() - 1));
                                    if(e.getEmoji().asUnicodeEmoji().get().equals(BotUtils.arrowLeft))
                                        pageNbr.decrementAndGet();
                                    else if(e.getEmoji().asUnicodeEmoji().get().equals(BotUtils.arrowRight))
                                        pageNbr.incrementAndGet();
                                    else if(e.getEmoji().asUnicodeEmoji().get().equals(BotUtils.x)){
                                        m.removeAllReactions().subscribe();
                                        User u = e.getUser().block();
                                        m.edit(mes -> mes.setEmbed(ecs -> ecs
                                                .setTitle(m.getEmbeds().get(0).getTitle().orElse("Help"))
                                                .setDescription(LocaleManager.getLanguageString(lang,"help.closed", u.getUsername() + "#" + u.getDiscriminator()))
                                                .setColor(BotUtils.botColor)
                                        )).subscribe();
                                        Mono.delay(Duration.ofMillis(3000))
                                                .flatMap(x -> m.delete("Help message closed by " + u.getUsername() + "#" + u.getDiscriminator() + " (" + u.getId().asString() + ")"))
                                                .subscribe();
                                        return;
                                    }
                                    int currPage = -1;
                                    for(String pageName : pages.keySet()){
                                        currPage++;
                                        if(pageNbr.get() != currPage) continue;
                                        Map<String, String> page = pages.get(pageName);
                                        m.edit(mes -> mes.setEmbed(ecs -> ecs
                                                .setTitle(LocaleManager.getLanguageString(lang, "help.title"))
                                                .addField(pageName, page.values().stream().map(cmd -> BotUtils.formatString(cmd, prefix)).collect(Collectors.joining("\n")), false)
                                                .setFooter(LocaleManager.getLanguageString(lang, "help.footer", "" + (pageNbr.get()+1), "" + pages.size()), null)
                                                .setColor(new Color(8158463))
                                        )).subscribe();
                                        break;
                                    }
                                })
                        )
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
                    replace.clear();
                    ScriptExecutor.addMemberVariables(replace, e.getMember().get());
                    ScriptExecutor.addChannelVariables(replace, e.getMessage().getChannel().ofType(TextChannel.class).block());
                    ScriptExecutor.addMessageVariables(replace, e.getMessage());
                    return e.getMessage().getAuthor().get().getId().asLong() != e.getClient().getSelfId().get().asLong();
                })
                .filter(e -> e.getMessage().getAuthor().get().getId().asLong() == 226677096091484160L)
                .filter(e -> ScriptExecutor.executeScript(e.getGuild().block(), e.getMessage().getContent().get(), replace, e.getMessage().getTimestamp()))
                .subscribe();
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e ->  e.getGuildId().isPresent())
                .filter(e -> {
                    replace.clear();
                    ScriptExecutor.addMemberVariables(replace, e.getMember().get());
                    ScriptExecutor.addChannelVariables(replace, e.getMessage().getChannel().ofType(TextChannel.class).block());
                    ScriptExecutor.addMessageVariables(replace, e.getMessage());
                    return e.getMember().get().getId().asLong() != e.getClient().getSelfId().get().asLong();
                })
                .flatMap(e -> Flux.fromIterable(DataManager.getScripts(e.getGuildId().get().asLong(), ScriptExecutor.ScriptEvent.onMessage))
                        .filter(script -> ScriptExecutor.executeScript(e.getGuild().block(), script, replace, e.getMessage().getTimestamp()))
                )
                .subscribe();
        client.getEventDispatcher().on(CategoryCreateEvent.class)
                .filter(e -> {
                    replace.clear();
                    ScriptExecutor.addChannelVariables(replace, e.getCategory());
                    return true;
                })
                .flatMap(e -> Flux.fromIterable(BotUtils.addLists(DataManager.getScripts(e.getCategory().getGuildId().asLong(), ScriptExecutor.ScriptEvent.onCategoryCreated), DataManager.getScripts(e.getCategory().getGuildId().asLong(), ScriptExecutor.ScriptEvent.onChannelCreated)))
                        .filter(script -> ScriptExecutor.executeScript(e.getCategory().getGuild().block(), script, replace, null))
                )
                .subscribe();
        client.getEventDispatcher().on(TextChannelCreateEvent.class)
                .filter(e -> {
                    replace.clear();
                    ScriptExecutor.addChannelVariables(replace, e.getChannel());
                    return true;
                })
                .flatMap(e -> Flux.fromIterable(BotUtils.addLists(DataManager.getScripts(e.getChannel().getGuildId().asLong(), ScriptExecutor.ScriptEvent.onTextChannelCreated), DataManager.getScripts(e.getChannel().getGuildId().asLong(), ScriptExecutor.ScriptEvent.onChannelCreated)))
                        .filter(script -> ScriptExecutor.executeScript(e.getChannel().getGuild().block(), script, replace, null))
                )
                .subscribe();
        client.getEventDispatcher().on(VoiceChannelCreateEvent.class)
                .filter(e -> {
                    replace.clear();
                    ScriptExecutor.addChannelVariables(replace, e.getChannel());
                    return true;
                })
                .flatMap(e -> Flux.fromIterable(BotUtils.addLists(DataManager.getScripts(e.getChannel().getGuildId().asLong(), ScriptExecutor.ScriptEvent.onVoiceChannelCreated), DataManager.getScripts(e.getChannel().getGuildId().asLong(), ScriptExecutor.ScriptEvent.onChannelCreated)))
                        .filter(script -> ScriptExecutor.executeScript(e.getChannel().getGuild().block(), script, replace, null))
                )
                .subscribe();
    }

}