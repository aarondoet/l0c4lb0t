package Scripts;

import discord4j.core.object.entity.Guild;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;

public interface Action {

    Mono<Boolean> execute(Guild g, List<String> args, HashMap<String, String> variables, String varName);

}