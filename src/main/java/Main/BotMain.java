package Main;

import CommandHandling.BotCommands;
import DataManager.DataManager;
import Music.MusicManager;
import WebAPI.HTTPServer;
import discord4j.core.DiscordClient;

public class BotMain {

    public static final DiscordClient client = BotUtils.getClient(Tokens.BOT_TOKEN);

    public static void main(String[] args){
        PrivateFunctions.addAllPatrons();
        LocaleManager.initialize();
        DataManager.initialize();
        MusicManager.initialize();
        BotCommands.registerCommands();
        BotEvents.registerEvents(client);
        BotEvents.registerScriptEvents(client);
        DynamicVoiceChannels.initialize(client);
        BotUtils.startAutoBackups(client);
        BotUtils.startRichPresences(client);
        //HTTPServer.startServer();
        client.login().block();
    }

}