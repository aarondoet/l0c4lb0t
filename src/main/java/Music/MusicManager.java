package Music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class MusicManager {

    private static Map<Long, VoiceConnection> guildConnections = new HashMap<>();
    private static AudioPlayerManager playerManager;
    private static AudioPlayer player;
    private static AudioProvider provider;

    public static void initialize(){
        playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(playerManager);
        player = playerManager.createPlayer();
        provider = new LavaPlayerAudioProvider(player);
    }

    public static Mono<Void> setGuildConnection(Long gId, VoiceConnection con){guildConnections.put(gId, con); return Mono.empty();}
    public static VoiceConnection getGuildConnection(Long gId){return guildConnections.get(gId);}

    public static AudioPlayerManager getPlayerManager(){return playerManager;}
    public static AudioPlayer getPlayer() {return player;}
    public static AudioProvider getProvider() {return provider;}
}
