package Patreon;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PatreonManager {

    public static List<Patron> patrons = new ArrayList<>();

    public static boolean isPatronGuild(Long gId){
        return patrons.stream().anyMatch(patron -> patron.getGuildId() == gId);
    }

    public static boolean isPatron(Long uId){
        return patrons.stream().anyMatch(patron -> patron.getUserId() == uId);
    }

    public static List<Patron> getPublicPatrons(){
        return patrons.stream().filter(Patron::isPublic).collect(Collectors.toList());
    }

    public static List<Long> getPatronGuilds(){
        return patrons.stream().filter(patron -> patron.getGuildId() > 0).map(Patron::getGuildId).collect(Collectors.toList());
    }

    public static List<Long> getPatronUsers(){
        return patrons.stream().filter(patron -> patron.getUserId() > 0).map(Patron::getUserId).collect(Collectors.toList());
    }

    public static int getPatronCount(){
        return patrons.size();
    }

    public static long getPublicPatronCount(){
        return patrons.stream().filter(Patron::isPublic).count();
    }

    public static long getPrivatePatronCount(){
        return patrons.stream().filter(Patron::isPrivate).count();
    }

}
