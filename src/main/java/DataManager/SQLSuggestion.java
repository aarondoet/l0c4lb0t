package DataManager;

import java.time.Instant;
import java.util.Optional;
import DataManager.DataManager.SuggestionStatus;

public class SQLSuggestion {

    private long guildId = 0L;
    private String title = "";
    private String content = "";
    private SuggestionStatus status = SuggestionStatus.CREATED;
    private String detailedStatus = "";
    private int id = 0;
    private long creatorId = 0L;
    private Instant createdAt = Instant.MIN;
    private Instant lastUpdate = Instant.MIN;

    public SQLSuggestion(long guildId, String title, String content, byte status, String detailedStatus, int id, long creatorId, Instant createdAt, Instant lastUpdate) {
        this.guildId = guildId;
        this.title = title;
        this.content = content;
        this.status = SuggestionStatus.getSuggestionStatus(status);
        this.detailedStatus = detailedStatus;
        this.id = id;
        this.creatorId = creatorId;
        this.createdAt = createdAt;
        this.lastUpdate = lastUpdate;
    }

    public long getGuildId() {return guildId;}
    public String getTitle() {return title;}
    public String getContent() {return content;}
    public SuggestionStatus getStatus() {return status;}
    public Optional<String> getDetailedStatus() {return Optional.ofNullable(detailedStatus);}
    public int getId() {return id;}
    public long getCreatorId() {return creatorId;}
    public Instant getCreatedAt() {return createdAt;}
    public Instant getLastUpdate() {return lastUpdate;}

}
