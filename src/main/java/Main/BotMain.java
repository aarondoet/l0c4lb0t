package Main;

import DataManager.DataManager;
import Music.MusicManager;
import discord4j.core.DiscordClient;

import java.util.ArrayList;
import java.util.List;

public class BotMain {

    public static final DiscordClient client = BotUtils.getClient(Tokens.BOT_TOKEN);

    public static void main(String[] args){
        LocaleManager.initialize();
        DataManager.initialize();
        MusicManager.initialize();
        /*
         * load stuff here
         */
        BotEvents.registerEvents(client);
        BotEvents.registerScriptEvents(client);
        DynamicVoiceChannels.initialize(client);
        client.login().block();
    }

}