package LyricsCrawler;

public class Song {

    private String title, artist, url, lyrics;
    public Song(String title, String artist, String url, String lyrics){
        this.artist = artist;
        this.lyrics = lyrics;
        this.title = title;
        this.url = url;
    }

    public String getArtist() {
        return artist;
    }
    public String getLyrics() {
        return lyrics;
    }
    public String getTitle() {
        return title;
    }
    public String getUrl() {
        return url;
    }
}
