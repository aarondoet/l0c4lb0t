package Main;

import DataManager.DataManager;
import discord4j.core.DiscordClient;

import java.util.ArrayList;
import java.util.List;

public class BotMain {

    public static final DiscordClient client = BotUtils.getClient(Tokens.BOT_TOKEN);

    public static void main(String[] args){
        LocaleManager.initialize();
        DataManager.initialize();
        /*
         * load stuff here
         */
        BotEvents.registerEvents(client);
        BotEvents.registerScriptEvents(client);
        DynamicVoiceChannels.initialize(client);
        client.login().block();
    }

    static List<String> parse(String input){
        List<String> args = new ArrayList<>();
        boolean escaped = false;
        boolean inQuotes = false;
        boolean endedQuote = false;
        List<Integer> toDelete = new ArrayList<>();
        String currArg = "";
        for(char c : input.toCharArray()){
            if(endedQuote){
                endedQuote = false;
                if(c == ' ') continue;
            }
            if(c == ' ' && !inQuotes){
                args.add(currArg);
                currArg = "";
                escaped = false;
                continue;
            }
            if(c == '"' && inQuotes && !escaped){
                args.add(currArg);
                currArg = "";
                inQuotes = false;
                endedQuote = true;
                continue;
            }
            if(c == '\\' && !escaped){
                escaped = true;
                continue;
            }
            if(c == '"' && !escaped && !inQuotes){
                inQuotes = true;
                continue;
            }
            escaped = false;
            currArg += "" + c;
        }
        args.add(currArg);
        return args;
    }

}