package DataManager;

public class SQLStats{

    private int sentMessageCount = 0, receivedMessageCount = 0, receivedCommandCount = 0, receivedUnknownCommandCount = 0, receivedCustomCommandCount = 0, receivedDMCount = 0, sentDMCount = 0;

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

    public int getSentMessageCount(){return sentMessageCount;}
    public int getReceivedMessageCount(){return receivedMessageCount;}
    public int getReceivedCommandCount(){return receivedCommandCount;}
    public int getReceivedUnknownCommandCount(){return receivedUnknownCommandCount;}
    public int getReceivedCustomCommandCount(){return receivedCustomCommandCount;}
    public int getReceivedDMCount(){return receivedDMCount;}
    public int getSentDMCount(){return sentDMCount;}

}
