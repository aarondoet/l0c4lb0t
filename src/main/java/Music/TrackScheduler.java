package Music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class TrackScheduler implements AudioLoadResultHandler {

    private final AudioPlayer player;

    public TrackScheduler(final AudioPlayer player){
        this.player = player;
    }

    @Override
    public void trackLoaded(final AudioTrack track){
        player.playTrack(track);
    }

    @Override
    public void playlistLoaded(final AudioPlaylist playlist){
        // TODO: implement
    }

    @Override
    public void noMatches(){
        // TODO: implement
    }

    @Override
    public void loadFailed(final FriendlyException exception){
        // TODO: implement
    }

}
