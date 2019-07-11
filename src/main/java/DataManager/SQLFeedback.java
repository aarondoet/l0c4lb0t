package DataManager;

import java.time.Instant;
import java.util.Optional;
import DataManager.DataManager.SuggestionStatus;

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

    private long guildId = 0L;
    private String title = "";
    private String content = "";
    private SuggestionStatus status = SuggestionStatus.CREATED;
    private String detailedStatus = "";
    private int id = 0;
    private long creatorId = 0L;
    private Instant createdAt = Instant.MIN;
    private Instant lastUpdate = Instant.MIN;
    private FeedbackType type;

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

    public long getGuildId() {return guildId;}
    public String getTitle() {return title;}
    public String getContent() {return content;}
    public SuggestionStatus getStatus() {return status;}
    public Optional<String> getDetailedStatus() {return Optional.ofNullable(detailedStatus);}
    public int getId() {return id;}
    public long getCreatorId() {return creatorId;}
    public Instant getCreatedAt() {return createdAt;}
    public Instant getLastUpdate() {return lastUpdate;}
    public FeedbackType getType() {return type;}

}
