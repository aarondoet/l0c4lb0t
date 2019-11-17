package DataManager;

import lombok.Getter;

public class SQLPermissions {

    @Getter private long guildId;
    @Getter private long holderId;
    @Getter private String action;
    @Getter private int type;

    public SQLPermissions(long guildId, long holderId, String action, int type) {
        this.guildId = guildId;
        this.holderId = holderId;
        this.action = action;
        this.type = type;
    }

}
