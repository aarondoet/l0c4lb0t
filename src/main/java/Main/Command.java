package Main;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

import java.util.List;

public interface Command {

    Mono<Boolean> execute(MessageCreateEvent e, String prefix, List<String> args);

}