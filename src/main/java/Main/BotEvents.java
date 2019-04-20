package Main;

import Scripts.ScriptExecutor;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Image;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;

public class BotEvents {

    public static void registerEvents(DiscordClient client){
        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(e -> System.out.println("Logged in as " + e.getSelf().getUsername()));
        client.getEventDispatcher().on(GuildCreateEvent.class)
                .filter(e -> !DataManager.guildIsRegistered(e.getGuild().getId().asLong()))
                .flatMap(e -> DataManager.initializeGuild(e.getGuild()))
                .subscribe();
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> e.getGuildId().isPresent())
                .filter(e -> e.getMessage().getAuthor().isPresent())
                .filter(e -> e.getMessage().getAuthor().get().getId().asLong() != e.getClient().getSelfId().get().asLong())
                .flatMap(e -> Mono.justOrEmpty(e.getMessage().getContent())
                        .flatMap(content -> Flux.fromIterable(BotCommands.commands.entrySet())
                                .flatMap(cmd -> BotUtils.getPrefix(e.getGuildId().get().asLong())
                                        .filter(pref -> BotUtils.isCommand(content, cmd.getKey(), pref))
                                        .flatMap(pref -> BotUtils.truncateMessage(content, cmd.getKey(), pref)
                                                .flatMap(truncated -> BotUtils.messageToArgs(truncated)
                                                        .flatMap(args -> cmd.getValue().execute(e, pref, args)
                                                                .filter(success -> !success)
                                                                .flatMap(success -> BotUtils.sendHelpMessage(e.getMessage().getChannel().block(), cmd.getKey()[0], pref))
                                                        )
                                                )
                                        )
                                ).next()
                        )
                ).subscribe();
    }

    public static void registerScriptEvents(DiscordClient client){
        HashMap<String, String> replace = new HashMap<>();
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> e.getMessage().getAuthor().isPresent())
                .filter(e -> {
                    // user
                    replace.put("%userid%", e.getMessage().getAuthor().get().getId().asString());
                    replace.put("%username%", e.getMessage().getAuthor().get().getUsername());
                    replace.put("%usernick%", e.getMessage().getAuthorAsMember().block().getDisplayName());
                    replace.put("%userpfp%", e.getMessage().getAuthor().get().getAvatarUrl(Image.Format.PNG).orElse(e.getMessage().getAuthor().get().getDefaultAvatarUrl()));
                    replace.put("%userdiscriminator%", e.getMessage().getAuthor().get().getDiscriminator());
                    replace.put("%usermention%", e.getMessage().getAuthor().get().getMention());
                    // channel
                    replace.put("%channelid%", e.getMessage().getChannel().block().getId().asString());
                    replace.put("%channeltopic%", ((TextChannel)e.getMessage().getChannel().block()).getTopic().orElse(""));
                    replace.put("%channelname%", ((TextChannel) e.getMessage().getChannel().block()).getName());
                    replace.put("%channelmention%", e.getMessage().getChannel().block().getMention());
                    // message
                    replace.put("%content%", e.getMessage().getContent().orElse(""));
                    replace.put("%messageid%", e.getMessage().getId().asString());
                    replace.put("%messageurl%", "https://discordapp.com/channels/%guildid%/%channelid%/%messageid%");

                    return e.getMessage().getAuthor().get().getId().asLong() != e.getClient().getSelfId().get().asLong();
                })
                .filter(e -> e.getMessage().getAuthor().get().getId().asLong() == 226677096091484160l)
                .flatMap(e -> ScriptExecutor.executeScript(e.getGuild().block(), e.getMessage().getContent().get(), replace))
                .subscribe();
    }

}