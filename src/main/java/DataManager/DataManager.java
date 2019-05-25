package DataManager;

import Main.BotUtils;
import Main.Tokens;
import Scripts.ScriptExecutor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Image;
import org.apache.commons.dbutils.DbUtils;

import java.awt.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.List;

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
        SUGGESTION_NOTIFICATIONS("suggestionnotifications");

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
                    "token TINYTEXT" +
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
                    "last_update TIMESTAMP" +
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
            stmt.close();
        }catch(SQLException ex){
            ex.printStackTrace();
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    //initialize();
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
            stmt = con.prepareStatement("SELECT bot_prefix FROM " + Table.GUILDS.getName() + " WHERE guild_id=?");
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
        String available = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-.,_:;!\"§$%&/()=?+~#<>|{}[]";
        String token = "";
        Random rn = new Random();
        int length = rn.nextInt(10) + 50;
        while(length-- > 0){
            token += available.charAt(rn.nextInt(available.length()));
        }
        if(isTokenRegistered(token))
            return generateToken();
        else
            return token;
    }
    private static boolean isTokenRegistered(String token){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean registered = true;
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT guild_id FROM " + Table.GUILDS.getName() + " WHERE token=?");
            stmt.setString(1, token);
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
            return "";
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
                    "suggestion_channel_id" +           // 23
                    ")" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
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
            stmt = con.prepareStatement("UPDATE " + Table.GUILDS.getName() + " SET " +
                    "name=?," +
                    "icon_url=?," +
                    "owner_id=?" +
                    " WHERE guild_id=?");
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
            stmt = con.prepareStatement("SELECT * FROM " + Table.GUILDS.getName() + " WHERE guild_id=?");
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
                    rs.getLong("suggestion_channel_id")
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
            stmt = con.prepareStatement("UPDATE " + Table.GUILDS.getName() + " SET " + key + "=? WHERE guild_id=?");
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
            stmt = con.prepareStatement("SELECT * FROM " + Table.USERS.getName() + " WHERE user_id=?");
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
            stmt = con.prepareStatement("UPDATE " + Table.USERS.getName() + " SET " + key + "=? WHERE user_id=?");
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
        SQLMember m = null;
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.MEMBERS.getName() + " WHERE guild_id=? AND user_id=?");
            stmt.setLong(1, gId);
            stmt.setLong(2, uId);
            rs = stmt.executeQuery();
            rs.next();
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
            stmt = con.prepareStatement("UPDATE " + Table.USERS.getName() + " SET " + key + "=? WHERE guild_id=? AND user_id=?");
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
            stmt = con.prepareStatement("SELECT * FROM " + Table.DVCS.getName() + " WHERE guild_id=? AND name=?");
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
            stmt = con.prepareStatement("DELETE FROM " + Table.DVCS.getName() + " WHERE guild_id=? AND name=?");
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
            stmt = con.prepareStatement("SELECT * FROM " + Table.WHITELISTED_INVITES.getName() + " WHERE guild_id=?");
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
            stmt = con.prepareStatement("DELETE FROM " + Table.WHITELISTED_INVITES.getName() + " WHERE guild_id=? AND code=?");
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
            stmt = con.prepareStatement("SELECT * FROM " + Table.WHITELISTED_INVITES.getName() + " WHERE guild_id=? AND code=?");
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
            stmt = con.prepareStatement("DELETE FROM " + Table.BLOCKED_CHANNELS.getName() + " WHERE guild_id=? AND channel_id=?");
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
            stmt = con.prepareStatement("DELETE FROM " + Table.CUSTOM_COMMANDS.getName() + " WHERE guild_id=? AND command=?");
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
            stmt = con.prepareStatement("SELECT * FROM " + Table.BOT_SUGGESTIONS.getName() + " WHERE id=?");
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
            stmt = con.prepareStatement("UPDATE " + Table.BOT_SUGGESTIONS.getName() + " SET " + key + "=? WHERE id=?");
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
            stmt = con.prepareStatement("UPDATE " + Table.BOT_SUGGESTIONS.getName() + " SET status=?, detailed_status=?, last_update=? WHERE id=?");
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
                stmt = con.prepareStatement("DELETE FROM " + Table.BOT_SUGGESTION_NOTIFICATIONS.getName() + " WHERE user_id=? AND suggestion_id=?");
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

    public static int addSuggestion(Long gId, Long uId, String title, String content, Instant createdAt){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int suggestionId = -1;
        try {
            con = getConnection();
            stmt = con.prepareStatement("INSERT INTO " + Table.SUGGESTIONS.getName() + " (id, user_id, title, content, created_at, status, detailed_status, last_update, guild_id) VALUES ((SELECT count FROM (SELECT MAX(id) AS count FROM " + Table.SUGGESTIONS.getName() + " WHERE guild_id=?) AS cnt)+1, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            stmt.setLong(1, gId);
            stmt.setLong(2, uId);
            stmt.setString(3, title);
            stmt.setString(4, content);
            stmt.setTimestamp(5, Timestamp.from(createdAt));
            stmt.setByte(6, (byte)0);
            stmt.setString(7, "Opened");
            stmt.setTimestamp(8, Timestamp.from(createdAt));
            stmt.setLong(9, gId);
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
    public static SQLSuggestion getSuggestion(Long gId, int sId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        SQLSuggestion suggestion = null;
        try{
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.SUGGESTIONS.getName() + " WHERE id=? AND guild_id=?");
            stmt.setInt(1, sId);
            stmt.setLong(2, gId);
            rs = stmt.executeQuery();
            if(rs.next())
                suggestion = new SQLSuggestion(
                        rs.getLong("guild_id"),
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
    public static List<SQLSuggestion> getSuggestions(Long gId, long pageNumber, long itemsPerPage){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<SQLSuggestion> suggestions = new ArrayList<>();
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
            stmt.setByte(1, SuggestionStatus.DELETED.getStatus());
            stmt.setLong(2, gId);
            stmt.setByte(3, SuggestionStatus.DELETED.getStatus());
            stmt.setLong(4, gId);
            stmt.setLong(5, startId);
            stmt.setLong(6, itemsPerPage);
            rs = stmt.executeQuery();
            while(rs.next())
                suggestions.add(new SQLSuggestion(
                        rs.getLong("guild_id"),
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
    public static long getSuggestionPageCount(Long gId, long itemsPerPage){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        long count = -1;
        try{
            con = getConnection();
            stmt = con.prepareStatement("SELECT COUNT(*) AS count FROM " + Table.SUGGESTIONS.getName() + " WHERE status<>? AND guild_id=?");
            stmt.setByte(1, SuggestionStatus.DELETED.getStatus());
            stmt.setLong(2, gId);
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
    public static boolean setSuggestion(Long gId, int sId, String key, Object value, JDBCType type){
        Connection con = null;
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            con = getConnection();
            stmt = con.prepareStatement("UPDATE " + Table.SUGGESTIONS.getName() + " SET " + key + "=? WHERE id=? AND guild_id=?");
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
            stmt = con.prepareStatement("UPDATE " + Table.SUGGESTIONS.getName() + " SET status=?, detailed_status=?, last_update=? WHERE id=? AND guild_id=?");
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
            stmt = con.prepareStatement("SELECT * FROM " + Table.SUGGESTION_NOTIFICATIONS.getName() + " WHERE suggestion_id=? AND guild_id=?");
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
                stmt = con.prepareStatement("DELETE FROM " + Table.SUGGESTION_NOTIFICATIONS.getName() + " WHERE user_id=? AND suggestion_id=? AND guild_id=?");
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

}