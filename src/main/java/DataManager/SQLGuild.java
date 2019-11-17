package DataManager;

import lombok.Getter;

import java.time.Instant;

public class SQLGuild {

    @Getter private long guildId = 0L;
    @Getter private String name = "";
    @Getter private String iconUrl = "";
    @Getter private Instant createdAt = Instant.MIN;
    @Getter private Instant joinedAt = Instant.MIN;
    @Getter private long ownerId = 0L;
    @Getter private String botPrefix = "=";
    @Getter private String language = "en";
    @Getter private long joinRole = 0L;
    @Getter private String joinMessage = "";
    @Getter private String leaveMessage = "";
    @Getter private String banMessage = "";
    @Getter private String unknownCommandMessage = "";
    @Getter private long publicChannelId = 0L;
    @Getter private boolean deleteInvites = false;
    @Getter private String inviteWarning = "";
    @Getter private int sentMessageCount = 0;
    @Getter private int receivedMessageCount = 0;
    @Getter private int receivedCommandCount = 0;
    @Getter private int receivedPublicMessageCount = 0;
    @Getter private int receivedUnknownCommandCount = 0;
    @Getter private int receivedCustomCommandCount = 0;
    @Getter private String token = "";
    @Getter private long suggestionChannelId = 0L;
    @Getter private String readonlyToken = "";

    public SQLGuild(){}
    public SQLGuild(long guildId, String name, String iconUrl, Instant createdAt, Instant joinedAt, long ownerId, String botPrefix, String language, long joinRole, String joinMessage, String leaveMessage, String banMessage, String unknownCommandMessage, long publicChannelId, boolean deleteInvites, String inviteWarning, int sentMessageCount, int receivedMessageCount, int receivedCommandCount, int receivedPublicMessageCount, int receivedUnknownCommandCount, int receivedCustomCommandCount, String token, long suggestionChannelId, String readonlyToken) {
        this.guildId = guildId;
        this.name = name;
        this.iconUrl = iconUrl;
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
        this.receivedMessageCount = receivedMessageCount;
        this.receivedCommandCount = receivedCommandCount;
        this.receivedPublicMessageCount = receivedPublicMessageCount;
        this.receivedUnknownCommandCount = receivedUnknownCommandCount;
        this.receivedCustomCommandCount = receivedCustomCommandCount;
        this.token = token;
        this.suggestionChannelId = suggestionChannelId;
        this.readonlyToken = readonlyToken;
    }

}
