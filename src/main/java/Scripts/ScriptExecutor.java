package Scripts;

import Main.BotUtils;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Image;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptExecutor {

    private static HashMap<String, Action> actions = new HashMap<>();
    static {
        actions.put("sendMessage", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> g.getChannelById(Snowflake.of(Long.parseLong(args.get(0)))).ofType(TextChannel.class)
                        .flatMap(c -> c.createMessage(BotUtils.jsonToMessage(args.get(1)))
                                .filter(m -> varName != null)
                                .flatMap(m -> Mono.justOrEmpty(variables.put(varName, m.getId().asString())))
                        )
                ).flatMap(x -> Mono.just(true))
        );
        actions.put("editMessage", (g, args, variables, varName) -> Mono.just(g).filter(fd -> {
                    System.out.println("a" + variables);
                    return true;
                })
                        .flatMap(fd -> Mono.just(false))
        );
        actions.put("deleteMessage", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("sendDM", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("giveRole", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("removeRole", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("setNickname", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("resetNickname", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("kickUser", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("banUser", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("unbanUser", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("clearReactions", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("createRole", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("deleteRole", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("createTextChannel", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("createVoiceChannel", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("createCategory", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("deleteChannel", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("joinVoiceChannel", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("leaveVoiceChannel", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("clearQueue", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("playSong", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("pauseSong", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("continueSong", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("skipSong", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("enqueueSong", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("calc", (g, args, variables, varName) -> {return Mono.just(true);});
        actions.put("wait", (g, args, variables, varName) -> {return Mono.just(true);});



        actions.put("continueIfEquals", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(args.get(0).equals(args.get(1))))
        );
        actions.put("breakIfEquals", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(!args.get(0).equals(args.get(1))))
        );
        actions.put("continueIfStartsWith", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(args.get(0).startsWith(args.get(1))))
        );

        actions.put("continueIfEndsWith", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(args.get(0).endsWith(args.get(1))))
        );

        actions.put("continueIfContains", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(args.get(0).contains(args.get(1))))
        );

        actions.put("continueIfMatches", (g, args, variables, varName) -> Mono.just(g)
                .filter(x -> args.size() == 2)
                .flatMap(x -> Mono.just(Pattern.matches(args.get(1), args.get(0))))
        );

        actions.put("continueIfMentions", (g, args, variables, varName) -> g.getChannels().ofType(MessageChannel.class)
                .filter(c -> args.size() == 2)
                .flatMap(c -> c.getMessageById(Snowflake.of(Long.parseLong(args.get(0))))
                        .flatMap(m -> {
                            if(m.mentionsEveryone() && args.get(1).equals("everyone")) return Mono.just(true);
                            try {
                                Snowflake sf = Snowflake.of(Long.parseLong(args.get(1)));
                                return Mono.just(
                                        m.getRoleMentionIds().contains(sf) ||
                                                m.getUserMentionIds().contains(sf)
                                );
                            } catch (Exception e) {
                                return Mono.just(false);
                            }
                        })
                ).next()
        );
    }

    public static Mono<Void> executeScript(Guild g, String script, HashMap<String, String> replace){
        replace.put("%guildid%", g.getId().asString());
        replace.put("%guildname%", g.getName());
        replace.put("%guildicon%", g.getIconUrl(Image.Format.PNG).orElse("No icon set"));
        replace.put("%botid%", g.getClient().getSelfId().get().asString());

        for(Map.Entry<String, String> entry : replace.entrySet())
            script = script.replace(entry.getKey(), entry.getValue());

        HashMap<String, String> variables = new HashMap<>();

        String[] commands = script.split("\n");
        int slept = 0;
        for(String command : commands){
            while(command.startsWith(" ") || command.startsWith("\t")) command = command.substring(1);
            if(command.endsWith("\r")) command = command.substring(0, command.length() - 1);
            List<String> args;
            Matcher matcher3 = Pattern.compile("([^(\\n]*)\\( *(\"(?:%[a-zA-Z]+%|\\d+)\") *, *(\"(?:%[a-zA-Z]+%|\\d+)\") *, *(\".*\") *\\)").matcher(command);
            //Matcher matcher20 = Pattern.compile("([^(\\n]*)\\( *(\"\\d+\") *, *(\".*\") *\\)").matcher(command);
            Matcher matcher21 = Pattern.compile("([^(\\n]*)\\( *(\"(?:%[a-zA-Z]+%|\\d+)\") *, *(\".*\") *\\)").matcher(command);
            Matcher matcher22 = Pattern.compile("([^(\\n]*)\\( *(\".*\")(?: +)?,(?: +)?(\".*\")(?: +)?\\)").matcher(command);
            //Matcher matcher10 = Pattern.compile("([^(\\n]*)\\( *(\"\\d+\")(?: +)?\\)").matcher(command);
            Matcher matcher11 = Pattern.compile("([^(\\n]*)\\( *(\"(?:%[a-zA-Z]+%|\\d+)\")(?: +)?\\)").matcher(command);
            Matcher matcher12 = Pattern.compile("([^(\\n]*)\\( *(\".*\")(?: +)?\\)").matcher(command);
            Matcher matcher0 = Pattern.compile("([^(\\n]*)\\( *\\)").matcher(command);
            if(matcher3.matches()){
                args = Arrays.asList(matcher3.group(2), matcher3.group(3), matcher3.group(4));
                command = matcher3.group(1).trim();
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
                String arg = args.get(i);
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
            for(Map.Entry<String, Action> action : actions.entrySet()){
                if(action.getKey().equals(command)){
                    for(int i = 0; i < args.size(); i++)
                        for(Map.Entry<String, String> variable : variables.entrySet())
                            args.set(i, args.get(i).replace("%" + variable.getKey() + "%", variable.getValue()));
                    try{
                        if(!action.getValue().execute(g, args, variables, variableName).block()) return Mono.empty();
                    }catch(Exception ex){}
                }
            }
        }
        return Mono.empty();
    }

}