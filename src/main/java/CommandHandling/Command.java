package CommandHandling;

import discord4j.core.object.util.Permission;

public class Command{

    private CommandExecutable executable;
    private boolean nsfwOnly = false;
    private boolean usableInDM = false;
    private boolean usableInGuilds = true;
    private String botPermission = null;
    private boolean everyone = false;
    private Permission[] defaultPerms = new Permission[]{};
    private boolean requiresOwner = false;
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

    public CommandExecutable getExecutable(){return executable;}
    public boolean isNsfwOnly(){return nsfwOnly;}
    public boolean isUsableInDM(){return usableInDM;}
    public boolean isUsableInGuilds(){return usableInGuilds;}
    public String getBotPermission(){return botPermission;}
    public boolean usableByEveryone(){return everyone;}
    public Permission[] getDefaultPerms(){return defaultPerms;}
    public boolean requiresOwner(){return requiresOwner;}

}
