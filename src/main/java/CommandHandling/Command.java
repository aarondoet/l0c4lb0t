package CommandHandling;

import Main.RatelimitUtils;
import discord4j.core.object.util.Permission;
import reactor.core.publisher.Mono;

public class Command{

    private CommandExecutable executable;
    private boolean nsfwOnly = false;
    private boolean usableInDM = false;
    private boolean usableInGuilds = true;
    private String botPermission = null;
    private boolean everyone = false;
    private Permission[] defaultPerms = new Permission[]{};
    private boolean requiresOwner = false;
    private RatelimitUtils.Ratelimit ratelimit = null;
    private boolean requiresBotOwner = false;
    public Command(CommandExecutable executable, boolean nsfwOnly, boolean usableInDM, boolean usableInGuilds, String botPermission, boolean everyone, Permission... defaultPerms){
        this.executable = executable;
        this.nsfwOnly = nsfwOnly;
        this.usableInDM = usableInDM;
        this.usableInGuilds = usableInGuilds;
        this.botPermission = botPermission;
        this.everyone = everyone;
        this.defaultPerms = defaultPerms;
    }
    public Command(CommandExecutable executable, boolean nsfwOnly, boolean usableInDM, String botPermission, boolean everyone, Permission... defaultPerms){
        this.executable = executable;
        this.nsfwOnly = nsfwOnly;
        this.usableInDM = usableInDM;
        this.botPermission = botPermission;
        this.everyone = everyone;
        this.defaultPerms = defaultPerms;
    }
    public Command(CommandExecutable executable, String botPermission, boolean everyone, Permission... defaultPerms){
        this.executable = executable;
        this.botPermission = botPermission;
        this.everyone = everyone;
        this.defaultPerms = defaultPerms;
    }
    public Command(CommandExecutable executable, boolean nsfwOnly, boolean usableInDM){
        this.executable = executable;
        this.nsfwOnly = nsfwOnly;
        this.usableInDM = usableInDM;
    }
    public Command(CommandExecutable executable, boolean requiresOwner){
        this.executable = executable;
        this.requiresOwner = requiresOwner;
    }
    public Command(CommandExecutable executable){
        this.executable = executable;
    }
    public Command(){
        this.executable = (e, prefix, args, lang) -> e.getMessage().getChannel().flatMap(c -> c.createMessage("This command is not implemented yet.")).map(m -> true);
    };

    public CommandExecutable getExecutable(){return executable;}
    public boolean isNsfwOnly(){return nsfwOnly;}
    public boolean isUsableInDM(){return usableInDM;}
    public boolean isUsableInGuilds(){return usableInGuilds;}
    public String getBotPermission(){return botPermission;}
    public boolean usableByEveryone(){return everyone;}
    public Permission[] getDefaultPerms(){return defaultPerms;}
    public boolean requiresOwner(){return requiresOwner;}
    public RatelimitUtils.Ratelimit getRatelimit(){return ratelimit;}
    public boolean requiresBotOwner(){return requiresBotOwner;}

    public Command nsfwOnly(boolean nsfwOnly){this.nsfwOnly = nsfwOnly; return this;}
    public Command usableInDM(boolean usableInDM){this.usableInDM = usableInDM; return this;}
    public Command usableInGuilds(boolean usableInGuilds){this.usableInGuilds = usableInGuilds; return this;}
    public Command usableByEveryone(boolean everyone){this.everyone = everyone; return this;}
    public Command requiresOwner(boolean requiresOwner){this.requiresOwner = requiresOwner; return this;}
    public Command requiresBotOwner(boolean requiresBotOwner){this.requiresBotOwner = requiresBotOwner; return this;}
    public Command withExecutable(CommandExecutable executable){this.executable = executable; return this;}
    public Command withBotPermission(String botPermission){this.botPermission = botPermission; return this;}
    public Command withDefaultPerms(Permission[] defaultPerms){this.defaultPerms = defaultPerms; return this;}
    public Command withRatelimit(RatelimitUtils.Ratelimit ratelimit){this.ratelimit = ratelimit; return this;}

}
