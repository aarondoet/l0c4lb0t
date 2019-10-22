package Main;

import CommandHandling.BotCommands;
import DataManager.*;
import DataManager.SQLGuild;
import Scripts.ScriptExecutor;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.CategoryCreateEvent;
import discord4j.core.event.domain.channel.TextChannelCreateEvent;
import discord4j.core.event.domain.channel.VoiceChannelCreateEvent;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildUpdateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.GuildMessageChannel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.trait.Categorizable;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
                .doOnNext(e -> BotUtils.l0c4lh057 = e.getClient().getUserById(Snowflake.of(226677096091484160L)).block())
                .doOnNext(e -> System.out.println("Logged in as " + e.getSelf().getUsername() + "#" + e.getSelf().getDiscriminator()))
                .subscribe();
        // initialize nonexistent guilds and update existing ones
        client.getEventDispatcher().on(GuildCreateEvent.class)
                .doOnNext(e -> DataManager.initializeGuild(e.getGuild()))
                .subscribe();
        client.getEventDispatcher().on(GuildUpdateEvent.class)
                .doOnNext(e -> DataManager.initializeGuild(e.getCurrent()))
                .subscribe();
        // stats
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .doOnNext(e -> {
                    if(e.getMessage().getAuthor().map(User::getId).map(Snowflake::asLong).orElse(0L).equals(e.getClient().getSelfId().map(Snowflake::asLong).orElse(1L))){
                        DataManager.updateStats("sent_message_count");
                        e.getGuildId().map(Snowflake::asLong).ifPresentOrElse(gId -> DataManager.updateGuildStats(gId, "sent_message_count"), () -> DataManager.updateStats("sent_dm_count"));
                    }else{
                        DataManager.updateStats("received_message_count");
                        e.getGuildId().map(Snowflake::asLong).ifPresentOrElse(gId -> DataManager.updateGuildStats(gId, "received_message_count"), () -> DataManager.updateStats("received_dm_count"));
                    }
                })
                .subscribe();
        // commands
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> e.getMember().isPresent()) // no webhooks and only in guilds
                .filter(e -> !e.getMessage().getAuthor().get().isBot()) // no bots
                .filterWhen(e -> {
                    List<Long> blocked = DataManager.getBlockedChannels(e.getGuildId().get().asLong());
                    //return !(blocked.contains(e.getMessage().getChannelId().asLong()) || blocked.contains(e.getMessage().getChannel().ofType(Categorizable.class).block().getCategoryId().orElse(Snowflake.of(0)).asLong()));
                    return e.getMessage().getChannel().ofType(Categorizable.class).map(c -> c.getCategoryId().orElse(Snowflake.of(0))).map(categoryId -> !blocked.contains(categoryId.asLong()) || !blocked.contains(e.getMessage().getChannelId().asLong()));
                })
                .flatMap(e -> Mono.justOrEmpty(e.getMessage().getContent())
                        .flatMap(content -> Mono.just(BotUtils.getPrefix(e.getGuildId().get().asLong()))
                                // commands
                                .flatMap(pref -> Flux.fromIterable(BotCommands.commands.entrySet())
                                        .filter(cmd -> cmd.getValue().isUsableInGuilds())
                                        .filterWhen(cmd -> e.getGuild().map(g -> g.getOwnerId().asLong() == e.getMember().get().getId().asLong() || !cmd.getValue().requiresOwner()))
                                        //.filter(cmd -> BotUtils.isCommand(content, cmd.getKey(), pref)) // removed, bc it is part of truncateMessage now (returns empty when it is not the command)
                                        .flatMap(cmd -> Mono.justOrEmpty(BotUtils.truncateMessage(content, cmd.getKey(), pref))
                                                // command found
                                                .doOnNext(truncated -> {
                                                    DataManager.updateStats("received_command_count");
                                                    DataManager.updateGuildStats(e.getGuildId().get().asLong(), "received_command_count");
                                                })
                                                .flatMap(truncated -> e.getMessage().getChannel().ofType(GuildMessageChannel.class)
                                                        .filter(channel -> !cmd.getValue().isNsfwOnly() || BotUtils.checkChannelForNSFW(channel))
                                                        .flatMap(channel -> Mono.justOrEmpty(BotUtils.messageToArgs(truncated))
                                                                .flatMap(args -> Mono.just(LocaleManager.getGuildLanguage(e.getGuildId().get().asLong()))
                                                                        .flatMap(lang -> e.getGuild()
                                                                                .flatMap(guild -> {
                                                                                    if(!PermissionManager.hasPermission(guild, e.getMember().get(), channel, cmd.getValue().getBotPermission(), cmd.getValue().usableByEveryone(), cmd.getValue().getDefaultPerms()))
                                                                                        return Mono.just(BotUtils.sendNoPermissionsMessage(channel));
                                                                                    return cmd.getValue().getExecutable().execute(e, pref, args, lang).doOnError(Throwable::printStackTrace).onErrorReturn(false)
                                                                                            // SCRIPT EXECUTION START
                                                                                            .doOnNext(success -> ScriptExecutor.onCommandEvent(e, cmd.getKey(), args, success))
                                                                                            // SCRIPT EXECUTION END
                                                                                            .filter(success -> !success)
                                                                                            .doOnNext(success -> BotUtils.sendHelpMessage(channel, cmd.getKey()[0], pref, lang))
                                                                                            .defaultIfEmpty(true);
                                                                                })
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                        // custom commands
                                        .switchIfEmpty(Flux.fromIterable(DataManager.getCustomCommands(e.getGuildId().get().asLong()).entrySet())
                                                .filter(cmd -> BotUtils.isCommand(content, new String[]{cmd.getKey()}, pref))
                                                // custom command found
                                                .doOnNext(truncated -> {
                                                    DataManager.updateStats("received_custom_command_count");
                                                    DataManager.updateGuildStats(e.getGuildId().get().asLong(), "received_custom_command_count");
                                                })
                                                .flatMap(cmd -> e.getMessage().getChannel()
                                                        .flatMap(c -> c.createMessage(mcs -> mcs.setContent(cmd.getValue())))
                                                        .doOnNext(x -> ScriptExecutor.onCustomCommandEvent(e, cmd.getKey()))
                                                        .map(x -> true)
                                                )
                                        )
                                        // no command found
                                        .switchIfEmpty(Mono.just(Optional.ofNullable(DataManager.getGuild(e.getGuildId().get().asLong()).getUnknownCommandMessage()).orElse(""))
                                                .filter(em -> BotUtils.isCommand(content, new String[]{"[a-zA-Z0-9]+"}, pref))
                                                .doOnNext(em -> {
                                                    DataManager.updateStats("received_unknown_command_count");
                                                    DataManager.updateGuildStats(e.getGuildId().get().asLong(), "received_unknown_command_count");
                                                })
                                                .doOnNext(em -> ScriptExecutor.onUnknownCommand(e))
                                                .filter(em -> em.trim().length() > 0)
                                                .flatMap(em -> e.getMessage().getChannel()
                                                        .flatMap(c -> c.createMessage(mcs -> mcs.setContent(em)))
                                                        .map(x -> true)
                                                )
                                        )
                                        .next()
                                )
                        ).doOnError(Throwable::printStackTrace)
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
                            if(!PermissionManager.hasPermission(e.getGuild().block(), e.getUser().flatMap(u -> u.asMember(e.getGuildId().get())).block(), "vote", true)){
                                m.removeReaction(e.getEmoji(), e.getUserId()).subscribe();
                                return Mono.empty();
                            }
                            boolean multiVote = m.getEmbeds().get(0).getFooter().get().getText().equals("Multiple votes per user") || PermissionManager.hasPermission(e.getGuild().block(), e.getUser().flatMap(u -> u.asMember(e.getGuildId().get())).block(), "multiVote", false);
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
                            if(!PermissionManager.hasPermission(e.getGuild().block(), e.getUser().flatMap(u -> u.asMember(e.getGuildId().get())).block(), "voteWhenEnded", false)){
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
        // bot suggestions
        client.getEventDispatcher().on(ReactionAddEvent.class)
                .flatMap(e -> e.getMessage()
                        .filter(m -> e.getUserId().asLong() != e.getClient().getSelfId().get().asLong())
                        .filter(BotUtils::isBotSuggestionMessage)
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
                                    .setTitle("Bot Suggestions")
                                    .setDescription(s)
                                    .setFooter("Page " + currentPage.get() + "/" + maxPages, null)
                            )).subscribe();
                            m.removeReaction(e.getEmoji(), e.getUserId()).subscribe();
                        })
                ).subscribe();
        // suggestions
        client.getEventDispatcher().on(ReactionAddEvent.class)
                .filter(e -> e.getGuildId().isPresent())
                .flatMap(e -> e.getMessage()
                        .filter(m -> e.getUserId().asLong() != e.getClient().getSelfId().get().asLong())
                        .flatMap(m -> Mono.just(BotUtils.getSuggestionPageNumber(m))
                                .filter(nbr -> nbr > -1)
                                .doOnNext(nbr -> {
                                    long itemsPerPage = 5;
                                    AtomicLong currentPage = new AtomicLong(nbr);
                                    if(e.getEmoji().asUnicodeEmoji().get().equals(BotUtils.arrowLeft))
                                        currentPage.decrementAndGet();
                                    else if(e.getEmoji().asUnicodeEmoji().get().equals(BotUtils.arrowRight))
                                        currentPage.incrementAndGet();
                                    long maxPages = DataManager.getSuggestionPageCount(e.getGuildId().get().asLong(), itemsPerPage);
                                    currentPage.set(BotUtils.clamp(currentPage.get(), 1L, maxPages));
                                    List<SQLFeedback> suggestions = DataManager.getSuggestions(e.getGuildId().get().asLong(), currentPage.get(), itemsPerPage);
                                    String s = suggestions.stream().map(suggestion -> suggestion.getStatus().getEmoji().asUnicodeEmoji().get().getRaw() + " #" + suggestion.getId() + ": " + suggestion.getTitle()).collect(Collectors.joining("\n"));
                                    m.edit(mes -> mes.setEmbed(ecs -> ecs
                                            .setTitle("Suggestions")
                                            .setDescription(s)
                                            .setFooter("Page " + currentPage.get() + "/" + maxPages, null)
                                    )).subscribe();
                                    m.removeReaction(e.getEmoji(), e.getUserId()).subscribe();
                                })
                        )
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
                                                .setColor(BotUtils.botColor)
                                        )).subscribe();
                                        break;
                                    }
                                })
                        )
                ).subscribe();
        // delete server invites
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> e.getGuildId().isPresent())
                .filter(e -> e.getMember().isPresent())
                .filter(e -> e.getMessage().getContent().isPresent())
                //.filter(e -> !PermissionManager.hasPermission(e.getGuild().block(), e.getMember().get(), "sendInvites", false))
                .filterWhen(e -> e.getGuild().map(g -> !PermissionManager.hasPermission(g, e.getMember().get(), "sendInvites", false)))
                .flatMap(e -> e.getMessage().getChannel()
                        .flatMap(channel -> {
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
                                    if(warning.length() > 0) channel.createMessage(
                                            warning .replace("%mention%", m.getMention())
                                                    .replace("%username%", m.getUsername())
                                                    .replace("%nickname%", m.getDisplayName())
                                                    .replace("%discriminator%", m.getDiscriminator())
                                                    .replace("%id%", m.getId().asString())
                                                    .replace("%code%", invite))
                                            .subscribe();
                                    break;
                                }
                            return Mono.empty();
                        })
                ).subscribe();
        // dm actions
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> e.getGuildId().isEmpty())
                .filter(e -> e.getMessage().getContent().isPresent())
                .filter(e -> e.getMessage().getAuthor().isPresent())
                .filter(e -> e.getMessage().getAuthor().get().getId().asLong() != e.getClient().getSelfId().get().asLong())
                .flatMap(e -> Flux.fromIterable(BotCommands.commands.entrySet())
                        .filter(cmd -> cmd.getValue().isUsableInDM())
                        .flatMap(cmd -> Mono.justOrEmpty(BotUtils.truncateMessage(e.getMessage().getContent().get(), cmd.getKey(), "="))
                                .flatMap(truncated -> e.getMessage().getChannel()
                                        .flatMap(channel -> Mono.just(BotUtils.messageToArgs(truncated))
                                                .flatMap(args -> Mono.just("en")
                                                // TODO: add users to database, then use their selected language here
                                                //.flatMap(args -> Mono.just(DataManager.getUser(e.getMessage().getAuthor().get().getId().asLong()).getLanguage())
                                                        .flatMap(lang -> cmd.getValue().getExecutable().execute(e, "=", args, lang).doOnError(Throwable::printStackTrace).onErrorReturn(false)
                                                                .filter(success -> !success)
                                                                .flatMap(success -> {
                                                                    BotUtils.sendHelpMessage(channel, cmd.getKey()[0], "=", lang);
                                                                    return Mono.just(true);
                                                                })
                                                                .switchIfEmpty(Mono.just(true))
                                                        )
                                                )
                                        )
                                )
                        )
                )
                /*.flatMap(e -> e.getMessage().getChannel()
                        .flatMap(c -> c.createEmbed(ecs -> ecs
                                .setTitle("Sorry, but private commands are not available yet.")
                                .setDescription(
                                        "You can support me on [Patreon](https://patreon.com/l0c4lh057/) and fund this bot. Currently I am working on this bot in my free time, but since I am a student I have school stuff to do and can't work on this bot all day long. Money wouldn't change this, but I would definitely have more motivation to work on this.\n\n" +
                                        "You can also support the development of this bot on [GitHub](https://github.com/l0c4lh057/l0c4lb0t/). If I don't have the time to work on something, maybe you have. I will credit you in the `about` command and I'll appreciate your effort."
                                )
                                .setColor(BotUtils.botColor)
                                .setAuthor(BotUtils.l0c4lh057.getUsername(), null, BotUtils.l0c4lh057.getAvatarUrl())
                                .setFooter("- The bot developer", null)
                        ))
                )*/
                .subscribe();
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
                    ScriptExecutor.addChannelVariables(replace, e.getMessage().getChannel().ofType(GuildMessageChannel.class).block());
                    ScriptExecutor.addMessageVariables(replace, e.getMessage());
                    return e.getMessage().getAuthor().get().getId().asLong() != e.getClient().getSelfId().get().asLong();
                })
                .filter(e -> e.getMessage().getAuthor().get().getId().asLong() == 226677096091484160L)
                .filter(e -> ScriptExecutor.executeScript(e.getGuild().block(), e.getMessage().getContent().get(), replace, e.getMessage().getTimestamp()))
                .subscribe();
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e ->  e.getMember().isPresent()) // only in guilds and no webhook messages
                .filter(e -> {
                    replace.clear();
                    ScriptExecutor.addMemberVariables(replace, e.getMember().get());
                    ScriptExecutor.addChannelVariables(replace, e.getMessage().getChannel().ofType(GuildMessageChannel.class).block());
                    ScriptExecutor.addMessageVariables(replace, e.getMessage());
                    return e.getMember().get().getId().asLong() != e.getClient().getSelfId().get().asLong();
                })
                .flatMap(e -> Flux.fromIterable(DataManager.getScripts(e.getGuildId().get().asLong(), ScriptExecutor.ScriptEvent.onMessage))
                        //.filter(script -> ScriptExecutor.executeScript(e.getGuild().block(), script, replace, e.getMessage().getTimestamp()))
                        .filterWhen(script -> e.getGuild().map(g -> ScriptExecutor.executeScript(g, script, replace, e.getMessage().getTimestamp())))
                )
                .subscribe();
        client.getEventDispatcher().on(CategoryCreateEvent.class)
                .doOnNext(e -> {
                    replace.clear();
                    ScriptExecutor.addChannelVariables(replace, e.getCategory());
                })
                .flatMap(e -> Flux.fromIterable(BotUtils.addLists(DataManager.getScripts(e.getCategory().getGuildId().asLong(), ScriptExecutor.ScriptEvent.onCategoryCreated), DataManager.getScripts(e.getCategory().getGuildId().asLong(), ScriptExecutor.ScriptEvent.onChannelCreated)))
                        //.filter(script -> ScriptExecutor.executeScript(e.getCategory().getGuild().block(), script, replace, null))
                        .filterWhen(script -> e.getCategory().getGuild().map(g -> ScriptExecutor.executeScript(g, script, replace, null)))
                )
                .subscribe();
        client.getEventDispatcher().on(TextChannelCreateEvent.class)
                .doOnNext(e -> {
                    replace.clear();
                    ScriptExecutor.addChannelVariables(replace, e.getChannel());
                })
                .flatMap(e -> Flux.fromIterable(BotUtils.addLists(DataManager.getScripts(e.getChannel().getGuildId().asLong(), ScriptExecutor.ScriptEvent.onTextChannelCreated), DataManager.getScripts(e.getChannel().getGuildId().asLong(), ScriptExecutor.ScriptEvent.onChannelCreated)))
                        //.filter(script -> ScriptExecutor.executeScript(e.getChannel().getGuild().block(), script, replace, null))
                        .filterWhen(script -> e.getChannel().getGuild().map(g -> ScriptExecutor.executeScript(g, script, replace, null)))
                )
                .subscribe();
        client.getEventDispatcher().on(VoiceChannelCreateEvent.class)
                .doOnNext(e -> {
                    replace.clear();
                    ScriptExecutor.addChannelVariables(replace, e.getChannel());
                })
                .flatMap(e -> Flux.fromIterable(BotUtils.addLists(DataManager.getScripts(e.getChannel().getGuildId().asLong(), ScriptExecutor.ScriptEvent.onVoiceChannelCreated), DataManager.getScripts(e.getChannel().getGuildId().asLong(), ScriptExecutor.ScriptEvent.onChannelCreated)))
                        //.filter(script -> ScriptExecutor.executeScript(e.getChannel().getGuild().block(), script, replace, null))
                        .filterWhen(script -> e.getChannel().getGuild().map(g -> ScriptExecutor.executeScript(g, script, replace, null)))
                )
                .subscribe();
    }

}