package DataManager;

import java.time.Instant;
import java.util.Optional;
import DataManager.DataManager.SuggestionStatus;
import lombok.Getter;

public class SQLFeedback{

    public enum FeedbackType {
        SUGGESTION(0, "Suggestion"),
        ISSUE(1, "Issue"),
        BUG(2, "Bug"),

        OTHER(10, "Other");

        private int value;
        private String name;
        FeedbackType(int value, String name){
            this.value = value;
            this.name = name;
        }
        public int getValue(){return value;}
        public String getName(){return name;}

        public static FeedbackType getByValue(int val){
            for(FeedbackType t : FeedbackType.values())
                if(t.getValue() == val)
                    return t;
            return null;
        }
        public static FeedbackType getFeedbackType(String val){
            if(val.matches("\\d+")){
                try{
                    int v = Integer.parseInt(val);
                    for(FeedbackType t : FeedbackType.values())
                        if(v == t.getValue())
                            return t;
                }catch(Exception ex){}
            }
            for(FeedbackType t : FeedbackType.values())
                if(t.getName().equalsIgnoreCase(val))
                    return t;
            return null;
        }
        public static FeedbackType getFeedbackType(String val, FeedbackType defaultValue){
            FeedbackType t = getFeedbackType(val);
            if(t == null) return defaultValue; else return t;
        }
    }

    @Getter private long guildId = 0L;
    @Getter private String title = "";
    @Getter private String content = "";
    @Getter private SuggestionStatus status = SuggestionStatus.CREATED;
    private String detailedStatus = "";
    @Getter private int id = 0;
    @Getter private long creatorId = 0L;
    @Getter private Instant createdAt = Instant.MIN;
    @Getter private Instant lastUpdate = Instant.MIN;
    @Getter private FeedbackType type;

    public SQLFeedback(long guildId, String title, String content, byte status, String detailedStatus, int id, long creatorId, Instant createdAt, Instant lastUpdate, FeedbackType type) {
        this.guildId = guildId;
        this.title = title;
        this.content = content;
        this.status = SuggestionStatus.getSuggestionStatus(status);
        this.detailedStatus = detailedStatus;
        this.id = id;
        this.creatorId = creatorId;
        this.createdAt = createdAt;
        this.lastUpdate = lastUpdate;
        this.type = type;
    }

    public Optional<String> getDetailedStatus() {return Optional.ofNullable(detailedStatus);}

}
