package Main;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

import java.util.List;

public interface Command {

    /**
     * Executes the command and returns whether the the command was executed successfully
     * @param e      The event itself
     * @param prefix The bot prefix for that guild
     * @param args   The arguments the user gave
     * @param lang   The language of the guild
     * @return Whether the command got executed successfully
     */
    Mono<Boolean> execute(MessageCreateEvent e, String prefix, List<String> args, String lang);

}