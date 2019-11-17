package DataManager;

import lombok.Getter;

import java.time.Instant;

public class SQLMember {

    @Getter private long userId = 0L;
    @Getter private long guildId = 0L;
    @Getter private boolean nicked = false;
    @Getter private String guildName = "";
    @Getter private Instant joinedAt = Instant.MIN;
    @Getter private int sentMessageCount = 0;
    @Getter private int sentCommandCount = 0;
    @Getter private int sentPublicMessageCount = 0;
    @Getter private int sentUnknownCommandCount = 0;
    @Getter private int sentCustomCommandCount = 0;

    public SQLMember(){}
    public SQLMember(long userId, long guildId, boolean nicked, String guildName, Instant joinedAt, int sentMessageCount, int sentCommandCount, int sentPublicMessageCount, int sentUnknownCommandCount, int sentCustomCommandCount) {
        this.userId = userId;
        this.guildId = guildId;
        this.nicked = nicked;
        this.guildName = guildName;
        this.joinedAt = joinedAt;
        this.sentMessageCount = sentMessageCount;
        this.sentCommandCount = sentCommandCount;
        this.sentPublicMessageCount = sentPublicMessageCount;
        this.sentUnknownCommandCount = sentUnknownCommandCount;
        this.sentCustomCommandCount = sentCustomCommandCount;
    }

}
