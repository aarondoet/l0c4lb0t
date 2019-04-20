package Main;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class DynamicVoiceChannels {

    public static void initialize(DiscordClient client){
        guildChannels = new HashMap<>();
        guildChannels.put(518442628400939009L, new HashMap<>());
        guildChannels.get(518442628400939009L).put("Allgemein", new ArrayList<>());
        client.getEventDispatcher().on(VoiceStateUpdateEvent.class)
                .flatMap(e -> {
                    Optional<VoiceChannel> vc1 = e.getOld().isPresent() ? e.getOld().get().getChannel().blockOptional() : Optional.empty();
                    Optional<VoiceChannel> vc2 = e.getCurrent().getChannel().blockOptional();
                    if (!vc1.isPresent() && vc2.isPresent()){
                        createVoiceChannel(e.getCurrent().getGuild().block(), vc2.get(), e.getCurrent().getUser().block().asMember(e.getCurrent().getGuildId()).block());
                    }else if(vc1.isPresent() && !vc2.isPresent()) {
                        deleteVoiceChannel(e.getCurrent().getGuild().block(), vc1.get());
                    }else if(vc1.isPresent() && vc2.isPresent()){
                        deleteVoiceChannel(e.getCurrent().getGuild().block(), vc1.get());
                        createVoiceChannel(e.getCurrent().getGuild().block(), vc2.get(), e.getCurrent().getUser().block().asMember(e.getCurrent().getGuildId()).block());
                    }
                    return Mono.just(true);
                }).subscribe();
    }

    private static HashMap<Long, HashMap<String, List<Long>>> guildChannels;

    private static void createVoiceChannel(Guild g, VoiceChannel c, Member m){
        if(!guildChannels.containsKey(g.getId().asLong())) return;
        if(!guildChannels.get(g.getId().asLong()).containsKey(c.getName())) return;
        List<Long> channels = guildChannels.get(g.getId().asLong()).get(c.getName());
        g.createVoiceChannel(vccs -> vccs.setParentId(c.getCategoryId().orElse(null)).setBitrate(c.getBitrate()).setName(c.getName() + " #" + (channels.size() + 1)).setPermissionOverwrites(c.getPermissionOverwrites()).setUserLimit(c.getUserLimit()).setReason("User " + m.getUsername() + "#" + m.getDiscriminator() + " (" + m.getId().asString() + ") joined DVC " + c.getName()))
                .flatMap(vc -> {
                    channels.add(vc.getId().asLong());
                    return m.edit(gmes -> gmes.setNewVoiceChannel(vc.getId()));
                })
                .subscribe();
    }

    private static void deleteVoiceChannel(Guild g, VoiceChannel c){
        if(!guildChannels.containsKey(g.getId().asLong())) return;
        if(c.getVoiceStates().count().block() > 0) return;
        String dvcName = null;
        for(String s : guildChannels.get(g.getId().asLong()).keySet())
            if(guildChannels.get(g.getId().asLong()).get(s).contains(c.getId().asLong())){
                dvcName = s;
                break;
            }
        if(dvcName == null) return;
        List<Long> channels = guildChannels.get(g.getId().asLong()).get(dvcName);
        AtomicInteger pos = new AtomicInteger(-1);
        for(Long cId : channels){
            pos.incrementAndGet();
            if(cId == c.getId().asLong())
                break;
        }
        c.delete("The last user left from this DVC. Because this is not the parent DVC it got deleted.").subscribe();
        channels.remove(pos.get());
        String channelName = dvcName;
        for(AtomicInteger i = new AtomicInteger(pos.get()); i.get() < channels.size(); i.incrementAndGet()){
            VoiceChannel vc = g.getChannelById(Snowflake.of(channels.get(i.get()))).ofType(VoiceChannel.class).block();
            vc.edit(vces -> vces.setName(channelName + " #" + (i.get() + 1)).setReason("The last user left from DVC " + c.getName() + " and all DVCs behind it got renamed")).subscribe();
        }
    }

}