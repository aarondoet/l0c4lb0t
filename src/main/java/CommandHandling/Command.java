package CommandHandling;

import Main.BotMain;
import Main.BotUtils;
import Main.RatelimitUtils;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private String name;
    private String[] aliases = new String[]{};
    private Permission[] neededPermissions = new Permission[]{};
    public Command(String name, CommandExecutable executable){
        this.name = name;
        this.executable = executable;
    }
    public Command(String name){
        this.name = name;
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
    public String getName(){return name;}
    public String[] getAliases(){return aliases;}
    public Permission[] getNeededPermissions(){return neededPermissions;}

    public Command nsfwOnly(boolean nsfwOnly){this.nsfwOnly = nsfwOnly; return this;}
    public Command usableInDM(boolean usableInDM){this.usableInDM = usableInDM; return this;}
    public Command usableInGuilds(boolean usableInGuilds){this.usableInGuilds = usableInGuilds; return this;}
    public Command usableByEveryone(boolean everyone){this.everyone = everyone; return this;}
    public Command requiresOwner(boolean requiresOwner){this.requiresOwner = requiresOwner; return this;}
    public Command requiresBotOwner(boolean requiresBotOwner){this.requiresBotOwner = requiresBotOwner; return this;}
    public Command withExecutable(CommandExecutable executable){this.executable = executable; return this;}
    public Command withBotPermission(String botPermission){this.botPermission = botPermission; return this;}
    public Command withDefaultPerms(Permission... defaultPerms){this.defaultPerms = defaultPerms; return this;}
    public Command withPermissions(String botPermission, Permission... defaultPerms){this.botPermission = botPermission; this.defaultPerms = defaultPerms; return this;}
    public Command withRatelimit(RatelimitUtils.Ratelimit ratelimit){this.ratelimit = ratelimit; return this;}
    public Command withName(String name){this.name = name; return this;}
    public Command withAliases(String... aliases){this.aliases = aliases; return this;}
    public Command needsPermissions(Permission... permissions){this.neededPermissions = permissions; return this;}

    public String truncateMessage(String prefix, String msg){
        String bId = BotMain.client.getSelfId().map(Snowflake::asString).orElse("");
        Matcher m = Pattern.compile("^(?:" + BotUtils.escapeRegex(prefix) + "|<@!?" + bId + "> ?)(?i)(?:" + name + (aliases.length > 0 ? "|" + String.join("|", aliases) : "") + ")(?i)(?: (.*))?$", Pattern.DOTALL).matcher(msg);
        if(m.matches())
            return m.group(1) == null ? "" : m.group(1);
        else
            return null;
    }

    public void register(){
        if(getCommand(getName()).isPresent() || Arrays.stream(getAliases()).anyMatch(alias -> getCommand(alias).isPresent()))
            System.out.println("Command " + getName() + " is already registered");
        else
            commands.add(this);
    }

    private static List<Command> commands = new ArrayList<>();
    public static Optional<Command> getCommand(String name){
        return commands.stream().filter(cmd -> cmd.getName().equalsIgnoreCase(name) || Arrays.stream(cmd.getAliases()).anyMatch(alias -> alias.equalsIgnoreCase(name))).findAny();
    }
    public static List<Command> getCommands(){
        return Collections.unmodifiableList(commands);
    }

}
