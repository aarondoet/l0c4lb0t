package Main;

import discord4j.core.object.entity.MessageChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class RatelimitUtils{

    public enum RatelimitChannel {
        FEEDBACK(0),
        TEST(1)
        ;
        private int channel;
        RatelimitChannel(int channel){
            this.channel = channel;
        }
        public int getChannel(){return channel;}
    }




    private static Map<Integer, Integer> ratelimits = new HashMap<>();
    /**
     * Tells whether an action is ratelimited or not. If it is ratelimited {@code true} is returned, otherwise it adds the request to the counter and returns false
     * @param channel  The identifier for that specific action
     * @param amount   The count of requests allowed in the timespan
     * @param timespan Amount in milliseconds
     * @return Whether the action is ratelimited or not
     */
    public static boolean isRateLimited(RatelimitChannel channel, int amount, int timespan){
        int uses = ratelimits.getOrDefault(channel.getChannel(), 0);
        if(uses >= amount) return true;
        ratelimits.put(channel.getChannel(), uses + 1);
        Timer t = new Timer();
        t.schedule(new TimerTask(){
            @Override
            public void run(){
                ratelimits.put(channel.getChannel(), ratelimits.get(channel.getChannel()) - 1);
            }
        }, timespan);
        return false;
    }

    private static Map<Long, Map<Integer, Integer>> userRatelimits = new HashMap<>();
    /**
     * Tells whether an action is ratelimited or not. If it is ratelimited {@code true} is returned, otherwise it adds the request to the counter and returns false
     * @param userId   The id of the user
     * @param channel  The identifier for that specific action
     * @param amount   The count of requests allowed in the timespan
     * @param timespan Amount in milliseconds
     * @return Whether the action is ratelimited or not
     */
    public static boolean isUserRateLimited(long userId, RatelimitChannel channel, int amount, int timespan){
        Map<Integer, Integer> ratelimits = userRatelimits.getOrDefault(userId, new HashMap<>());
        if(!userRatelimits.containsKey(userId)){
            userRatelimits.put(userId, ratelimits);
        }
        int uses = ratelimits.getOrDefault(channel.getChannel(), 0);
        if(uses >= amount) return true;
        ratelimits.put(channel.getChannel(), uses + 1);
        Timer t = new Timer();
        t.schedule(new TimerTask(){
            @Override
            public void run(){
                ratelimits.put(channel.getChannel(), ratelimits.get(channel.getChannel()) - 1);
            }
        }, timespan);
        return false;
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
            c.createMessage(LocaleManager.getLanguageMessage(lang, "ratelimited", "" + amount, "" + timespan)).subscribe();
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





}
