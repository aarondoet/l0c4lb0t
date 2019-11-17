package DataManager;

import lombok.Getter;

public class SQLStats{

    @Getter private int sentMessageCount = 0;
    @Getter private int receivedMessageCount = 0;
    @Getter private int receivedCommandCount = 0;
    @Getter private int receivedUnknownCommandCount = 0;
    @Getter private int receivedCustomCommandCount = 0;
    @Getter private int receivedDMCount = 0;
    @Getter private int sentDMCount = 0;

    public SQLStats(){}
    public SQLStats(int sentMessageCount, int receivedMessageCount, int receivedCommandCount, int receivedUnknownCommandCount, int receivedCustomCommandCount, int receivedDMCount, int sentDMCount){
        this.sentMessageCount = sentMessageCount;
        this.receivedMessageCount = receivedMessageCount;
        this.receivedCommandCount = receivedCommandCount;
        this.receivedUnknownCommandCount = receivedUnknownCommandCount;
        this.receivedCustomCommandCount = receivedCustomCommandCount;
        this.receivedDMCount = receivedDMCount;
        this.sentDMCount = sentDMCount;
    }

}
