package DataManager;

import java.time.Instant;

public class SQLUser {

    private long userId;
    private String username;
    private Instant createdAt;
    private int sentMessageCount;
    private int sentCommandCount;
    private int sentPublicMessageCount;
    private int sentUnknownCommandCount;
    private int sentCustomCommandCount;
    private String botBanReason;
    private String publicChatBanReason;
    private String language;

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

    public long getUserId() {return userId;}
    public String getUsername() {return username;}
    public Instant getCreatedAt() {return createdAt;}
    public int getSentMessageCount() {return sentMessageCount;}
    public int getSentCommandCount() {return sentCommandCount;}
    public int getSentPublicMessageCount() {return sentPublicMessageCount;}
    public int getSentUnknownCommandCount() {return sentUnknownCommandCount;}
    public int getSentCustomCommandCount() {return sentCustomCommandCount;}
    public String getBotBanReason() {return botBanReason;}
    public String getPublicChatBanReason() {return publicChatBanReason;}
    public String getLanguage() {return language;}

}
