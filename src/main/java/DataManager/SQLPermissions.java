package DataManager;

public class SQLPermissions {

    private long guildId;
    private long holderId;
    private String action;
    private int type;

    public SQLPermissions(long guildId, long holderId, String action, int type) {
        this.guildId = guildId;
        this.holderId = holderId;
        this.action = action;
        this.type = type;
    }

    public long getGuildId() {return guildId;}
    public long getHolderId() {return holderId;}
    public String getAction() {return action;}
    public int getType() {return type;}

}
