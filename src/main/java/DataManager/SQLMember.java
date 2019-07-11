package DataManager;

import java.time.Instant;

public class SQLMember {

    private long userId = 0L;
    private long guildId = 0L;
    private boolean nicked = false;
    private String guildName = "";
    private Instant joinedAt = Instant.MIN;
    private int sentMessageCount = 0;
    private int sentCommandCount = 0;
    private int sentPublicMessageCount = 0;
    private int sentUnknownCommandCount = 0;
    private int sentCustomCommandCount = 0;

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
