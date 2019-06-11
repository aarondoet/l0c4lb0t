package Patreon;

public class Patron {

    private long guildId = 0L;
    private long userId = 0L;
    private String name = "";
    private boolean hidden = true;
    private double donationAmount = 0d;
    private int tier = 0;

    public Patron(long guildId, long userId, String name, boolean hidden, double donationAmount, int tier) {
        this.guildId = guildId;
        this.userId = userId;
        this.name = name;
        this.hidden = hidden;
        this.donationAmount = donationAmount;
        this.tier = tier;
    }

    public long getGuildId() {return guildId;}
    public long getUserId() {return userId;}
    public String getName() {return name;}
    public boolean isPrivate() {return hidden;}
    public boolean isPublic() {return !hidden;}
    public double getDonationAmount() {return donationAmount;}
    public int getTier() {return tier;}

}
