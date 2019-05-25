package DataManager;

import java.time.Instant;
import java.util.Optional;

import DataManager.DataManager.*;

public class SQLBotSuggestion {

    private String title = "";
    private String content = "";
    private SuggestionStatus status = SuggestionStatus.CREATED;
    private String detailedStatus = "";
    private int id = 0;
    private long creatorId = 0L;
    private Instant createdAt = Instant.MIN;
    private Instant lastUpdate = Instant.MIN;

    public SQLBotSuggestion(String title, String content, byte status, String detailedStatus, int id, long creatorId, Instant createdAt, Instant lastUpdate) {
        this.title = title;
        this.content = content;
        this.status = SuggestionStatus.getSuggestionStatus(status);
        this.detailedStatus = detailedStatus;
        this.id = id;
        this.creatorId = creatorId;
        this.createdAt = createdAt;
        this.lastUpdate = lastUpdate;
    }

    public String getTitle() {return title;}
    public String getContent() {return content;}
    public SuggestionStatus getStatus() {return status;}
    public Optional<String> getDetailedStatus() {return Optional.ofNullable(detailedStatus);}
    public int getId() {return id;}
    public long getCreatorId() {return creatorId;}
    public Instant getCreatedAt() {return createdAt;}
    public Instant getLastUpdate() {return lastUpdate;}

}
