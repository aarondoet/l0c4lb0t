package DataManager;

import Main.BotUtils;
import Main.Tokens;
import Scripts.ScriptExecutor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import discord4j.core.object.entity.*;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Image;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import org.apache.commons.dbutils.DbUtils;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataManager {

    private static final String schemeName = "l0c4lb0t";
    private static final int port = 3306;
    private static final String url = "jdbc:mysql://localhost:" + port + "/" + schemeName + "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";

    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;
    static {
        config.setJdbcUrl(url);
        config.setUsername(Tokens.MYSQL_USERNAME);
        config.setPassword(Tokens.MYSQL_PASSWORD);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        ds = new HikariDataSource(config);
    }
    private static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public enum Table {
        USERS("users"),
        MEMBERS("members"),
        GUILDS("guilds"),
        CUSTOM_COMMANDS("customcommands"),
        PERMISSIONS("permissions"),
        BANS("bans"),
        REACTION_ROLES("reactionroles"),
        PLAYLISTS("playlists"),
        DVCS("dvcs"),
        WHITELISTED_INVITES("whitelistedinvites"),
        BLOCKED_CHANNELS("blockedchannels"),
        SCRIPTS("scripts"),
        BOT_STATS("botstats"),
        BOT_SUGGESTIONS("botsuggestions"),
        BOT_SUGGESTION_NOTIFICATIONS("botsuggestionnotifications"),
        SUGGESTIONS("suggestions"),
        SUGGESTION_NOTIFICATIONS("suggestionnotifications"),

        BACKUP_GENERAL("backupgeneral"),
        BACKUP_ROLES("backuproles"),
        BACKUP_CHANNELS("backupchannels"),
        BACKUP_CHANNEL_PERMISSION_OVERWRITES("backuppermissionoverwrites"),
        BACKUP_USERS("backupusers"),
        BACKUP_USER_ROLES("backupuserroles"),
        BACKUP_BANS("backupbans");

        private final String name;
        Table(String name){this.name = name;}
        public String getName(){return this.name;}
    }

    public enum SuggestionStatus {
        CREATED((byte)0, "created", new Color(69, 69, 69), ReactionEmoji.unicode("\uD83D\uDDD2")),
        ACCEPTED((byte)1, "accepted", new Color(0, 135, 189), ReactionEmoji.unicode("\u2611")),
        REJECTED((byte)2, "rejected", new Color(255, 82, 70), ReactionEmoji.unicode("\u274C")),
        DELETED((byte)3, "deleted", new Color(237, 10, 14), ReactionEmoji.unicode("")),
        IN_PROGRESS((byte)4, "in progress", new Color(31, 117, 254), ReactionEmoji.unicode("\u23F3")),
        IMPLEMENTED((byte)5, "implemented", new Color(86, 130, 3), ReactionEmoji.unicode("\u2705"));

        private final byte status;
        private final String name;
        private final Color color;
        private final ReactionEmoji emoji;
        SuggestionStatus(byte status, String name, Color color, ReactionEmoji emoji){this.status = status;this.name = name;this.color = color;this.emoji = emoji;}
        public byte getStatus(){return this.status;}
        public String getName(){return this.name;}
        public Color getColor(){return this.color;}
        public ReactionEmoji getEmoji(){return this.emoji;}

        public static SuggestionStatus getSuggestionStatus(byte status){
            for(SuggestionStatus s : SuggestionStatus.values())
                if(s.getStatus() == status)
                    return s;
            return null;
        }
        public static SuggestionStatus getSuggestionStatus(String status){
            try{
                byte b = Byte.parseByte(status);
                return getSuggestionStatus(b);
            }catch (NumberFormatException ex){
                try{
                    return SuggestionStatus.valueOf(status.toUpperCase());
                }catch (IllegalArgumentException ex2){
                    for(SuggestionStatus s : SuggestionStatus.values())
                        if(s.getName().equalsIgnoreCase(status))
                            return s;
                }
            }
            return null;
        }
    }

    public static void initialize(){
        Connection con = null;
        Statement stmt = null;
        try {
            con = getConnection();
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
                    "icon_url TINYTEXT," +
                    "created_at TIMESTAMP," +
                    "joined_at TIMESTAMP," +
                    "owner_id BIGINT," +
                    "bot_prefix TINYTEXT," +
                    "language TINYTEXT," +
                    "join_role BIGINT," +
                    "join_message TEXT," +
                    "leave_message TEXT," +
                    "ban_message TEXT," +
                    "unknown_command_message TEXT," +
                    "public_channel_id BIGINT," +
                    "suggestion_channel_id BIGINT," +
                    "delete_invites BOOLEAN," +
                    "invite_warning TEXT," +
                    "sent_message_count INT," +
                    "sent_command_count INT," +
                    "sent_public_message_count INT," +
                    "sent_unknown_command_count INT," +
                    "sent_custom_command_count INT," +
                    "token TINYTEXT," +
                    "readonly_token TINYTEXT" +
                    ")";
            String createCustomCommandsTable = "CREATE TABLE IF NOT EXISTS " + Table.CUSTOM_COMMANDS.getName() + " (" +
                    "guild_id BIGINT," +
                    "command TINYTEXT," +
                    "response TEXT" +
                    ")";
            String createPermissionsTable = "CREATE TABLE IF NOT EXISTS " + Table.PERMISSIONS.getName() + " (" +
                    "guild_id BIGINT," +
                    "holder_id BIGINT," +
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
                    "song TINYTEXT" +
                    ")";
            String createDvcTable = "CREATE TABLE IF NOT EXISTS " + Table.DVCS.getName() + " (" +
                    "guild_id BIGINT," +
                    "name TINYTEXT" +
                    ")";
            String createWhitelistedInvitesTable = "CREATE TABLE IF NOT EXISTS " + Table.WHITELISTED_INVITES.getName() + " (" +
                    "guild_id BIGINT," +
                    "code TINYTEXT" +
                    ")";
            String createBlockedChannelsTable = "CREATE TABLE IF NOT EXISTS " + Table.BLOCKED_CHANNELS.getName() + " (" +
                    "guild_id BIGINT," +
                    "channel_id BIGINT" +
                    ")";
            String createScriptsTable = "CREATE TABLE IF NOT EXISTS " + Table.SCRIPTS.getName() + " (" +
                    "guild_id BIGINT," +
                    "type TINYTEXT," +
                    "name TINYTEXT," +
                    "content MEDIUMTEXT" +
                    ")";
            String createStatsTable = "CREATE TABLE IF NOT EXISTS " + Table.BOT_STATS.getName() + " (" +
                    "sent_message_count INT," +
                    "public_message_count INT," +
                    "received_message_count INT," +
                    "received_command_count INT," +
                    "received_custom_command_count INT," +
                    "received_unknown_command_count INT," +
                    "guild_count INT," +
                    "user_count INT" +
                    ")";
            String createBotSuggestionsTable = "CREATE TABLE IF NOT EXISTS " + Table.BOT_SUGGESTIONS.getName() + " (" +
                    "id INT NOT NULL PRIMARY KEY AUTO_INCREMENT," +
                    "title TINYTEXT," +
                    "content TEXT," +
                    "created_at TIMESTAMP," +
                    "user_id BIGINT," +
                    "status TINYINT," +
                    "detailed_status TEXT," +
                    "last_update TIMESTAMP" +
                    ")";
            String createBotSuggestionNotificationsTable = "CREATE TABLE IF NOT EXISTS " + Table.BOT_SUGGESTION_NOTIFICATIONS.getName() + " (" +
                    "suggestion_id INT," +
                    "user_id BIGINT" +
                    ")";
            String createSuggestionsTable = "CREATE TABLE IF NOT EXISTS " + Table.SUGGESTIONS.getName() + " (" +
                    "guild_id BIGINT," +
                    "id INT," +
                    "title TINYTEXT," +
                    "content TEXT," +
                    "created_at TIMESTAMP," +
                    "user_id BIGINT," +
                    "status TINYINT," +
                    "detailed_status TEXT," +
                    "last_update TIMESTAMP," +
                    "type INT" +
                    ")";
            String createSuggestionNotificationTable = "CREATE TABLE IF NOT EXISTS " + Table.SUGGESTION_NOTIFICATIONS.getName() + " (" +
                    "guild_id BIGINT," +
                    "suggestion_id INT," +
                    "user_id BIGINT" +
                    ")";
            stmt = con.createStatement();
            stmt.execute(createGuildsTable);
            stmt.execute(createMembersTable);
            stmt.execute(createUsersTable);
            stmt.execute(createDvcTable);
            stmt.execute(createPermissionsTable);
            stmt.execute(createWhitelistedInvitesTable);
            stmt.execute(createScriptsTable);
            stmt.execute(createCustomCommandsTable);
            stmt.execute(createBlockedChannelsTable);
            stmt.execute(createBotSuggestionsTable);
            stmt.execute(createBotSuggestionNotificationsTable);
            stmt.execute(createSuggestionsTable);
            stmt.execute(createSuggestionNotificationTable);

            String createServerBackupTable = "CREATE TABLE IF NOT EXISTS " + Table.BACKUP_GENERAL.getName() + " (" +
                    "backup_id TINYTEXT," +
                    "guild_id BIGINT," +
                    "name TINYTEXT," +
                    "system_channel BIGINT," +
                    "afk_channel BIGINT," +
                    "afk_timeout SMALLINT," +
                    "default_message_notifications TINYINT," +
                    "verification_level TINYINT," +
                    "explicit_content_filter TINYINT," +
                    "backup_time TIMESTAMP," +
                    "automated BOOLEAN" +
                    ")";
            String createRolesBackupTable = "CREATE TABLE IF NOT EXISTS " + Table.BACKUP_ROLES.getName() + " (" +
                    "backup_id TINYTEXT," +
                    "guild_id BIGINT," +
                    "role_id BIGINT," +
                    "name TINYTEXT," +
                    "permissions BIGINT," +
                    "position SMALLINT," +
                    "color INT," +
                    "mentionable BOOLEAN," +
                    "hoist BOOLEAN" +
                    ")";
            String createChannelsBackupTable = "CREATE TABLE IF NOT EXISTS " + Table.BACKUP_CHANNELS.getName() + " (" +
                    "backup_id TINYTEXT," +
                    "guild_id BIGINT," +
                    "channel_id BIGINT," +
                    "name TINYTEXT," +
                    "parent_channel BIGINT," +
                    "position SMALLINT," +
                    "slowmode SMALLINT," +
                    "type TINYINT," +
                    "bitrate INT," +
                    "user_limit TINYINT," +
                    "topic TEXT," +
                    "nsfw BOOLEAN" +
                    ")";
            String createChannelPermissionsBackupTable = "CREATE TABLE IF NOT EXISTS " + Table.BACKUP_CHANNEL_PERMISSION_OVERWRITES.getName() + " (" +
                    "backup_id TINYTEXT," +
                    "guild_id BIGINT," +
                    "channel_id BIGINT," +
                    "holder_id BIGINT," +
                    "is_role BOOLEAN," +
                    "allow BIGINT," +
                    "deny BIGINT" +
                    ")";
            String createUsersBackupTable = "CREATE TABLE IF NOT EXISTS " + Table.BACKUP_USERS.getName() + " (" +
                    "backup_id TINYTEXT," +
                    "guild_id BIGINT," +
                    "user_id BIGINT," +
                    "nickname TINYTEXT" +
                    ")";
            String createUserRolesBackupTable = "CREATE TABLE IF NOT EXISTS " + Table.BACKUP_USER_ROLES.getName() + " (" +
                    "backup_id TINYTEXT," +
                    "guild_id BIGINT," +
                    "user_id BIGINT," +
                    "role_id BIGINT" +
                    ")";
            String createBansBackupTable = "CREATE TABLE IF NOT EXISTS " + Table.BACKUP_BANS.getName() + " (" +
                    "backup_id TINYTEXT," +
                    "guild_id BIGINT," +
                    "user_id BIGINT," +
                    "reason TEXT" +
                    ")";
            stmt.execute(createServerBackupTable);
            stmt.execute(createRolesBackupTable);
            stmt.execute(createChannelsBackupTable);
            stmt.execute(createChannelPermissionsBackupTable);
            stmt.execute(createUsersBackupTable);
            stmt.execute(createUserRolesBackupTable);
            stmt.execute(createBansBackupTable);

            stmt.close();
        }catch(SQLException ex){
            ex.printStackTrace();
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    initialize();
                }
            }, 50);
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
    }

    public static boolean guildIsRegistered(Long gId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean registered = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT guild_id FROM " + Table.GUILDS.getName() + " WHERE guild_id=? LIMIT 1");
            stmt.setLong(1, gId);
            rs = stmt.executeQuery();
            registered = rs.next();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return registered;
    }

    private static String generateToken(){
        String available = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-.,_:;!\"$%&/()=?+~#<>|{}[]";
        StringBuilder token = new StringBuilder("");
        Random rn = new Random();
        int length = rn.nextInt(10) + 50;
        while(length-- > 0){
            token.append(available.charAt(rn.nextInt(available.length())));
        }
        if(isTokenRegistered(token.toString()))
            return generateToken();
        else
            return token.toString();
    }
    private static boolean isTokenRegistered(String token){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean registered = true;
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT guild_id FROM " + Table.GUILDS.getName() + " WHERE token=? OR readonly_token=? LIMIT 1");
            stmt.setString(1, token);
            stmt.setString(2, token);
            rs = stmt.executeQuery();
            registered = rs.next();
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return registered;
    }
    public static String renewToken(Long gId){
        String token = generateToken();
        if(setGuild(gId, "token", token, JDBCType.VARCHAR))
            return token;
        else
            return null;
    }
    public static String renewReadonlyToken(Long gId){
        String token = generateToken();
        if(setGuild(gId, "readonly_token", token, JDBCType.VARCHAR))
            return token;
        else
            return null;
    }

    public static void initializeGuild(Guild g){
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            stmt = con.prepareStatement("INSERT INTO " + Table.GUILDS.getName() + " (" +
                    "guild_id," +                       // 1
                    "name," +                           // 2
                    "created_at," +                     // 3
                    "joined_at," +                      // 4
                    "owner_id," +                       // 5
                    "bot_prefix," +                     // 6
                    "language," +                       // 7
                    "public_channel_id," +              // 8
                    "delete_invites," +                 // 9
                    "invite_warning," +                 // 10
                    "sent_message_count," +             // 11
                    "sent_command_count," +             // 12
                    "sent_public_message_count," +      // 13
                    "sent_unknown_command_count," +     // 14
                    "sent_custom_command_count," +      // 15
                    "token," +                          // 16
                    "icon_url," +                       // 17
                    "join_role," +                      // 18
                    "join_message," +                   // 19
                    "leave_message," +                  // 20
                    "ban_message," +                    // 21
                    "unknown_command_message," +        // 22
                    "suggestion_channel_id," +          // 23
                    "readonly_token" +                  // 24
                    ")" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setLong(1, g.getId().asLong());
            stmt.setString(2, g.getName());
            stmt.setTimestamp(3, Timestamp.from(BotUtils.getSnowflakeCreationDate(g.getId().asLong())));
            stmt.setTimestamp(4, Timestamp.from(g.getJoinTime().orElse(Instant.now())));
            stmt.setLong(5, g.getOwnerId().asLong());
            stmt.setString(6, "=");
            stmt.setString(7, "en");
            stmt.setLong(8, 0L);
            stmt.setBoolean(9, false);
            stmt.setString(10, "");
            stmt.setInt(11, 0);
            stmt.setInt(12, 0);
            stmt.setInt(13, 0);
            stmt.setInt(14, 0);
            stmt.setInt(15, 0);
            stmt.setString(16, generateToken());
            stmt.setString(17, g.getIconUrl(Image.Format.PNG).orElse(""));
            stmt.setLong(18, 0);
            stmt.setString(19, "");
            stmt.setString(20, "");
            stmt.setString(21, "");
            stmt.setString(22, "");
            stmt.setLong(23, 0L);
            stmt.setString(24, generateToken());
            stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
    }

    public static void updateGuild(Guild g){
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            stmt = con.prepareStatement("UPDATE " + Table.GUILDS.getName() + " SET name=?,icon_url=?,owner_id=? WHERE guild_id=? LIMIT 1");
            stmt.setString(1, g.getName());
            stmt.setString(2, g.getIconUrl(Image.Format.PNG).orElse(""));
            stmt.setLong(3, g.getOwnerId().asLong());
            stmt.setLong(4, g.getId().asLong());
            stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
    }

    public static SQLGuild getGuild(Long gId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        SQLGuild g = new SQLGuild();
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.GUILDS.getName() + " WHERE guild_id=? LIMIT 1");
            stmt.setLong(1, gId);
            rs = stmt.executeQuery();
            rs.next();
            g = new SQLGuild(rs.getLong("guild_id"),
                    rs.getString("name"),
                    rs.getString("icon_url"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("joined_at").toInstant(),
                    rs.getLong("owner_id"),
                    rs.getString("bot_prefix"),
                    rs.getString("language"),
                    rs.getLong("join_role"),
                    rs.getString("join_message"),
                    rs.getString("leave_message"),
                    rs.getString("ban_message"),
                    rs.getString("unknown_command_message"),
                    rs.getLong("public_channel_id"),
                    rs.getBoolean("delete_invites"),
                    rs.getString("invite_warning"),
                    rs.getInt("sent_message_count"),
                    rs.getInt("sent_command_count"),
                    rs.getInt("sent_public_message_count"),
                    rs.getInt("sent_unknown_command_count"),
                    rs.getInt("sent_custom_command_count"),
                    rs.getString("token"),
                    rs.getLong("suggestion_channel_id"),
                    rs.getString("readonly_token")
            );
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return g;
    }

    public static SQLGuild getGuild(String token, boolean allowReadOnly){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        SQLGuild g = new SQLGuild();
        try {
            con = getConnection();
            if(allowReadOnly){
                stmt = con.prepareStatement("SELECT * FROM " + Table.GUILDS.getName() + " WHERE token=? OR readonly_token=? LIMIT 1");
                stmt.setString(2, token);
            }else{
                stmt = con.prepareStatement("SELECT * FROM " + Table.GUILDS.getName() + " WHERE token=? LIMIT 1");
            }
            stmt.setString(1, token);
            rs = stmt.executeQuery();
            if(rs.next())
                g = new SQLGuild(rs.getLong("guild_id"),
                        rs.getString("name"),
                        rs.getString("icon_url"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("joined_at").toInstant(),
                        rs.getLong("owner_id"),
                        rs.getString("bot_prefix"),
                        rs.getString("language"),
                        rs.getLong("join_role"),
                        rs.getString("join_message"),
                        rs.getString("leave_message"),
                        rs.getString("ban_message"),
                        rs.getString("unknown_command_message"),
                        rs.getLong("public_channel_id"),
                        rs.getBoolean("delete_invites"),
                        rs.getString("invite_warning"),
                        rs.getInt("sent_message_count"),
                        rs.getInt("sent_command_count"),
                        rs.getInt("sent_public_message_count"),
                        rs.getInt("sent_unknown_command_count"),
                        rs.getInt("sent_custom_command_count"),
                        rs.getString("token"),
                        rs.getLong("suggestion_channel_id"),
                        rs.getString("readonly_token")
                );
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return g;
    }

    public static boolean setGuild(Long gId, String key, Object value, JDBCType type){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("UPDATE " + Table.GUILDS.getName() + " SET " + key + "=? WHERE guild_id=? LIMIT 1");
            stmt.setObject(1, value, type);
            stmt.setLong(2, gId);
            stmt.executeUpdate();
            success = true;
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }

    public static SQLUser getUser(Long uId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        SQLUser u = null;
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.USERS.getName() + " WHERE user_id=? LIMIT 1");
            stmt.setLong(1, uId);
            rs = stmt.executeQuery();
            rs.next();
            u = new SQLUser(rs.getLong("user_id"),
                    rs.getString("username"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getInt("sent_message_count"),
                    rs.getInt("sent_command_count"),
                    rs.getInt("sent_public_message_count"),
                    rs.getInt("sent_unknown_command_count"),
                    rs.getInt("sent_custom_command_count"),
                    rs.getString("bot_ban_reason"),
                    rs.getString("public_chat_ban_reason"));
        } catch(SQLException ex) {
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return u;
    }

    public static boolean setUser(Long uId, String key, Object value, JDBCType type){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("UPDATE " + Table.USERS.getName() + " SET " + key + "=? WHERE user_id=? LIMIT 1");
            stmt.setObject(1, value, type);
            stmt.setLong(2, uId);
            stmt.executeUpdate();
            success = true;
        } catch(SQLException ex) {
            ex.printStackTrace();
        } finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }

    public static SQLMember getMember(Long gId, Long uId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        SQLMember m = new SQLMember();
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.MEMBERS.getName() + " WHERE guild_id=? AND user_id=? LIMIT 1");
            stmt.setLong(1, gId);
            stmt.setLong(2, uId);
            rs = stmt.executeQuery();
            if(rs.next())
                m = new SQLMember(rs.getLong("user_id"),
                        rs.getLong("guild_id"),
                        rs.getBoolean("is_nicked"),
                        rs.getString("guildname"),
                        rs.getTimestamp("joined_at").toInstant(),
                        rs.getInt("sent_message_count"),
                        rs.getInt("sent_command_count"),
                        rs.getInt("sent_public_message_count"),
                        rs.getInt("sent_unknown_command_count"),
                        rs.getInt("sent_custom_command_count"));
        } catch(SQLException ex) {
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return m;
    }

    public static boolean setMember(Long gId, Long uId, String key, Object value, JDBCType type){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("UPDATE " + Table.USERS.getName() + " SET " + key + "=? WHERE guild_id=? AND user_id=? LIMIT 1");
            stmt.setObject(1, value, type);
            stmt.setLong(2, gId);
            stmt.setLong(3, uId);
            stmt.executeUpdate();
            success = true;
        } catch(SQLException ex) {
            ex.printStackTrace();
        } finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }

    public static List<String> getDVCs(Long gId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<String> dvcs = new ArrayList<>();
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.DVCS.getName() + " WHERE guild_id=?");
            stmt.setLong(1, gId);
            rs = stmt.executeQuery();
            while(rs.next())
                dvcs.add(rs.getString("name"));
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return dvcs;
    }

    public static boolean isDVC(Long gId, String name){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean isDVC = false;
        try{
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.DVCS.getName() + " WHERE guild_id=? AND name=? LIMIT 1");
            stmt.setLong(1, gId);
            stmt.setString(2, name);
            rs = stmt.executeQuery();
            isDVC = rs.next();
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return isDVC;
    }
    public static boolean addDVC(Long gId, String name){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("INSERT INTO " + Table.DVCS.getName() + " (guild_id, name) VALUES (?, ?)");
            stmt.setLong(1, gId);
            stmt.setString(2, name);
            stmt.executeUpdate();
            success = true;
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }
    public static boolean removeDVC(Long gId, String name){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("DELETE FROM " + Table.DVCS.getName() + " WHERE guild_id=? AND name=? LIMIT 1");
            stmt.setLong(1, gId);
            stmt.setString(2, name);
            stmt.executeUpdate();
            success = true;
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }

    public static List<SQLPermissions> getPermissions(Long gId, String cmd){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<SQLPermissions> p = new ArrayList<>();
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.PERMISSIONS.getName() + " WHERE guild_id=? AND action=?");
            stmt.setLong(1, gId);
            stmt.setString(2, cmd);
            rs = stmt.executeQuery();
            while (rs.next()){
                p.add(new SQLPermissions(rs.getLong("guild_id"), rs.getLong("holder_id"), rs.getString("action"), rs.getInt("type")));
            }
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return p;
    }
    public static List<SQLPermissions> getPermissions(Long gId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<SQLPermissions> p = new ArrayList<>();
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.PERMISSIONS.getName() + " WHERE guild_id=?");
            stmt.setLong(1, gId);
            rs = stmt.executeQuery();
            while (rs.next())
                p.add(new SQLPermissions(rs.getLong("guild_id"), rs.getLong("holder_id"), rs.getString("action"), rs.getInt("type")));
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return p;
    }
    public static boolean setPermissions(Long gId, String cmd, int type, Long hId){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("INSERT INTO " + Table.PERMISSIONS.getName() + " (guild_id, action, holder_id, type) VALUES (?, ?, ?, ?)");
            stmt.setLong(1, gId);
            stmt.setString(2, cmd);
            stmt.setLong(3, hId);
            stmt.setInt(4, type);
            stmt.executeUpdate();
            removePermissions(gId, cmd, (type + 2) % 4, hId);
            success = true;
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }
    public static boolean removePermissions(Long gId, String cmd){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("");

            return true;
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }
    public static boolean removePermissions(Long gId, String cmd, Long hId){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("");

            return true;
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }
    public static boolean removePermissions(Long gId, String cmd, int type, Long hId){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("");

            return true;
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }

    public static List<String> getAllowedInvites(Long gId){
        Connection con = null;
        PreparedStatement stmt = null;
        List<String> allowed = new ArrayList<>();
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.WHITELISTED_INVITES.getName() + " WHERE guild_id=? LIMIT 1");
            stmt.setLong(1, gId);
            ResultSet rs = stmt.executeQuery();
            while(rs.next())
                allowed.add(rs.getString("code"));
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return allowed;
    }
    public static boolean allowInvite(Long gId, String invite){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("INSERT INTO " + Table.WHITELISTED_INVITES.getName() + " (guild_id, code) VALUES (?, ?)");
            stmt.setLong(1, gId);
            stmt.setString(2, invite);
            stmt.executeUpdate();
            success = true;
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }
    public static boolean disallowInvite(Long gId, String invite){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("DELETE FROM " + Table.WHITELISTED_INVITES.getName() + " WHERE guild_id=? AND code=? LIMIT 1");
            stmt.setLong(1, gId);
            stmt.setString(2, invite);
            stmt.executeUpdate();
            success = true;
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }
    public static boolean isInviteAllowed(Long gId, String invite){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean allowed = true;
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.WHITELISTED_INVITES.getName() + " WHERE guild_id=? AND code=? LIMIT 1");
            stmt.setLong(1, gId);
            stmt.setString(2, invite);
            rs = stmt.executeQuery();
            allowed = rs.next();
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return allowed;
    }

    public static boolean addScript(Long gId, ScriptExecutor.ScriptEvent event, String name, String content){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("INSERT INTO " + Table.SCRIPTS.getName() + " (guild_id, type, name, content) VALUES (?, ?, ?, ?)");
            stmt.setLong(1, gId);
            stmt.setString(2, event.getEventName());
            stmt.setString(3, name);
            stmt.setString(4, content);
            stmt.executeUpdate();
            success = true;
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }
    public static List<String> getScripts(Long gId, ScriptExecutor.ScriptEvent event){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<String> scripts = new ArrayList<>();
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.SCRIPTS.getName() + " WHERE guild_id=? AND type=?");
            stmt.setLong(1, gId);
            stmt.setString(2, event.getEventName());
            rs = stmt.executeQuery();
            while(rs.next())
                scripts.add(rs.getString("content"));
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return scripts;
    }

    public static List<Long> getBlockedChannels(Long gId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Long> channels = new ArrayList<>();
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.BLOCKED_CHANNELS.getName() + " WHERE guild_id=?");
            stmt.setLong(1, gId);
            rs = stmt.executeQuery();
            while (rs.next())
                channels.add(rs.getLong("channel_id"));
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return channels;
    }
    public static boolean addBlockedChannel(Long gId, Long cId){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try{
            con = getConnection();
            stmt = con.prepareStatement("INSERT INTO " + Table.BLOCKED_CHANNELS.getName() + " (guild_id, channel_id) VALUES (?, ?)");
            stmt.setLong(1, gId);
            stmt.setLong(2, cId);
            stmt.executeUpdate();
            success = true;
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }
    public static boolean removeBlockedChannel(Long gId, Long cId){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("DELETE FROM " + Table.BLOCKED_CHANNELS.getName() + " WHERE guild_id=? AND channel_id=? LIMIT 1");
            stmt.setLong(1, gId);
            stmt.setLong(2, cId);
            stmt.executeUpdate();
            success = true;
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }

    public static Map<String, String> getCustomCommands(Long gId){
        if(gId == 0) return new HashMap<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Map<String, String> commands = new HashMap<>();
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.CUSTOM_COMMANDS.getName() + " WHERE guild_id=?");
            stmt.setLong(1, gId);
            rs = stmt.executeQuery();
            while (rs.next())
                commands.put(rs.getString("command"), rs.getString("response"));
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return commands;
    }
    public static boolean addCustomCommand(Long gId, String cmd, String response){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("INSERT INTO " + Table.CUSTOM_COMMANDS.getName() + " (guild_id, command, response) VALUES (?, ?, ?)");
            stmt.setLong(1, gId);
            stmt.setString(2, cmd);
            stmt.setString(3, response);
            stmt.executeUpdate();
            success = true;
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }
    public static boolean removeCustomCommand(Long gId, String cmd){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("DELETE FROM " + Table.CUSTOM_COMMANDS.getName() + " WHERE guild_id=? AND command=? LIMIT 1");
            stmt.setLong(1, gId);
            stmt.setString(2, cmd);
            stmt.executeUpdate();
            success = true;
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }

    public static int addBotSuggestion(Long uId, String title, String content, Instant createdAt){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int suggestionId = -1;
        try {
            con = getConnection();
            stmt = con.prepareStatement("INSERT INTO " + Table.BOT_SUGGESTIONS.getName() + " (id, user_id, title, content, created_at, status, detailed_status, last_update) VALUES (NULL, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            stmt.setLong(1, uId);
            stmt.setString(2, title);
            stmt.setString(3, content);
            stmt.setTimestamp(4, Timestamp.from(createdAt));
            stmt.setByte(5, (byte)0);
            stmt.setString(6, "Opened");
            stmt.setTimestamp(7, Timestamp.from(createdAt));
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if(rs.next())
                suggestionId = rs.getInt(1);
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(con);
        }
        return suggestionId;
    }
    public static SQLBotSuggestion getBotSuggestion(int sId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        SQLBotSuggestion suggestion = null;
        try{
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.BOT_SUGGESTIONS.getName() + " WHERE id=? LIMIT 1");
            stmt.setInt(1, sId);
            rs = stmt.executeQuery();
            if(rs.next())
                suggestion = new SQLBotSuggestion(
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getByte("status"),
                        rs.getString("detailed_status"),
                        rs.getInt("id"),
                        rs.getLong("user_id"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("last_update").toInstant()
                );
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return suggestion;
    }
    public static List<SQLBotSuggestion> getBotSuggestions(long pageNumber, long itemsPerPage){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<SQLBotSuggestion> suggestions = new ArrayList<>();
        try{
            con = getConnection();
            long startId = (pageNumber-1)*itemsPerPage+1;
            stmt = con.prepareStatement(
                    "SELECT * FROM " + Table.BOT_SUGGESTIONS.getName() + " WHERE status<>? AND id>=" +
                        "(SELECT id FROM" +
                            "(SELECT id FROM " + Table.BOT_SUGGESTIONS.getName() + " WHERE status<>? ORDER BY id ASC LIMIT ?)" +
                        "AS mostInner ORDER BY id DESC LIMIT 1)" +
                    "ORDER BY id ASC LIMIT ?"
            );
            stmt.setByte(1, SuggestionStatus.DELETED.getStatus());
            stmt.setByte(2,SuggestionStatus.DELETED.getStatus());
            stmt.setLong(3, startId);
            stmt.setLong(4, itemsPerPage);
            rs = stmt.executeQuery();
            while(rs.next())
                suggestions.add(new SQLBotSuggestion(
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getByte("status"),
                        rs.getString("detailed_status"),
                        rs.getInt("id"),
                        rs.getLong("user_id"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("last_update").toInstant()
                ));
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return suggestions;
    }
    public static long getBotSuggestionPageCount(long itemsPerPage){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        long count = -1;
        try{
            con = getConnection();
            stmt = con.prepareStatement("SELECT COUNT(*) AS count FROM " + Table.BOT_SUGGESTIONS.getName() + " WHERE status<>?");
            stmt.setByte(1, SuggestionStatus.DELETED.getStatus());
            rs = stmt.executeQuery();
            if(rs.next())
                count = rs.getLong("count");
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return Math.round(Math.ceil((double)count / (double)itemsPerPage));
    }
    public static boolean setBotSuggestion(int sId, String key, Object value, JDBCType type){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("UPDATE " + Table.BOT_SUGGESTIONS.getName() + " SET " + key + "=? WHERE id=? LIMIT 1");
            stmt.setObject(1, value, type);
            stmt.setInt(2, sId);
            stmt.executeUpdate();
            success = true;
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }
    public static boolean setBotSuggestionStatus(int sId, byte status, String detailedStatus, Instant editedTimestamp){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("UPDATE " + Table.BOT_SUGGESTIONS.getName() + " SET status=?, detailed_status=?, last_update=? WHERE id=? LIMIT 1");
            stmt.setByte(1, status);
            stmt.setString(2, detailedStatus);
            stmt.setTimestamp(3, Timestamp.from(editedTimestamp));
            stmt.setInt(4, sId);
            stmt.executeUpdate();
            success = true;
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }

    public static List<Long> getBotSuggestionNotifications(int sId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Long> uIds = new ArrayList<>();
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.BOT_SUGGESTION_NOTIFICATIONS.getName() + " WHERE suggestion_id=?");
            stmt.setInt(1, sId);
            rs = stmt.executeQuery();
            while(rs.next())
                uIds.add(rs.getLong("user_id"));
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return uIds;
    }
    public static boolean setBotSuggestionNotification(Long uId, int sId, boolean notify){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try{
            con = getConnection();
            if(notify)
                stmt = con.prepareStatement("INSERT INTO " + Table.BOT_SUGGESTION_NOTIFICATIONS.getName() + " (user_id, suggestion_id) VALUES (?, ?)");
            else
                stmt = con.prepareStatement("DELETE FROM " + Table.BOT_SUGGESTION_NOTIFICATIONS.getName() + " WHERE user_id=? AND suggestion_id=? LIMIT 1");
            stmt.setLong(1, uId);
            stmt.setInt(2, sId);
            stmt.executeUpdate();
            success = true;
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }

    public static SQLFeedback addSuggestion(Long gId, Long uId, String title, String content, Instant createdAt, SQLFeedback.FeedbackType type){
        Connection con = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt2 = null;
        ResultSet rs = null;
        SQLFeedback suggestion = null;
        try {
            con = getConnection();
            stmt = con.prepareStatement("INSERT INTO " + Table.SUGGESTIONS.getName() + " (id, user_id, title, content, created_at, status, detailed_status, last_update, guild_id, type) VALUES ((SELECT count FROM (SELECT MAX(id) AS count FROM " + Table.SUGGESTIONS.getName() + " WHERE guild_id=?) AS cnt)+1, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setLong(1, gId);
            stmt.setLong(2, uId);
            stmt.setString(3, title);
            stmt.setString(4, content);
            stmt.setTimestamp(5, Timestamp.from(createdAt));
            stmt.setByte(6, (byte)0);
            stmt.setString(7, "Opened");
            stmt.setTimestamp(8, Timestamp.from(createdAt));
            stmt.setLong(9, gId);
            stmt.setInt(10, type.getValue());
            stmt.executeUpdate();
            stmt2 = con.prepareStatement("SELECT * FROM " + Table.SUGGESTIONS.getName() + " WHERE guild_id=? AND user_id=? AND created_at=? ORDER BY id DESC LIMIT 1");
            stmt2.setLong(1, gId);
            stmt2.setLong(2, uId);
            stmt2.setTimestamp(3, Timestamp.from(createdAt));
            rs = stmt2.executeQuery();
            if(rs.next())
                suggestion = new SQLFeedback(
                        rs.getLong("guild_id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getByte("status"),
                        rs.getString("detailed_status"),
                        rs.getInt("id"),
                        rs.getLong("user_id"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("last_update").toInstant(),
                        SQLFeedback.FeedbackType.getByValue(rs.getInt("type"))
                );
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(stmt2);
            DbUtils.closeQuietly(con);
        }
        return suggestion;
    }
    public static SQLFeedback getSuggestion(Long gId, int sId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        SQLFeedback suggestion = null;
        try{
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.SUGGESTIONS.getName() + " WHERE id=? AND guild_id=? LIMIT 1");
            stmt.setInt(1, sId);
            stmt.setLong(2, gId);
            rs = stmt.executeQuery();
            if(rs.next())
                suggestion = new SQLFeedback(
                        rs.getLong("guild_id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getByte("status"),
                        rs.getString("detailed_status"),
                        rs.getInt("id"),
                        rs.getLong("user_id"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("last_update").toInstant(),
                        SQLFeedback.FeedbackType.getByValue(rs.getInt("type"))
                );
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return suggestion;
    }
    public static List<SQLFeedback> getSuggestions(Long gId, long pageNumber, long itemsPerPage){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<SQLFeedback> suggestions = new ArrayList<>();
        try{
            con = getConnection();
            long startId = (pageNumber-1)*itemsPerPage+1;
            stmt = con.prepareStatement(
                    "SELECT * FROM " + Table.SUGGESTIONS.getName() + " WHERE guild_id=? AND status<>? AND id>=" +
                        "(SELECT id FROM" +
                            "(SELECT id FROM " + Table.SUGGESTIONS.getName() + " WHERE guild_id=? AND status<>? ORDER BY id ASC LIMIT ?)" +
                        "AS mostInner ORDER BY id DESC LIMIT 1)" +
                    "ORDER BY id ASC LIMIT ?"
            );
            stmt.setLong(1, gId);
            stmt.setByte(2, SuggestionStatus.DELETED.getStatus());
            stmt.setLong(3, gId);
            stmt.setByte(4, SuggestionStatus.DELETED.getStatus());
            stmt.setLong(5, startId);
            stmt.setLong(6, itemsPerPage);
            rs = stmt.executeQuery();
            while(rs.next())
                suggestions.add(new SQLFeedback(
                        rs.getLong("guild_id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getByte("status"),
                        rs.getString("detailed_status"),
                        rs.getInt("id"),
                        rs.getLong("user_id"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("last_update").toInstant(),
                        SQLFeedback.FeedbackType.getByValue(rs.getInt("type"))
                ));
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return suggestions;
    }
    public static List<SQLFeedback> getFeedback(Long gId, int items, int offset, boolean deleted){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<SQLFeedback> suggestions = new ArrayList<>();
        try{
            con = getConnection();
            if(!deleted){
                if(offset == 0){
                    stmt = con.prepareStatement("SELECT * FROM " + Table.SUGGESTIONS.getName() + " WHERE guild_id=? AND status<>? ORDER BY id ASC LIMIT ?");
                    stmt.setLong(1, gId);
                    stmt.setByte(2, SuggestionStatus.DELETED.getStatus());
                    stmt.setInt(3, items);
                }else{
                    stmt = con.prepareStatement(
                            "SELECT * FROM " + Table.SUGGESTIONS.getName() + " WHERE guild_id=? AND status<>? AND id>" +
                                "(SELECT id FROM" +
                                    "(SELECT id FROM " + Table.SUGGESTIONS.getName() + " WHERE guild_id=? AND status<>? ORDER BY id ASC LIMIT ?)" +
                                "AS mostInner ORDER BY id DESC LIMIT 1)" +
                            "ORDER BY id ASC LIMIT ?"
                    );
                    stmt.setLong(1, gId);
                    stmt.setByte(2, SuggestionStatus.DELETED.getStatus());
                    stmt.setLong(3, gId);
                    stmt.setByte(4, SuggestionStatus.DELETED.getStatus());
                    stmt.setInt(5, offset);
                    stmt.setInt(6, items);
                }
            }else{
                stmt = con.prepareStatement("SELECT * FROM " + Table.SUGGESTIONS.getName() + " WHERE guild_id=? AND id>? LIMIT ?");
                stmt.setLong(1, gId);
                stmt.setInt(2, offset);
                stmt.setInt(3, items);
            }
            rs = stmt.executeQuery();
            while(rs.next())
                suggestions.add(new SQLFeedback(
                        rs.getLong("guild_id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getByte("status"),
                        rs.getString("detailed_status"),
                        rs.getInt("id"),
                        rs.getLong("user_id"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("last_update").toInstant(),
                        SQLFeedback.FeedbackType.getByValue(rs.getInt("type"))
                ));
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return suggestions;
    }
    public static long getSuggestionPageCount(Long gId, long itemsPerPage){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        long count = -1;
        try{
            con = getConnection();
            stmt = con.prepareStatement("SELECT COUNT(*) AS cnt FROM " + Table.SUGGESTIONS.getName() + " WHERE status<>? AND guild_id=?");
            stmt.setByte(1, SuggestionStatus.DELETED.getStatus());
            stmt.setLong(2, gId);
            rs = stmt.executeQuery();
            if(rs.next())
                count = rs.getLong("cnt");
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return Math.round(Math.ceil((double)count / (double)itemsPerPage));
    }
    public static boolean setSuggestion(Long gId, int sId, String key, Object value, JDBCType type){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("UPDATE " + Table.SUGGESTIONS.getName() + " SET " + key + "=? WHERE id=? AND guild_id=? LIMIT 1");
            stmt.setObject(1, value, type);
            stmt.setInt(2, sId);
            stmt.setLong(3, gId);
            stmt.executeUpdate();
            success = true;
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }
    public static boolean setSuggestionStatus(Long gId, int sId, byte status, String detailedStatus, Instant editedTimestamp){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("UPDATE " + Table.SUGGESTIONS.getName() + " SET status=?, detailed_status=?, last_update=? WHERE id=? AND guild_id=? LIMIT 1");
            stmt.setByte(1, status);
            stmt.setString(2, detailedStatus);
            stmt.setTimestamp(3, Timestamp.from(editedTimestamp));
            stmt.setInt(4, sId);
            stmt.setLong(5, gId);
            stmt.executeUpdate();
            success = true;
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }

    public static List<Long> getSuggestionNotifications(Long gId, int sId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Long> uIds = new ArrayList<>();
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT user_id FROM " + Table.SUGGESTION_NOTIFICATIONS.getName() + " WHERE suggestion_id=? AND guild_id=?");
            stmt.setInt(1, sId);
            stmt.setLong(2, gId);
            rs = stmt.executeQuery();
            while(rs.next())
                uIds.add(rs.getLong("user_id"));
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return uIds;
    }
    public static boolean setSuggestionNotification(Long gId, Long uId, int sId, boolean notify){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try{
            con = getConnection();
            if(notify)
                stmt = con.prepareStatement("INSERT INTO " + Table.SUGGESTION_NOTIFICATIONS.getName() + " (user_id, suggestion_id, guild_id) VALUES (?, ?, ?)");
            else
                stmt = con.prepareStatement("DELETE FROM " + Table.SUGGESTION_NOTIFICATIONS.getName() + " WHERE user_id=? AND suggestion_id=? AND guild_id=? LIMIT 1");
            stmt.setLong(1, uId);
            stmt.setInt(2, sId);
            stmt.setLong(3, gId);
            stmt.executeUpdate();
            success = true;
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return success;
    }

    public static boolean guildBackupExists(Long gId, String bId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean exists = false;
        try{
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.BACKUP_GENERAL.getName() + " WHERE guild_id=? AND backup_id=? LIMIT 1");
            stmt.setLong(1, gId);
            stmt.setString(2, bId);
            rs = stmt.executeQuery();
            exists = rs.next();
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return exists;
    }

    public static int getGuildBackupCount(Long gId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int count = -1;
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT COUNT(*) AS cnt FROM " + Table.BACKUP_GENERAL.getName() + " WHERE guild_id=?");
            stmt.setLong(1, gId);
            rs = stmt.executeQuery();
            if(rs.next())
                count = rs.getInt("cnt");
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return count;
    }

    public static int getGuildBackupCount(Long gId, boolean automated){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int count = -1;
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT COUNT(*) AS cnt FROM " + Table.BACKUP_GENERAL.getName() + " WHERE guild_id=? AND automated=?");
            stmt.setLong(1, gId);
            stmt.setBoolean(2, automated);
            rs = stmt.executeQuery();
            if(rs.next())
                count = rs.getInt("cnt");
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return count;
    }

    public static boolean createGuildBackup(Guild g, String bId, boolean automated){
        Connection con = null;
        PreparedStatement stmt1 = null, stmt2 = null, stmt3 = null, stmt4 = null, stmt5 = null, stmt6 = null, stmt7 = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt1 = con.prepareStatement("INSERT INTO " + Table.BACKUP_GENERAL.getName() + " (" +
                    "backup_id," +
                    "guild_id," +
                    "name," +
                    "system_channel," +
                    "afk_channel," +
                    "afk_timeout," +
                    "default_message_notifications," +
                    "verification_level," +
                    "explicit_content_filter," +
                    "backup_time," +
                    "automated" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt1.setString(1, bId);
            stmt1.setLong(2, g.getId().asLong());
            stmt1.setString(3, g.getName());
            stmt1.setLong(4, g.getSystemChannelId().orElse(Snowflake.of(0L)).asLong());
            stmt1.setLong(5, g.getAfkChannelId().orElse(Snowflake.of(0L)).asLong());
            stmt1.setInt(6, g.getAfkTimeout());
            stmt1.setInt(7, g.getNotificationLevel().getValue());
            stmt1.setInt(8, g.getVerificationLevel().getValue());
            stmt1.setInt(9, g.getContentFilterLevel().getValue());
            stmt1.setTimestamp(10, Timestamp.from(Instant.now()));
            stmt1.setBoolean(11, automated);
            List<Object> vars = new ArrayList<>();
            StringBuilder prep2 = new StringBuilder("INSERT INTO " + Table.BACKUP_ROLES.getName() + " (backup_id, guild_id, role_id, name, permissions, position, color, mentionable, hoist) VALUES ");
            g.getRoles()
                    .doOnNext(r -> {
                        vars.add(bId);
                        vars.add(g.getId().asLong());
                        vars.add(r.getId().asLong());
                        vars.add(r.getName());
                        vars.add(r.getPermissions().getRawValue());
                        vars.add(r.getRawPosition());
                        vars.add(r.getColor().getRGB());
                        vars.add(r.isMentionable());
                        vars.add(r.isHoisted());
                        prep2.append("(?,?,?,?,?,?,?,?,?),");
                    })
                    .blockLast();
            prep2.deleteCharAt(prep2.length() - 1);
            stmt2 = con.prepareStatement(prep2.toString());
            for(int i = 0; i < vars.size(); i++)
                stmt2.setObject(i+1, vars.get(i));
            vars.clear();
            StringBuilder prep3 = new StringBuilder("INSERT INTO " + Table.BACKUP_CHANNELS.getName() + " (backup_id, guild_id, channel_id, name, parent_channel, position, slowmode, type, bitrate, user_limit, topic, nsfw) VALUES ");
            g.getChannels()
                    .doOnNext(c -> {
                        vars.add(bId);
                        vars.add(g.getId().asLong());
                        vars.add(c.getId().asLong());
                        vars.add(c.getName());
                        if(c instanceof GuildMessageChannel) vars.add(((GuildMessageChannel)c).getCategoryId().orElse(Snowflake.of(0L)).asLong()); else if(c instanceof VoiceChannel) vars.add(((VoiceChannel)c).getCategoryId().orElse(Snowflake.of(0L)).asLong()); else vars.add(0L);
                        vars.add(c.getRawPosition());
                        if(c instanceof TextChannel) vars.add(((TextChannel)c).getRateLimitPerUser()); else vars.add(0);
                        vars.add(c.getType().getValue());
                        if(c instanceof VoiceChannel) vars.add(((VoiceChannel)c).getBitrate()); else vars.add(0);
                        if(c instanceof VoiceChannel) vars.add(((VoiceChannel)c).getUserLimit()); else vars.add(0);
                        if(c instanceof GuildMessageChannel) vars.add(((GuildMessageChannel)c).getTopic().orElse("")); else vars.add("");
                        if(c instanceof GuildMessageChannel) vars.add(((GuildMessageChannel)c).isNsfw()); else vars.add(false);
                        prep3.append("(?,?,?,?,?,?,?,?,?,?,?,?),");
                    })
                    .blockLast();
            prep3.deleteCharAt(prep3.length() - 1);
            stmt3 = con.prepareStatement(prep3.toString());
            for(int i = 0; i < vars.size(); i++)
                stmt3.setObject(i+1, vars.get(i));
            vars.clear();
            AtomicBoolean backupPermissionOverwrites = new AtomicBoolean(false);
            StringBuilder prep4 = new StringBuilder("INSERT INTO " + Table.BACKUP_CHANNEL_PERMISSION_OVERWRITES.getName() + " (backup_id, guild_id, channel_id, holder_id, is_role, allow, deny) VALUES ");
            g.getChannels()
                    .doOnNext(c -> c.getPermissionOverwrites().forEach(p -> {
                        backupPermissionOverwrites.set(true);
                        vars.add(bId);
                        vars.add(g.getId().asLong());
                        vars.add(c.getId().asLong());
                        vars.add(p.getTargetId().asLong());
                        vars.add(p.getRoleId().isPresent());
                        vars.add(p.getAllowed().getRawValue());
                        vars.add(p.getDenied().getRawValue());
                        prep4.append("(?,?,?,?,?,?,?),");
                    }))
                    .blockLast();
            prep4.deleteCharAt(prep4.length() - 1);
            stmt4 = con.prepareStatement(prep4.toString());
            for(int i = 0; i < vars.size(); i++)
                stmt4.setObject(i+1, vars.get(i));
            vars.clear();
            AtomicBoolean backupUsers = new AtomicBoolean(false);
            StringBuilder prep5 = new StringBuilder("INSERT INTO " + Table.BACKUP_USERS.getName() + " (backup_id, guild_id, user_id, nickname) VALUES ");
            g.getMembers()
                    .filter(m -> m.getNickname().isPresent())
                    .doOnNext(m -> {
                        backupUsers.set(true);
                        vars.add(bId);
                        vars.add(g.getId().asLong());
                        vars.add(m.getId().asLong());
                        vars.add(m.getNickname().orElse(""));
                        prep5.append("(?,?,?,?),");
                    })
                    .blockLast();
            prep5.deleteCharAt(prep5.length() - 1);
            stmt5 = con.prepareStatement(prep5.toString());
            for(int i = 0; i < vars.size(); i++)
                stmt5.setObject(i+1, vars.get(i));
            vars.clear();
            AtomicBoolean backupUserRoles = new AtomicBoolean(false);
            StringBuilder prep6 = new StringBuilder("INSERT INTO " + Table.BACKUP_USER_ROLES.getName() + " (backup_id, guild_id, user_id, role_id) VALUES ");
            g.getMembers()
                    .doOnNext(m -> m.getRoles()
                            .doOnNext(r -> {
                                backupUserRoles.set(true);
                                vars.add(bId);
                                vars.add(g.getId().asLong());
                                vars.add(m.getId().asLong());
                                vars.add(r.getId().asLong());
                                prep6.append("(?,?,?,?),");
                            }).blockLast()
                    )
                    .blockLast();
            prep6.deleteCharAt(prep6.length() - 1);
            stmt6 = con.prepareStatement(prep6.toString());
            for(int i = 0; i < vars.size(); i++)
                stmt6.setObject(i+1, vars.get(i));
            vars.clear();
            AtomicBoolean backupBans = new AtomicBoolean(false);
            StringBuilder prep7 = new StringBuilder("INSERT INTO " + Table.BACKUP_BANS.getName() + " (backup_id, guild_id, user_id, reason) VALUES ");
            g.getBans()
                    .doOnNext(b -> {
                        backupBans.set(true);
                        vars.add(bId);
                        vars.add(g.getId().asLong());
                        vars.add(b.getUser().getId().asLong());
                        vars.add(b.getReason().orElse(""));
                        prep7.append("(?,?,?,?),");
                    })
                    .blockLast();
            prep7.deleteCharAt(prep7.length() - 1);
            stmt7 = con.prepareStatement(prep7.toString());
            for(int i = 0; i < vars.size(); i++)
                stmt7.setObject(i+1, vars.get(i));
            vars.clear();
            stmt1.executeUpdate();
            stmt2.executeUpdate();
            stmt3.executeUpdate();
            if(backupPermissionOverwrites.get()) stmt4.executeUpdate();
            if(backupUsers.get()) stmt5.executeUpdate();
            if(backupUserRoles.get()) stmt6.executeUpdate();
            if(backupBans.get()) stmt7.executeUpdate();
            success = true;
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt1);
            DbUtils.closeQuietly(stmt2);
            DbUtils.closeQuietly(stmt3);
            DbUtils.closeQuietly(stmt4);
            DbUtils.closeQuietly(stmt5);
            DbUtils.closeQuietly(stmt6);
            DbUtils.closeQuietly(stmt7);
            DbUtils.closeQuietly(con);
        }
        return success;
    }

    public static boolean restoreGuildBackup(Guild g, String bId){
        Connection con = null;
        PreparedStatement stmt1 = null, stmt2 = null, stmt3 = null, stmt4 = null, stmt5 = null, stmt6 = null, stmt7 = null;
        boolean success = false;
        try{
            con = getConnection();
            stmt1 = con.prepareStatement("SELECT * FROM " + Table.BACKUP_GENERAL.getName() + " WHERE guild_id=? AND backup_id=? LIMIT 1");
            stmt2 = con.prepareStatement("SELECT * FROM " + Table.BACKUP_ROLES.getName() + " WHERE guild_id=? AND backup_id=?");
            stmt3 = con.prepareStatement("SELECT * FROM " + Table.BACKUP_CHANNELS.getName() + " WHERE guild_id=? AND backup_id=?");
            stmt4 = con.prepareStatement("SELECT * FROM " + Table.BACKUP_CHANNEL_PERMISSION_OVERWRITES.getName() + " WHERE guild_id=? AND backup_id=?");
            stmt5 = con.prepareStatement("SELECT * FROM " + Table.BACKUP_USERS.getName() + " WHERE guild_id=? AND backup_id=?");
            stmt6 = con.prepareStatement("SELECT * FROM " + Table.BACKUP_USER_ROLES.getName() + " WHERE guild_id=? AND backup_id=?");
            stmt7 = con.prepareStatement("SELECT * FROM " + Table.BACKUP_BANS.getName() + " WHERE guild_id=? AND backup_id=?");
            stmt1.setLong(1, g.getId().asLong());
            stmt1.setString(2, bId);
            stmt2.setLong(1, g.getId().asLong());
            stmt2.setString(2, bId);
            stmt3.setLong(1, g.getId().asLong());
            stmt3.setString(2, bId);
            stmt4.setLong(1, g.getId().asLong());
            stmt4.setString(2, bId);
            stmt5.setLong(1, g.getId().asLong());
            stmt5.setString(2, bId);
            stmt6.setLong(1, g.getId().asLong());
            stmt6.setString(2, bId);
            stmt7.setLong(1, g.getId().asLong());
            stmt7.setString(2, bId);
            ResultSet rs1 = stmt1.executeQuery();
            ResultSet rs2 = stmt2.executeQuery();
            ResultSet rs3 = stmt3.executeQuery();
            ResultSet rs4 = stmt4.executeQuery();
            ResultSet rs5 = stmt5.executeQuery();
            ResultSet rs6 = stmt6.executeQuery();
            ResultSet rs7 = stmt7.executeQuery();

            if(!rs1.next()) return false;
            g.edit(ges -> {
                try{
                    ges.setReason("Restoring backup " + bId)
                            .setName(rs1.getString("name"))
                            .setVerificationLevel(Guild.VerificationLevel.of(rs1.getInt("verification_level")))
                            .setDefaultMessageNotificationsLevel(Guild.NotificationLevel.of(rs1.getInt("default_message_notifications")))
                            .setAfkTimeout(rs1.getInt("afk_timeout"))
                    ;
                }catch (SQLException ex){
                    ex.printStackTrace();
                }
            }).subscribe();

            Map<Long, String> userNicks = new HashMap<>();
            while(rs5.next())
                userNicks.put(rs5.getLong("user_id"), rs5.getString("nickname"));
            g.getMembers()
                    .flatMap(m -> m.getId().asLong() == g.getClient().getSelfId().get().asLong() ? g.changeSelfNickname(userNicks.get(m.getId().asLong())) : m.edit(gmes -> gmes.setNickname(userNicks.get(m.getId().asLong())))
                            .onErrorResume(err -> Mono.empty())
                    )
                    .subscribe();

            Map<Long, GuildChannel> channels = new HashMap<>();
            Map<Long, Role> roles = new HashMap<>();

            while(rs2.next()){
                Role role = g.getRoleById(Snowflake.of(rs2.getLong("role_id")))
                        .flatMap(r -> r.edit(res -> {
                            try{
                                res.setReason("Restoring backup " + bId)
                                        .setName(rs2.getString("name"))
                                        .setColor(new Color(rs2.getInt("color")))
                                        .setHoist(rs2.getBoolean("hoist"))
                                        .setMentionable(rs2.getBoolean("mentionable"))
                                        .setPermissions(PermissionSet.of(rs2.getLong("permissions")))
                                ;
                            }catch (SQLException ex){
                                ex.printStackTrace();
                            }
                        }))
                        .switchIfEmpty(g.createRole(rcs -> {
                            try{
                                rcs.setReason("Restoring backup " + bId)
                                        .setName(rs2.getString("name"))
                                        .setColor(new Color(rs2.getInt("color")))
                                        .setHoist(rs2.getBoolean("hoist"))
                                        .setMentionable(rs2.getBoolean("mentionable"))
                                        .setPermissions(PermissionSet.of(rs2.getLong("permissions")))
                                ;
                            }catch (SQLException ex){
                                ex.printStackTrace();
                            }
                        }))
                        .block();
                roles.put(rs2.getLong("role_id"), role);
            }
            g.getRoles()
                    .filter(r -> roles.values().stream().noneMatch(role -> role.getId().asLong() == r.getId().asLong()))
                    .flatMap(r -> r.delete("Restoring backup " + bId))
                    .subscribe();

            Map<Long, List<Snowflake>> userRoles = new HashMap<>();
            while(rs6.next())
                if(userRoles.containsKey(rs6.getLong("user_id")))
                    userRoles.get(rs6.getLong("user_id")).add(roles.get(rs6.getLong("role_id")).getId());
                else
                    userRoles.put(rs6.getLong("user_id"), new ArrayList<>(Arrays.asList(roles.get(rs6.getLong("role_id")).getId())));
            g.getMembers()
                    .flatMap(m -> m.edit(gmes -> gmes.setReason("Restoring backup " + bId).setRoles(Set.of(userRoles.getOrDefault(m.getId().asLong(), new ArrayList<>()).toArray(new Snowflake[]{})))))
                    .subscribe();

            Map<Long, String> bans = new HashMap<>();
            while(rs7.next())
                bans.put(rs7.getLong("user_id"), rs7.getString("reason"));
            bans.forEach((uId, reason) -> g.ban(Snowflake.of(uId), bqs -> bqs.setReason(reason)).subscribe());
            g.getBans()
                    .filter(b -> !bans.containsKey(b.getUser().getId().asLong()))
                    .flatMap(b -> g.unban(b.getUser().getId(), "Restoring backup " + bId))
                    .subscribe();
            success = true;
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt1);
            DbUtils.closeQuietly(stmt2);
            DbUtils.closeQuietly(stmt3);
            DbUtils.closeQuietly(stmt4);
            DbUtils.closeQuietly(stmt5);
            DbUtils.closeQuietly(stmt6);
            DbUtils.closeQuietly(stmt7);
            DbUtils.closeQuietly(con);
        }
        return success;
    }

}