package DataManager;

import java.time.Instant;
import java.util.Optional;

import DataManager.DataManager.*;
import lombok.Getter;

public class SQLBotSuggestion {

    @Getter private String title = "";
    @Getter private String content = "";
    @Getter private SuggestionStatus status = SuggestionStatus.CREATED;
    private String detailedStatus = "";
    @Getter private int id = 0;
    @Getter private long creatorId = 0L;
    @Getter private Instant createdAt = Instant.MIN;
    @Getter private Instant lastUpdate = Instant.MIN;

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

    public Optional<String> getDetailedStatus() {return Optional.ofNullable(detailedStatus);}

}
