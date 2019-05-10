package DataManager;

import java.time.Instant;

public class SQLMember {

    private long userId;
    private long guildId;
    private boolean nicked;
    private String guildName;
    private Instant joinedAt;
    private int sentMessageCount;
    private int sentCommandCount;
    private int sentPublicMessageCount;
    private int sentUnknownCommandCount;
    private int sentCustomCommandCount;

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

    public long getUserId() {return userId;}
    public long getGuildId() {return guildId;}
    public boolean isNicked() {return nicked;}
    public String getGuildName() {return guildName;}
    public Instant getJoinedAt() {return joinedAt;}
    public int getSentMessageCount() {return sentMessageCount;}
    public int getSentCommandCount() {return sentCommandCount;}
    public int getSentPublicMessageCount() {return sentPublicMessageCount;}
    public int getSentUnknownCommandCount() {return sentUnknownCommandCount;}
    public int getSentCustomCommandCount() {return sentCustomCommandCount;}

}
