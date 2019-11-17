package DataManager;

import lombok.Getter;

import java.time.Instant;

public class SQLUser {

    @Getter private long userId;
    @Getter private String username;
    @Getter private Instant createdAt;
    @Getter private int sentMessageCount;
    @Getter private int sentCommandCount;
    @Getter private int sentPublicMessageCount;
    @Getter private int sentUnknownCommandCount;
    @Getter private int sentCustomCommandCount;
    @Getter private String botBanReason;
    @Getter private String publicChatBanReason;
    @Getter private String language;

    public SQLUser(long userId, String username, Instant createdAt, int sentMessageCount, int sentCommandCount, int sentPublicMessageCount, int sentUnknownCommandCount, int sentCustomCommandCount, String botBanReason, String publicChatBanReason, String language) {
        this.userId = userId;
        this.username = username;
        this.createdAt = createdAt;
        this.sentMessageCount = sentMessageCount;
        this.sentCommandCount = sentCommandCount;
        this.sentPublicMessageCount = sentPublicMessageCount;
        this.sentUnknownCommandCount = sentUnknownCommandCount;
        this.sentCustomCommandCount = sentCustomCommandCount;
        this.botBanReason = botBanReason;
        this.publicChatBanReason = publicChatBanReason;
        this.language = language;
    }

}
