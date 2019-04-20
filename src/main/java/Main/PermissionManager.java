package Main;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.util.Permission;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

public class PermissionManager {

    public enum PermissionType {
        USER_BLACKLIST(0),
        USER_WHITELIST(1),
        ROLE_BLACKLIST(2),
        ROLE_WHITELIST(3);

        private int type;
        PermissionType(int type){
            this.type = type;
        }
        public int getType() {
            return type;
        }
    }

    public static boolean hasPermission(Guild g, Member m, String cmd, boolean everyone, Permission... defaultPerms){
        if(m == null) return false;
        if(BotUtils.getBotAdmins().contains(m.getId().asLong())) return true;
        if(m.getBasePermissions().block().contains(Permission.ADMINISTRATOR)) return true;

        boolean has = false;

        List<Long> roleWhitelist = null;
        List<Long> roleBlacklist = null;
        List<Long> userWhitelist = null;
        List<Long> userBlacklist = null;
        // blacklist only
        List<Long> roleEverything = null;
        List<Long> userEverything = null;

        if(userEverything.contains(m.getId().asLong())) return false;
        if(m.getRoles()
                .filter(r -> roleEverything.contains(r.getId().asLong()))
                .flatMap(r -> Mono.just(true))
                .next()
                .blockOptional().isPresent())
            return false;

        if(m.getRoles()
                .filter(r -> roleWhitelist.contains(r.getId().asLong()))
                .flatMap(r -> Mono.just(true))
                .next()
                .blockOptional().isPresent())
            has = true;
        if(m.getRoles()
                .filter(r -> roleBlacklist.contains(r.getId().asLong()))
                .flatMap(r -> Mono.just(true))
                .next()
                .blockOptional().isPresent())
            has = false;
        if(userWhitelist.contains(m.getId().asLong())) has = true;
        if(userBlacklist.contains(m.getId().asLong())) has = false;

        if(userWhitelist.isEmpty() && roleWhitelist.isEmpty()){
            boolean blacklisted = false;
            if(m.getRoles()
                    .filter(r -> roleBlacklist.contains(r.getId().asLong()))
                    .flatMap(r -> Mono.just(true))
                    .next()
                    .blockOptional().isPresent())
                blacklisted = true;
            if(userBlacklist.contains(m.getId().asLong())) blacklisted = true;
            if(!blacklisted){
                if(m.getBasePermissions().block().containsAll(Arrays.asList(defaultPerms))) has = true;
                if(everyone) has = true;
            }
        }

        return has;
    }

}