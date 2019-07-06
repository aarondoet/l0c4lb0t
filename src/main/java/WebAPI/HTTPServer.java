package WebAPI;

import DataManager.*;
import Main.BotMain;
import Main.BotUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.util.Image;
import discord4j.core.object.util.Snowflake;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HTTPServer implements Runnable{

    static final int port = 1337;

    static final boolean verbose = false;

    private Socket connect;

    public HTTPServer(Socket c){
        connect = c;
    }



    public static void startServer(){
        Thread t = new Thread(()->{
            try{
                ServerSocket serverConnect = new ServerSocket(port);
                System.out.println("Server started");

                while(true){
                    HTTPServer myServer = new HTTPServer(serverConnect.accept());
                    if(verbose)
                        System.out.println("Connection opened");
                    Thread thread = new Thread(myServer);
                    thread.start();
                }
            }catch(IOException ex){
                System.out.println("Server connection error");
                ex.printStackTrace();
            }
        });
        t.start();
    }



    @Override
    public void run(){
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;
        try {
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            out = new PrintWriter(connect.getOutputStream());
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            String input = in.readLine();
            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase();
            fileRequested = parse.nextToken().toLowerCase();

            Map<String, List<String>> params = new HashMap<>();
            String[] urlParts = fileRequested.split("\\?");
            if(urlParts.length > 1){
                String query = urlParts[1];
                fileRequested = urlParts[0];
                for(String param : query.split("&")){
                    String[] pair = param.split("=");
                    String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                    String value = "";
                    if(pair.length > 1)
                        value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    List<String> values = params.get(key);
                    if(values == null){
                        values = new ArrayList<>();
                        params.put(key, values);
                    }
                    values.add(value);
                }
            }

            boolean unauthorized = false;

            String write = "{\"success\":false}";
            List<String> authTokens = params.get("auth");
            if(authTokens == null) unauthorized = true;
            else if(authTokens.size() != 1) unauthorized = true;
            else write = getReturnString(params.get("auth").get(0), fileRequested, method, params);

            if(write.length() == 0) unauthorized = true;
            //if(unauthorized) write = "auth=j%5Dnure)%5D6%3AU%2Czf9K5HVz%2CrNau§%2F(z3d%26-%3DuA9_%5DR>)-%2CfvU%26%7D%24%24_c";

            byte[] fileData = write.getBytes(StandardCharsets.UTF_8);
            int length = fileData.length;
            String contentType = "application/json";

            if(unauthorized) out.println("HTTP/1.1 401 Unauthorized");
            else out.println("HTTP/1.1 200 OK");
            out.println("Server: Java HTTP Server: 1.0");
            out.println("Date: " + new Date());
            out.println("Content-type: " + contentType);
            out.println("Content-length: " + length);
            out.println();
            out.flush();

            dataOut.write(fileData, 0, length);
            dataOut.flush();
        }catch(IOException ex){
            System.out.println("Server error");
            ex.printStackTrace();
        }finally{
            try{
                in.close();
                out.close();
                dataOut.close();
                connect.close();
            }catch(Exception ex){
                System.out.println("Error closing stream");
                ex.printStackTrace();
            }
            if(verbose)
                System.out.println("Connection closed\n");
        }
    }

    private String getReturnString(String guildAuth, String requested, String type, Map<String, List<String>> params){
        SQLGuild guild = DataManager.getGuild(guildAuth);
        if(guild.getGuildId() == 0L) return "";
        Guild g = BotMain.client.getGuildById(Snowflake.of(guild.getGuildId())).block();
        if(g == null) return "";

        ObjectNode node = new ObjectMapper().createObjectNode()
                .put("guild_id", g.getId().asLong());
        if(type.equals("GET")){
            if(requested.startsWith("/user/")){
                requested = requested.substring(6);
                Member m = BotUtils.getMemberFromArgument(g, requested).block();
                if(m == null) return new ObjectMapper().createObjectNode()
                        .put("success", false)
                        .toString();
                SQLMember sm = DataManager.getMember(m.getGuildId().asLong(), m.getId().asLong());

                node    .put("username", m.getUsername())
                        .put("tag", m.getDiscriminator())
                        .put("nickname", m.getNickname().orElse(null))
                        .put("is_bot", m.isBot())
                        .put("joined_at", m.getJoinTime().toString())
                        .put("premium_time", m.getPremiumTime().isPresent() ? m.getPremiumTime().toString() : null)
                        .put("avatar", m.getAvatarUrl())
                        .put("animated_avatar", m.hasAnimatedAvatar())
                ;
            }else if(requested.equals("/feedback")){
                /*
                    Parameters:
                    offset = how many items should get skipped, default 0
                    items = how many items should get returned, default 20, max 100
                    deleted = whether deleted items should be included, default false
                    id = the id of the element you want, can be used multiple times but not more than 50 times
                 */
                int items = 20;
                int offset = 0;
                boolean includeDeleted = false;
                List<String> itms = params.get("items");
                if(itms != null) if(itms.size() == 1) try{items = Integer.parseInt(itms.get(0));}catch(NumberFormatException ex){}
                items = BotUtils.clamp(items, 1, 100);
                List<String> offst = params.get("offset");
                if(offst != null) if(offst.size() == 1) try{offset = Integer.parseInt(offst.get(0));}catch(NumberFormatException ex){}
                if(offset < 0) offset = 0;
                List<String> inclDel = params.get("deleted");
                if(inclDel != null) if(inclDel.size() == 1) includeDeleted = Boolean.parseBoolean(inclDel.get(0));
                List<Integer> feedbackIds = new ArrayList<>();
                List<String> feedbckIds = params.get("id");
                if(feedbckIds != null) for(String s : feedbckIds) try{int id = Integer.parseInt(s); if(id > 0 && feedbackIds.size() < 100 && !feedbackIds.contains(id)) feedbackIds.add(id);}catch(NumberFormatException ex){}

                List<SQLFeedback> feedbacks = new ArrayList<>();
                if(feedbckIds != null && feedbckIds.size() > 0){
                    for(int id : feedbackIds){
                        SQLFeedback feedback = DataManager.getSuggestion(g.getId().asLong(), id);
                        if(feedback != null)
                            feedbacks.add(feedback);
                    }
                }else{
                    feedbacks = DataManager.getFeedback(g.getId().asLong(), items, offset, includeDeleted);
                }
                ArrayNode n = new ObjectMapper().createArrayNode();
                for(SQLFeedback feedback : feedbacks)
                    n.add(getFeedbackNode(feedback));
                return n.toString();
            }else if(requested.equals("/guild")){
                node.put("guild_id", g.getId().asLong())
                        .put("name", g.getName())
                        .put("verification_level", g.getVerificationLevel().getValue())
                        .put("content_filter_level", g.getContentFilterLevel().getValue())
                        .put("owner_id", g.getOwnerId().asLong())
                        .put("icon_url", g.getIconUrl(Image.Format.PNG).orElse(null))
                        .put("afk_channel_id", g.getAfkChannelId().isPresent() ? g.getAfkChannelId().get().asLong() : null)
                        .put("afk_timeout", g.getAfkTimeout())
                        .put("join_time", g.getJoinTime().isPresent() ? g.getJoinTime().get().toString() : null)
                        .put("banner_url", g.getBannerUrl(Image.Format.PNG).orElse(null))
                        .put("mfa_level", g.getMfaLevel().getValue())
                        .put("member_count", g.getMemberCount().isPresent() ? g.getMemberCount().getAsInt() : null)
                        .put("premium_subscriptions_count", g.getPremiumSubcriptionsCount().isPresent() ? g.getPremiumSubcriptionsCount().getAsInt() : null)
                        .put("premium_tier", g.getPremiumTier().getValue())
                        .put("region", g.getRegionId())

                        .put("bot_prefix", guild.getBotPrefix())
                        .put("delete_invites", guild.getDeleteInvites())
                        .put("invite_warning", guild.getInviteWarning())
                        .put("unknown_command_message", guild.getUnknownCommandMessage())
                        .put("language", guild.getLanguage())
                        .put("suggestion_channel_id", guild.getSuggestionChannelId())
                        .put("ban_message", guild.getBanMessage())
                        .put("join_message", guild.getJoinMessage())
                        .put("join_role", guild.getJoinRole())
                        .put("leave_message", guild.getLeaveMessage())
                        .put("sent_command_count", guild.getSentCommandCount())
                        .put("sent_custom_command_count", guild.getSentCustomCommandCount())
                        .put("sent_message_count", guild.getSentMessageCount())
                        .put("sent_public_message_count", guild.getSentPublicMessageCount())
                        .put("sent_unknown_command_count", guild.getSentUnknownCommandCount())
                ;
            }
        }

        return node.toString();
    }

    private ObjectNode getFeedbackNode(SQLFeedback feedback){
        if(feedback == null) return new ObjectMapper().createObjectNode();
        return new ObjectMapper().createObjectNode()
                .put("guild_id", feedback.getGuildId())
                .put("id", feedback.getId())
                .put("title", feedback.getTitle())
                .put("content", feedback.getContent())
                .put("created_at", feedback.getCreatedAt().toString())
                .put("creator_id", feedback.getCreatorId())
                .put("status", feedback.getStatus().getStatus())
                .put("detailed_status", feedback.getDetailedStatus().orElse(null))
                .put("last_update", feedback.getLastUpdate().toString())
                .put("type", feedback.getType().getValue())
                ;
    }

}
