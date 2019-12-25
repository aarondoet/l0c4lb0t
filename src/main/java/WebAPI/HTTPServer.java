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
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerResponse;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class HTTPServer {

    static final int port = 1337;

    public static void startServer(){
        HttpServer.create().port(port)
                .route(routes ->
                    routes.get("/l0c4lb0t/user/{userId}", (request, response) -> {
                        ObjectNode node = new ObjectMapper().createObjectNode();
                        try{
                            String token = request.requestHeaders().get("Authorization", "");
                            SQLGuild guild = DataManager.getGuild(token, true);
                            if(guild.getGuildId() == 0L) return error(response, 401);
                            long uId = Long.parseLong(request.param("userId"));
                            SQLMember member = DataManager.getMember(guild.getGuildId(), uId);
                            return success(response)
                                    .sendByteArray(getGuild(guild.getGuildId()).flatMap(g -> getMember(g, uId).flatMap(m -> {
                                        node.put("status", 200)
                                            .set("result", new ObjectMapper().createObjectNode()
                                                    .put("username", m.getUsername())
                                                    .put("tag", m.getDiscriminator())
                                                    .put("nickname", m.getNickname().orElse(null))
                                                    .put("is_bot", m.isBot())
                                                    .put("joined_at", m.getJoinTime().toString())
                                                    .put("premium_time", m.getPremiumTime().isPresent() ? m.getPremiumTime().toString() : null)
                                                    .put("avatar", m.getAvatarUrl())
                                                    .put("animated_avatar", m.hasAnimatedAvatar())

                                                    .put("sent_message_count", member.getSentMessageCount())
                                                    .put("sent_command_count", member.getSentCommandCount())
                                                    .put("sent_unknown_command_count", member.getSentUnknownCommandCount())
                                                    .put("sent_custom_command_count", member.getSentCustomCommandCount())
                                                    .put("sent_public_message_count", member.getSentPublicMessageCount())
                                            );
                                        return Mono.just(node.toString().getBytes(StandardCharsets.UTF_8));
                                    }).onErrorReturn("{\"status\":404}".getBytes(StandardCharsets.UTF_8)).onErrorReturn("{\"status\":401}".getBytes(StandardCharsets.UTF_8))));
                        }catch(NumberFormatException ex){
                            return error(response, 400);
                        }
                    })
                    /**
                     *
                     */
                    .get("/l0c4lb0t/feedback", (request, response) -> {
                        Map<String, List<String>> params = getParams(request.uri());
                        ObjectNode node = new ObjectMapper().createObjectNode();
                        String token = request.requestHeaders().get("Authorization", "");
                        SQLGuild guild = DataManager.getGuild(token, true);
                        if(guild.getGuildId() == 0L) return error(response, 401);
                        return success(response).sendByteArray(getGuild(guild.getGuildId()).flatMap(g -> {
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
                            node.put("status", 200)
                                .set("result", n);
                            return Mono.just(node.toString().getBytes(StandardCharsets.UTF_8));
                        }).onErrorReturn("{\"status\":401}".getBytes(StandardCharsets.UTF_8)));
                    })
                    .get("/l0c4lb0t/guild", (request, response) -> {
                        ObjectNode node = new ObjectMapper().createObjectNode();
                        String token = request.requestHeaders().get("Authorization", "");
                        SQLGuild guild = DataManager.getGuild(token, true);
                        if(guild.getGuildId() == 0L) return error(response, 401);
                        return success(response).sendByteArray(getGuild(guild.getGuildId()).flatMap(g -> {
                            ArrayNode whitelistedInvites = new ObjectMapper().createArrayNode();
                            ArrayNode dvcs = new ObjectMapper().createArrayNode();
                            ArrayNode blockedChannels = new ObjectMapper().createArrayNode();
                            DataManager.getAllowedInvites(g.getId().asLong()).forEach(whitelistedInvites::add);
                            DataManager.getDVCs(g.getId().asLong()).forEach(dvcs::add);
                            DataManager.getBlockedChannels(g.getId().asLong()).forEach(blockedChannels::add);
                            ObjectNode result = new ObjectMapper().createObjectNode()
                                    .put("guild_id", g.getId().asLong())
                                    .put("name", g.getName())
                                    .put("verification_level", g.getVerificationLevel().getValue())
                                    .put("content_filter_level", g.getContentFilterLevel().getValue())
                                    .put("owner_id", g.getOwnerId().asLong())
                                    .put("icon_url", g.getIconUrl(Image.Format.PNG).orElse(null))
                                    .put("banner_url", g.getBannerUrl(Image.Format.PNG).orElse(null))
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
                                    .put("delete_invites", guild.isDeleteInvites())
                                    .put("invite_warning", guild.getInviteWarning())
                                    .put("unknown_command_message", guild.getUnknownCommandMessage())
                                    .put("language", guild.getLanguage())
                                    .put("suggestion_channel_id", guild.getSuggestionChannelId())
                                    .put("ban_message", guild.getBanMessage())
                                    .put("join_message", guild.getJoinMessage())
                                    .put("join_role", guild.getJoinRole())
                                    .put("leave_message", guild.getLeaveMessage())
                                    .put("sent_message_count", guild.getSentMessageCount())
                                    .put("received_message_count", guild.getReceivedMessageCount())
                                    .put("received_command_count", guild.getReceivedCommandCount())
                                    .put("received_custom_command_count", guild.getReceivedCustomCommandCount())
                                    //.put("received_public_message_count", guild.getReceivedPublicMessageCount())
                                    .put("received_unknown_command_count", guild.getReceivedUnknownCommandCount());
                            result.set("whitelisted_invites", whitelistedInvites);
                            result.set("dynamic_voice_channels", dvcs);
                            result.put("blocked_channels", blockedChannels);
                            node.put("status", 200)
                                .set("result", result);
                            return Mono.just(node.toString().getBytes(StandardCharsets.UTF_8));
                        }).onErrorReturn("{\"status\":401}".getBytes(StandardCharsets.UTF_8)));
                    })
                    .get("/l0c4lb0t/customcommands", (request, response) -> {
                        ObjectNode node = new ObjectMapper().createObjectNode();
                        String token = request.requestHeaders().get("Authorization", "");
                        SQLGuild guild = DataManager.getGuild(token, true);
                        if(guild.getGuildId() == 0L) return error(response, 401);
                        return success(response).sendByteArray(getGuild(guild.getGuildId()).flatMap(g -> {
                            ObjectNode n = new ObjectMapper().createObjectNode();
                            DataManager.getCustomCommands(g.getId().asLong()).forEach(n::put);
                            node.put("status", 200)
                                .set("result", n);
                            return Mono.just(node.toString().getBytes(StandardCharsets.UTF_8));
                        }).onErrorReturn("{\"status\":401}".getBytes(StandardCharsets.UTF_8)));
                    })


                    /*
                     * Header:
                     * Feedback-user: The user id ("unknown", "0" or id of an existing user)
                     * Feedback-title: The title (min length: 3)
                     * Feedback-content: The description (min length: 10)
                     * Feedback-type: The type (number or text, default: OTHER)
                     */
                    .post("/l0c4lb0t/feedback", (request, response) -> {
                        Map<String, List<String>> params = getParams(request.uri());
                        ObjectNode node = new ObjectMapper().createObjectNode();
                        String token = request.requestHeaders().get("Authorization", "");
                        SQLGuild guild = DataManager.getGuild(token, false);
                        if(guild.getGuildId() == 0L) return error(response, 401);
                        return success(response).sendByteArray(getGuild(guild.getGuildId()).flatMap(g -> {
                            AtomicLong uId = new AtomicLong(0L);
                            try{
                                String userId = request.requestHeaders().get("Feedback-user");
                                if(userId == null) return Mono.just("{\"status\":400}".getBytes(StandardCharsets.UTF_8));
                                if(!userId.equalsIgnoreCase("unknown")){
                                    uId.set(Long.parseLong(userId));
                                }
                            }catch(NumberFormatException ex){
                                return Mono.just("{\"status\":400}".getBytes(StandardCharsets.UTF_8));
                            }
                            String title = request.requestHeaders().get("Feedback-title", "");
                            String content = request.requestHeaders().get("Feedback-content", "");
                            if(title.length() < 3 || content.length() < 10)
                                return Mono.just("{\"status\":400}".getBytes(StandardCharsets.UTF_8));
                            Instant createdAt = Instant.now();
                            SQLFeedback.FeedbackType type = SQLFeedback.FeedbackType.getFeedbackType(request.requestHeaders().get("Feedback-type", "OTHER"), SQLFeedback.FeedbackType.OTHER);
                            if(uId.get() == 0L){
                                SQLFeedback feedback = DataManager.addSuggestion(g.getId().asLong(), uId.get(), title, content, createdAt, type);
                                node.put("status", 200)
                                    .set("result", getFeedbackNode(feedback));
                                return Mono.just(node.toString().getBytes(StandardCharsets.UTF_8));
                            }else{
                                return BotMain.client.getUserById(Snowflake.of(uId.get())).onErrorResume(err->Mono.empty()).flatMap(u -> {
                                    SQLFeedback feedback = DataManager.addSuggestion(g.getId().asLong(), uId.get(), title, content, createdAt, type);
                                    node.put("status", 200)
                                        .set("result", getFeedbackNode(feedback));
                                    return Mono.just(node.toString().getBytes(StandardCharsets.UTF_8));
                                }).switchIfEmpty(Mono.just("{\"status\":404}".getBytes(StandardCharsets.UTF_8)));
                            }
                        }).onErrorReturn("{\"status\":401}".getBytes(StandardCharsets.UTF_8)));
                    })
                )
                .bindNow();
    }

    public static Mono<Guild> getGuild(long gId){
        return BotMain.client.getGuildById(Snowflake.of(gId)).onErrorResume(err->Mono.empty());
    }
    public static Mono<Member> getMember(Guild g, long mId){
        return g.getMemberById(Snowflake.of(mId)).onErrorResume(err->Mono.empty());
    }

    private static ObjectNode getFeedbackNode(SQLFeedback feedback){
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

    public static NettyOutbound error(HttpServerResponse response, int status){
        return response
                .header("Connection", "Close")
                .header("Content-type", "application/json")
                .sendByteArray(Mono.just(("{\"status\":" + status + "}").getBytes(StandardCharsets.UTF_8)));
    }

    public static NettyOutbound success(HttpServerResponse response){
        return response.header("Content-type", "application/json")
                .header("Connection", "Close");
    }

    public static Map<String, List<String>> getParams(String url){
        int index = url.indexOf('?');
        Map<String, List<String>> params = new HashMap<>();
        if(index > 0){
            String query = url.substring(index+1);
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
        return params;
    }

}
