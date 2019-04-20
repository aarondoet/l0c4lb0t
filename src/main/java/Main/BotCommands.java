package Main;

import discord4j.core.object.entity.Member;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.JDBCType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

public class BotCommands {

    public static final Map<String[], Command> commands = new HashMap<>();
    static {
        commands.put(new String[]{"ping"}, (e, pref, args) -> e.getMessage().getChannel()
                .flatMap(c -> c.createMessage("pong"))
                .flatMap(c -> Mono.just(true))
        );
        commands.put(new String[]{"test2"}, (e, pref, args) -> e.getMessage().getChannel()
                .flatMap(c -> c.createMessage("Args: " + args.toString()))
                .flatMap(c -> Mono.just(true))
        );
        commands.put(new String[]{"prefix"}, (e, pref, args) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(args.size() != 1){
                        return Mono.just(false);
                    }
                    String newPref = args.get(0);
                    if(newPref.contains(" ") || newPref.length() == 0 || newPref.length() > 10){
                        return Mono.just(false);
                    }
                    if(DataManager.setGuild(e.getGuildId().get().asLong(), "bot_prefix", args.get(0), JDBCType.VARCHAR))
                        c.createMessage("Changed prefix to `" + newPref + "`").subscribe();
                    else
                        BotUtils.sendErrorMessage(c).subscribe();
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"choose", "c"}, (e, pref, args) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    if(args.size() < 2)
                        return Mono.just(false);
                    Random rn = new Random();
                    c.createMessage("I choose `" + args.get(rn.nextInt(args.size())) + "`").subscribe();
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"userlimit"}, (e, pref, args) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    return Mono.just(true);
                })
        );
        commands.put(new String[]{"resetnicks"}, (e, pref, args) -> e.getMessage().getChannel()
                .flatMap(c -> Mono.just(e.getMember().get())
                        .flatMap(u -> {
                            int cnt = 0;
                            for(Member m : e.getGuild().block().getMembers().toIterable()){
                                if(m.getNickname().isPresent()){
                                    cnt++;
                                    m.edit(gmes -> gmes.setNickname(null)).subscribe();
                                }
                            }
                            c.createMessage("Reset " + cnt + " nicknames").subscribe();
                            return Mono.just(true);
                        })
                )
        );
        commands.put(new String[]{"nix"}, (e, pref, args) -> e.getMessage().getChannel()
                .flatMap(c -> {
                    return Mono.just(true);
                })
        );
    }

}