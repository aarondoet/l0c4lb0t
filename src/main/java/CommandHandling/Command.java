package CommandHandling;

public class Command{

    private CommandExecutable executable;
    private boolean nsfwOnly = false;
    private boolean usableInDM = false;
    private boolean usableInGuilds = true;
    public Command(CommandExecutable executable, boolean nsfwOnly, boolean usableInDM, boolean usableInGuilds){
        this.executable = executable;
        this.nsfwOnly = nsfwOnly;
        this.usableInDM = usableInDM;
        this.usableInGuilds = usableInGuilds;
    }
    public Command(CommandExecutable executable, boolean nsfwOnly, boolean usableInDM){
        this.executable = executable;
        this.nsfwOnly = nsfwOnly;
        this.usableInDM = usableInDM;
    }
    public Command(CommandExecutable executable){
        this.executable = executable;
    }

    public CommandExecutable getExecutable(){return executable;}
    public boolean isNsfwOnly(){return nsfwOnly;}
    public boolean isUsableInDM(){return usableInDM;}
    public boolean isUsableInGuilds(){return usableInGuilds;}

}
