package Main;

import discord4j.core.object.entity.Guild;
import reactor.core.publisher.Mono;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.sql.*;
import java.time.Instant;

public class DataManager {

    private static final int port = 3306;
    private static final String url = "jdbc:mysql://localhost:" + port + "/l0c4lb0t?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";

    private static Connection con;

    public static enum Table {
        USERS("users"),
        MEMBERS("members"),
        GUILDS("guilds"),
        CUSTOM_COMMANDS("customcommands"),
        PERMISSIONS("permissions"),
        BANS("bans"),
        REACTION_ROLES("reactionroles"),
        PLAYLISTS("playlists"),
        DVCS("dvcs"),
        WHITELISTED_INVITES("whitelistedinvites");

        private final String name;
        Table(String name){
            this.name = name;
        }
        public String getName(){
            return this.name;
        }
    }

    public static void initialize(){
        try {
            con = DriverManager.getConnection(url, Tokens.MYSQL_USERNAME, Tokens.MYSQL_PASSWORD);
            String createMembersTable = "CREATE TABLE IF NOT EXISTS " + Table.MEMBERS.getName() + " (" +
                    "user_id BIGINT," +
                    "guild_id BIGINT," +
                    "is_nicked TINYINT," +
                    "guildname TINYTEXT," +
                    "joined_at TIMESTAMP," +
                    "sent_message_count INT," +
                    "sent_command_count INT," +
                    "sent_public_message_count INT," +
                    "sent_unknown_command_count INT," +
                    "sent_custom_command_count INT" +
                    ")";
            String createUsersTable = "CREATE TABLE IF NOT EXISTS " + Table.USERS.getName() + " (" +
                    "user_id BIGINT," +
                    "username TINYTEXT," +
                    "created_at TIMESTAMP," +
                    "sent_message_count INT," +
                    "sent_command_count INT," +
                    "sent_public_message_count INT," +
                    "sent_unknown_command_count INT," +
                    "sent_custom_command_count INT," +
                    "bot_ban_reason TEXT," +
                    "public_chat_ban_reason TEXT" +
                    ")";
            String createGuildsTable = "CREATE TABLE IF NOT EXISTS " + Table.GUILDS.getName() + " (" +
                    "guild_id BIGINT," +
                    "name TINYTEXT," +
                    "created_at TIMESTAMP," +
                    "joined_at TIMESTAMP," +
                    "owner_id BIGINT," +
                    "bot_prefix TINYTEXT," +
                    "language TINYTEXT," +
                    "public_channel_id BIGINT," +
                    "block_invites BOOLEAN," +
                    "sent_message_count INT," +
                    "sent_command_count INT," +
                    "sent_public_message_count INT," +
                    "sent_unknown_command_count INT," +
                    "sent_custom_command_count INT" +
                    ")";
            String createCustomCommandsTable = "CREATE TABLE IF NOT EXISTS " + Table.CUSTOM_COMMANDS.getName() + " (" +
                    "guild_id BIGINT," +
                    "command TINYTEXT," +
                    "response TEXT" +
                    ")";
            String createPermissionsTable = "CREATE TABLE IF NOT EXISTS " + Table.PERMISSIONS.getName() + " (" +
                    "guild_id BIGINT," +
                    "action TINYTEXT," +
                    "type TINYINT" +
                    ")";
            String createBansTable = "CREATE TABLE IF NOT EXISTS " + Table.BANS.getName() + " (" +
                    "user_id BIGINT," +
                    "reason TEXT," +
                    "date TIMESTAMP" +
                    ")";
            String createReactionRolesTable = "CREATE TABLE IF NOT EXISTS " + Table.REACTION_ROLES.getName() + " (" +
                    "guild_id BIGINT," +
                    "message_id BIGINT," +
                    "role_id BIGINT," +
                    "emoji TEXT" +
                    ")";
            String createPlaylistsTable = "CREATE TABLE IF NOT EXISTS " + Table.PLAYLISTS.getName() + " (" +
                    "user_id BIGINT," +
                    "name TINYTEXT," +
                    "songs ENUM" +
                    ")";
            String createDvcTable = "CREATE TABLE IF NOT EXISTS " + Table.DVCS.getName() + " (" +
                    "guild_id BIGINT," +
                    "name TINYTEXT" +
                    ")";
            String createWhitelistedInvitesTable = "CREATE TABLE IF NOT EXISTS " + Table.WHITELISTED_INVITES.getName() + " (" +
                    "guild_id BIGINT," +
                    "code TINYTEXT" +
                    ")";
            Statement stmt = con.createStatement();
            stmt.execute(createGuildsTable);
            stmt.execute(createMembersTable);
            stmt.execute(createUsersTable);
            stmt.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public static boolean guildIsRegistered(Long gId){
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT bot_prefix FROM " + Table.GUILDS.getName() + " WHERE guild_id=?");
            stmt.setLong(1, gId);
            ResultSet res = stmt.executeQuery();
            return res.next();
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public static Mono<Void> initializeGuild(Guild g){
        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO " + Table.GUILDS.getName() + " (" +
                    "guild_id," +                       // 1
                    "name," +                           // 2
                    "created_at," +                     // 3
                    "joined_at," +                      // 4
                    "owner_id," +                       // 5
                    "bot_prefix," +                     // 6
                    "public_channel_id," +              // 7
                    "block_invites," +                  // 8
                    "sent_message_count," +             // 9
                    "sent_command_count," +             // 10
                    "sent_public_message_count," +      // 11
                    "sent_unknown_command_count," +     // 12
                    "sent_custom_command_count" +       // 13
                    ")" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setLong(1, g.getId().asLong());
            stmt.setString(2, g.getName());
            stmt.setTimestamp(3, Timestamp.from(BotUtils.getSnowflakeCreationDate(g.getId().asLong())));
            stmt.setTimestamp(4, Timestamp.from(g.getJoinTime().orElse(Instant.now())));
            stmt.setLong(5, g.getOwnerId().asLong());
            stmt.setString(6, "=");
            stmt.setLong(7, 0L);
            stmt.setBoolean(8, false);
            stmt.setInt(9, 0);
            stmt.setInt(10, 0);
            stmt.setInt(11, 0);
            stmt.setInt(12, 0);
            stmt.setInt(13, 0);
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Mono.empty();
    }

    public static ResultSet getGuild(Long gId){
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT * FROM " + Table.GUILDS.getName() + " WHERE guild_id=?");
            stmt.setLong(1, gId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs;
        }catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public static boolean setGuild(Long gId, String key, Object value, JDBCType type){
        try {
            PreparedStatement stmt = con.prepareStatement("UPDATE " + Table.GUILDS.getName() + " SET " + key + "=? WHERE guild_id=?");
            stmt.setObject(1, value, type);
            stmt.setLong(2, gId);
            stmt.executeUpdate();
            return true;
        }catch (Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    public static ResultSet getUser(Long uId){
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT * FROM " + Table.USERS.getName() + " WHERE user_id=?");
            stmt.setLong(1, uId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static boolean setUser(Long uId, String key, Object value, JDBCType type){
        try {
            PreparedStatement stmt = con.prepareStatement("UPDATE " + Table.USERS.getName() + " SET " + key + "=? WHERE user_id=?");
            stmt.setObject(1, value, type);
            stmt.setLong(2, uId);
            stmt.executeUpdate();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static ResultSet getMember(Long gId, Long uId){
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT * FROM " + Table.MEMBERS.getName() + " WHERE guild_id=? AND user_id=?");
            stmt.setLong(1, gId);
            stmt.setLong(2, uId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static boolean setMember(Long gId, Long uId, String key, Object value, JDBCType type){
        try {
            PreparedStatement stmt = con.prepareStatement("UPDATE " + Table.USERS.getName() + " SET " + key + "=? WHERE guild_id=? AND user_id=?");
            stmt.setObject(1, value, type);
            stmt.setLong(2, gId);
            stmt.setLong(3, uId);
            stmt.executeUpdate();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

}