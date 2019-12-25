package Main;

import discord4j.core.object.entity.GuildMessageChannel;
import discord4j.core.object.entity.MessageChannel;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class RatelimitUtils{

    public static class RatelimitChannel {
        public static final RatelimitChannel FEEDBACK = new RatelimitChannel(0);
        public static final RatelimitChannel TEST = new RatelimitChannel(1);

        private int channel;
        RatelimitChannel(int channel){
            this.channel = channel;
        }
        public int getChannel(){return channel;}

        private static int currentChannel = 500;
        public static RatelimitChannel getNextChannel(){
            return new RatelimitChannel(currentChannel++);
        }
    }

    public static class Ratelimit {
        @Getter private int amount = 1000;
        @Getter private int timespan = 0;
        @Getter private RatelimitChannel channel;
        @Getter private boolean perUser = false;
        @Getter private boolean perChannel = false;
        @Getter private boolean perGuild = false;
        public Ratelimit(int amount, int timespan){
            this.amount = amount;
            this.timespan = timespan;
            this.channel = RatelimitChannel.getNextChannel();
        }
        public Ratelimit perUser(boolean perUser){this.perUser = perUser; return this;}
        public Ratelimit perChannel(boolean perChannel){this.perChannel = perChannel; return this;}
        public Ratelimit perGuild(boolean perGuild){this.perGuild = perGuild; return this;}
        public Ratelimit perUser(){return perUser(true);}
        public Ratelimit perChannel(){return perChannel(true);}
        public Ratelimit perGuild(){return perGuild(true);}
    }




    public static boolean isRatelimited(Ratelimit ratelimit, long gId, long cId, long uId, MessageChannel channel, String lang){
        if(ratelimit.isPerGuild() && ratelimit.isPerUser()) {
            return isMemberRateLimited(gId, uId, ratelimit.getChannel(), ratelimit.getAmount(), ratelimit.getTimespan(), channel, lang);
        }
        if(ratelimit.isPerChannel() && ratelimit.isPerUser()){

        }
        
        return false;
    }





    private static Map<Long, Map<Integer, Integer>> userRatelimits = new HashMap<>();
    /**
     * Tells whether an action is ratelimited or not. If it is ratelimited {@code true} is returned, otherwise it adds the request to the counter and returns false
     * @param userId   The id of the user
     * @return Whether the action is ratelimited or not
     */
    public static boolean isUserRatelimited(long userId, Ratelimit ratelimit){
        Map<Integer, Integer> ratelimits = userRatelimits.computeIfAbsent(userId, k -> new HashMap<>());
        int uses = ratelimits.getOrDefault(ratelimit.getChannel().getChannel(), 0);
        if(uses >= ratelimit.getAmount()) return true;
        ratelimits.put(ratelimit.getChannel().getChannel(), uses + 1);
        Timer t = new Timer();
        t.schedule(new TimerTask(){
            @Override
            public void run(){
                ratelimits.put(ratelimit.getChannel().getChannel(), ratelimits.get(ratelimit.getChannel().getChannel()) - 1);
            }
        }, ratelimit.getTimespan());
        return false;
    }
    public static boolean isUserRatelimited(long gId, Ratelimit ratelimit, MessageChannel c, String lang){
        if(ratelimit == null) return false;
        if(!isUserRatelimited(gId, ratelimit)) return false;
        c.createEmbed(LocaleManager.getLanguageMessage(lang, "ratelimited", "" + ratelimit.amount, "" + ratelimit.timespan)).subscribe();
        return true;
    }

    private static Map<Long, Map<Long, Map<Integer, Integer>>> memberRatelimits = new HashMap<>();
    /**
     * Tells whether an action is ratelimited or not. If it is ratelimited {@code true} is returned, otherwise it adds the request to the counter and returns false
     * @param userId   The id of the user
     * @param channel  The identifier for that specific action
     * @param amount   The count of requests allowed in the timespan
     * @param timespan Amount in milliseconds
     * @return Whether the action is ratelimited or not
     */
    public static boolean isMemberRateLimited(long guildId, long userId, RatelimitChannel channel, int amount, int timespan){
        Map<Long, Map<Integer, Integer>> guildRatelimits = memberRatelimits.getOrDefault(guildId, new HashMap<>());
        if(!memberRatelimits.containsKey(guildId)) memberRatelimits.put(guildId, guildRatelimits);
        Map<Integer, Integer> userRatelimits = guildRatelimits.getOrDefault(userId, new HashMap<>());
        if(!guildRatelimits.containsKey(userId)) guildRatelimits.put(userId, userRatelimits);
        int uses = userRatelimits.getOrDefault(channel.getChannel(), 0);
        if(uses >= amount) return true;
        userRatelimits.put(channel.getChannel(), uses + 1);
        Timer t = new Timer();
        t.schedule(new TimerTask(){
            @Override
            public void run(){
                userRatelimits.put(channel.getChannel(), userRatelimits.get(channel.getChannel()) - 1);
            }
        }, timespan);
        return false;
    }



    /**
     * Tells whether an action is ratelimited or not. If it is ratelimited {@code true} is returned, otherwise it adds the request to the counter and returns false
     * @param guildId  The id of the guild
     * @param userId   The id of the user
     * @param channel  The identifier for that specific action
     * @param amount   The count of requests allowed in the timespan
     * @param timespan Amount in milliseconds
     * @param c        The {@link MessageChannel} in which a warning should be sent
     * @param lang     The language that should be used
     * @return Whether the action is ratelimited or not
     */
    public static boolean isMemberRateLimited(long guildId, long userId, RatelimitChannel channel, int amount, int timespan, MessageChannel c, String lang){
        Map<Long, Map<Integer, Integer>> guildRatelimits = memberRatelimits.getOrDefault(guildId, new HashMap<>());
        if(!memberRatelimits.containsKey(guildId)) memberRatelimits.put(guildId, guildRatelimits);
        Map<Integer, Integer> userRatelimits = guildRatelimits.getOrDefault(userId, new HashMap<>());
        if(!guildRatelimits.containsKey(userId)) guildRatelimits.put(userId, userRatelimits);
        int uses = userRatelimits.getOrDefault(channel.getChannel(), 0);
        if(uses >= amount){
            c.createEmbed(LocaleManager.getLanguageMessage(lang, "ratelimited", "" + amount, "" + timespan)).subscribe();
            return true;
        }
        userRatelimits.put(channel.getChannel(), uses + 1);
        Timer t = new Timer();
        t.schedule(new TimerTask(){
            @Override
            public void run(){
                userRatelimits.put(channel.getChannel(), userRatelimits.get(channel.getChannel()) - 1);
            }
        }, timespan);
        return false;
    }




    private static Map<Long, Map<Integer, Integer>> guildRatelimits = new HashMap<>();
    public static boolean isGuildRatelimited(long gId, Ratelimit ratelimit){
        if(ratelimit == null) return false;
        Map<Integer, Integer> ratelimits = guildRatelimits.computeIfAbsent(gId, k -> new HashMap<>());
        int uses = ratelimits.getOrDefault(ratelimit.channel.getChannel(), 0);
        if(uses >= ratelimit.amount) return true;
        ratelimits.put(ratelimit.channel.getChannel(), uses + 1);
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run(){
                ratelimits.put(ratelimit.channel.getChannel(), ratelimits.get(ratelimit.channel.getChannel()) - 1);
            }
        }, ratelimit.timespan);
        return false;
    }
    public static boolean isGuildRatelimited(long gId, Ratelimit ratelimit, MessageChannel c, String lang){
        if(ratelimit == null) return false;
        if(!isGuildRatelimited(gId, ratelimit)) return false;
        c.createEmbed(LocaleManager.getLanguageMessage(lang, "ratelimited", "" + ratelimit.amount, "" + ratelimit.timespan)).subscribe();
        return true;
    }


}
