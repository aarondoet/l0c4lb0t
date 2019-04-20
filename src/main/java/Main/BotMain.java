package Main;

import discord4j.core.DiscordClient;

public class BotMain {

    public static final DiscordClient client = BotUtils.getClient(Tokens.BOT_TOKEN);

    public static void main(String[] args){
        DataManager.initialize();
        /*
         * load stuff here
         */
        BotEvents.registerEvents(client);
        BotEvents.registerScriptEvents(client);
        DynamicVoiceChannels.initialize(client);
        client.login().block();
    }

}