package Main;

import DataManager.DataManager;
import Music.MusicManager;
import discord4j.core.DiscordClient;

public class BotMain {

    public static final DiscordClient client = BotUtils.getClient(Tokens.BOT_TOKEN);

    public static void main(String[] args){
        PrivateFunctions.addAllPatrons();
        LocaleManager.initialize();
        DataManager.initialize();
        MusicManager.initialize();
        BotEvents.registerEvents(client);
        BotEvents.registerScriptEvents(client);
        DynamicVoiceChannels.initialize(client);
        client.login().block();
    }

}