package DataManager;

import Main.BotUtils;
import Main.Tokens;
import Scripts.ScriptExecutor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import org.apache.commons.dbutils.DbUtils;
import reactor.core.publisher.Mono;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class DataManager {

    private static final int port = 3306;
    private static final String url = "jdbc:mysql://localhost:" + port + "/l0c4lb0t?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";

    //private static Connection con = null;
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

    /*private static void ensureConnection(){
        boolean connect = false;
        if(con == null) connect = true;
        if(connect) {
            try {
                con = DriverManager.getConnection(url, Tokens.MYSQL_USERNAME, Tokens.MYSQL_PASSWORD);
            } catch(SQLException ex {}
        }
    }*/

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
        BOT_STATS("botstats");

        private final String name;
        Table(String name){this.name = name;}
        public String getName(){return this.name;}
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
                    "content MEDIUMBLOB" +
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
            stmt = con.prepareStatement("SELECT bot_prefix FROM " + Table.GUILDS.getName() + " WHERE guild_id=?");
            stmt.setLong(1, gId);
            rs = stmt.executeQuery();
            registered = rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
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

    public static Mono<Void> initializeGuild(Guild g){
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
                    "token" +                           // 16
                    ")" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
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
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(con);
        }
        return Mono.empty();
    }

    public static SQLGuild getGuild(Long gId){
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        SQLGuild g = null;
        try {
            con = getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + Table.GUILDS.getName() + " WHERE guild_id=?");
            stmt.setLong(1, gId);
            rs = stmt.executeQuery();
            rs.next();
            g = new SQLGuild(rs.getLong("guild_id"),
                    rs.getString("name"),
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
                    rs.getString("token"));
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
        } catch (SQLException e) {
            e.printStackTrace();
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
        }catch (SQLException e){
            e.printStackTrace();
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
        }catch (SQLException e){
            e.printStackTrace();
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
            Blob blob = con.createBlob();
            blob.setBytes(1, content.getBytes());
            stmt.setBlob(4, blob);
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
            while(rs.next()) {
                Blob blob = rs.getBlob("content");
                scripts.add(new String(blob.getBytes(1, (int)blob.length())));
            }
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

    public static Map<String, String> getCustomCommands(Long gId){
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

}