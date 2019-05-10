package Scripts;

import discord4j.core.object.entity.Guild;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Action {

    /**
     * Executes the action and returns whether the script execution should be continued
     * @param g         The guild the script gets executed in
     * @param args      The arguments passed in the function
     * @param variables The variables
     * @param varName   The name of the variable name the value should be saved in
     * @return Whether the script execution should be continued
     */
    Mono<Boolean> execute(Guild g, List<String> args, Map<String, String> variables, @Nullable String varName);

}