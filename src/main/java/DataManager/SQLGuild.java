package DataManager;

import java.time.Instant;

public class SQLGuild {

    private long guildId;
    private String name;
    private Instant createdAt;
    private Instant joinedAt;
    private long ownerId;
    private String botPrefix;
    private String language;
    private long joinRole;
    private String joinMessage;
    private String leaveMessage;
    private String banMessage;
    private String unknownCommandMessage;
    private long publicChannelId;
    private boolean deleteInvites;
    private String inviteWarning;
    private int sentMessageCount;
    private int sentCommandCount;
    private int sentPublicMessageCount;
    private int sentUnknownCommandCount;
    private int sentCustomCommandCount;
    private String token;

    public SQLGuild(long guildId, String name, Instant createdAt, Instant joinedAt, long ownerId, String botPrefix, String language, long joinRole, String joinMessage, String leaveMessage, String banMessage, String unknownCommandMessage, long publicChannelId, boolean deleteInvites, String inviteWarning, int sentMessageCount, int sentCommandCount, int sentPublicMessageCount, int sentUnknownCommandCount, int sentCustomCommandCount, String token) {
        this.guildId = guildId;
        this.name = name;
        this.createdAt = createdAt;
        this.joinedAt = joinedAt;
        this.ownerId = ownerId;
        this.botPrefix = botPrefix;
        this.language = language;
        this.joinRole = joinRole;
        this.joinMessage = joinMessage;
        this.leaveMessage = leaveMessage;
        this.banMessage = banMessage;
        this.unknownCommandMessage = unknownCommandMessage;
        this.publicChannelId = publicChannelId;
        this.deleteInvites = deleteInvites;
        this.inviteWarning = inviteWarning;
        this.sentMessageCount = sentMessageCount;
        this.sentCommandCount = sentCommandCount;
        this.sentPublicMessageCount = sentPublicMessageCount;
        this.sentUnknownCommandCount = sentUnknownCommandCount;
        this.sentCustomCommandCount = sentCustomCommandCount;
        this.token = token;
    }

    public long getGuildId() {return guildId;}

    public String getName() {return name;}

    public Instant getCreatedAt() {return createdAt;}

    public Instant getJoinedAt() {return joinedAt;}

    public long getOwnerId() {return ownerId;}

    public String getBotPrefix(){return botPrefix;}

    public String getLanguage() {return language;}

    public long getJoinRole() {return joinRole;}

    public String getJoinMessage() {return joinMessage;}

    public String getLeaveMessage() {return leaveMessage;}

    public String getBanMessage() {return banMessage;}

    public String getUnknownCommandMessage() {return unknownCommandMessage;}

    public long getPublicChannelId() {return publicChannelId;}

    public boolean getDeleteInvites() {return deleteInvites;}

    public String getInviteWarning() {return inviteWarning;}

    public int getSentMessageCount() {return sentMessageCount;}

    public int getSentCommandCount() {return sentCommandCount;}

    public int getSentPublicMessageCount() {return sentPublicMessageCount;}

    public int getSentUnknownCommandCount() {return sentUnknownCommandCount;}

    public int getSentCustomCommandCount() {return sentCustomCommandCount;}

    public String getToken() {return token;}
}
