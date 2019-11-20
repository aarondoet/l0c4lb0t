package Main;

import DataManager.*;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildMessageChannel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.util.Permission;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
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
        public static PermissionType fromInt(int type){
            for(PermissionType pt : PermissionType.values())
                if(pt.getType() == type)
                    return pt;
            return null;
        }
    }

    public static boolean hasPermission(Guild g, Member m, String cmd, boolean everyone, Permission... defaultPerms){
        if(m == null) return false;
        if(cmd == null) return true;
        if(BotUtils.getBotAdmins().contains(m.getId().asLong())) return true;
        if(g.getOwnerId().asLong() == m.getId().asLong()) return true;

        boolean has = m.getBasePermissions().map(perms -> perms.contains(Permission.ADMINISTRATOR)).blockOptional().orElse(false);

        List<Long> roleWhitelist = new ArrayList<>();
        List<Long> roleBlacklist = new ArrayList<>();
        List<Long> userWhitelist = new ArrayList<>();
        List<Long> userBlacklist = new ArrayList<>();
        // blacklist only
        List<Long> roleEverything = new ArrayList<>();
        List<Long> userEverything = new ArrayList<>();

        try {
            List<SQLPermissions> perms = DataManager.getPermissions(g.getId().asLong(), cmd);
            for(SQLPermissions perm : perms){
                int type = perm.getType();
                long hId = perm.getHolderId();
                if(type == 0)
                    roleWhitelist.add(hId);
                else if(type == 1)
                    userWhitelist.add(hId);
                else if(type == 2)
                    roleBlacklist.add(hId);
                else if(type == 3)
                    userBlacklist.add(hId);
            }
            List<SQLPermissions> everyonePerms = DataManager.getPermissions(g.getId().asLong(), "everything");
            for(SQLPermissions perm : everyonePerms){
                int type = perm.getType();
                long hId = perm.getHolderId();
                if(type == 2)
                    roleEverything.add(hId);
                else if(type == 3)
                    userEverything.add(hId);
            }
        }catch (Exception ex){
            return false;
        }

        if(userEverything.contains(m.getId().asLong())) return false;
        if(m.getRoles()
                .filter(r -> roleEverything.contains(r.getId().asLong()))
                .next()
                .blockOptional().isPresent())
            return false;

        if(m.getRoles()
                .filter(r -> roleWhitelist.contains(r.getId().asLong()))
                .next()
                .blockOptional().isPresent())
            has = true;
        if(m.getRoles()
                .filter(r -> roleBlacklist.contains(r.getId().asLong()))
                .next()
                .blockOptional().isPresent())
            has = false;
        if(userWhitelist.contains(m.getId().asLong())) has = true;
        if(userBlacklist.contains(m.getId().asLong())) has = false;

        if(userWhitelist.isEmpty() && roleWhitelist.isEmpty()){
            boolean blacklisted = false;
            if(m.getRoles()
                    .filter(r -> roleBlacklist.contains(r.getId().asLong()))
                    .next()
                    .blockOptional().isPresent())
                blacklisted = true;
            if(userBlacklist.contains(m.getId().asLong())) blacklisted = true;
            if(!blacklisted){
                if(defaultPerms.length > 0 && m.getBasePermissions().map(perms -> perms.containsAll(Arrays.asList(defaultPerms))).blockOptional().orElse(false)) has = true;
                if(everyone) has = true;
            }
        }

        return has;
    }

    public static boolean hasPermission(Guild g, Member m, GuildMessageChannel c, String cmd, boolean everyone, Permission... defaultPerms){
        if(m == null) return false;
        if(cmd == null) return true;
        if(BotUtils.getBotAdmins().contains(m.getId().asLong())) return true;
        if(g.getOwnerId().asLong() == m.getId().asLong()) return true;

        boolean has = m.getBasePermissions().map(perms -> perms.contains(Permission.ADMINISTRATOR)).blockOptional().orElse(false);

        List<Long> roleWhitelist = new ArrayList<>();
        List<Long> roleBlacklist = new ArrayList<>();
        List<Long> userWhitelist = new ArrayList<>();
        List<Long> userBlacklist = new ArrayList<>();
        // blacklist only
        List<Long> roleEverything = new ArrayList<>();
        List<Long> userEverything = new ArrayList<>();

        try {
            List<SQLPermissions> perms = DataManager.getPermissions(g.getId().asLong(), cmd);
            for(SQLPermissions perm : perms){
                int type = perm.getType();
                long hId = perm.getHolderId();
                if(type == 0)
                    roleWhitelist.add(hId);
                else if(type == 1)
                    userWhitelist.add(hId);
                else if(type == 2)
                    roleBlacklist.add(hId);
                else if(type == 3)
                    userBlacklist.add(hId);
            }
            List<SQLPermissions> everyonePerms = DataManager.getPermissions(g.getId().asLong(), "everything");
            for(SQLPermissions perm : everyonePerms){
                int type = perm.getType();
                long hId = perm.getHolderId();
                if(type == 2)
                    roleEverything.add(hId);
                else if(type == 3)
                    userEverything.add(hId);
            }
        }catch (Exception ex){
            return false;
        }

        if(userEverything.contains(m.getId().asLong())) return false;
        if(m.getRoles()
                .filter(r -> roleEverything.contains(r.getId().asLong()))
                .next()
                .blockOptional().isPresent())
            return false;

        if(m.getRoles()
                .filter(r -> roleWhitelist.contains(r.getId().asLong()))
                .next()
                .blockOptional().isPresent())
            has = true;
        if(m.getRoles()
                .filter(r -> roleBlacklist.contains(r.getId().asLong()))
                .next()
                .blockOptional().isPresent())
            has = false;
        if(userWhitelist.contains(m.getId().asLong())) has = true;
        if(userBlacklist.contains(m.getId().asLong())) has = false;

        if(userWhitelist.isEmpty() && roleWhitelist.isEmpty()){
            boolean blacklisted = false;
            if(m.getRoles()
                    .filter(r -> roleBlacklist.contains(r.getId().asLong()))
                    .next()
                    .blockOptional().isPresent())
                blacklisted = true;
            if(userBlacklist.contains(m.getId().asLong())) blacklisted = true;
            if(!blacklisted){
                if(defaultPerms.length > 0 && c.getEffectivePermissions(m.getId()).map(perms -> perms.containsAll(Arrays.asList(defaultPerms))).blockOptional().orElse(false)) has = true;
                if(everyone) has = true;
            }
        }

        return has;
    }

}