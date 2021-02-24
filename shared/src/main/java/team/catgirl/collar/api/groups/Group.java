package team.catgirl.collar.api.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class Group {

    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("type")
    public final GroupType type;
    @JsonProperty("server")
    public final String server;
    @JsonProperty("members")
    public final Map<MinecraftPlayer, Member> members;

    public Group(@JsonProperty("id") UUID id,
                 @JsonProperty("type") GroupType type, @JsonProperty("server") String server,
                 @JsonProperty("members") Map<MinecraftPlayer, Member> members) {
        this.id = id;
        this.type = type;
        this.server = server;
        this.members = members;
    }

    public static Group newGroup(UUID id, GroupType type, MinecraftPlayer owner, Location ownerLocation, List<MinecraftPlayer> members) {
        ImmutableMap.Builder<MinecraftPlayer, Member> state = ImmutableMap.<MinecraftPlayer, Member>builder()
                .put(owner, new Member(owner, MembershipRole.OWNER, MembershipState.ACCEPTED, ownerLocation));
        members.forEach(uuid -> state.put(uuid, new Member(uuid, MembershipRole.MEMBER, MembershipState.PENDING, Location.UNKNOWN)));
        return new Group(id, type, owner.server, state.build());
    }

    public boolean containsPlayer(UUID playerId) {
        return members.values().stream().anyMatch(member -> member.player.id.equals(playerId));
    }

    public boolean containsPlayer(MinecraftPlayer player) {
        return members.values().stream().anyMatch(member -> member.player.equals(player));
    }

    public Group updateMembershipState(MinecraftPlayer player, MembershipState newMembershipState) {
        Member member = members.get(player);
        if (member == null) {
            return this;
        }
        List<Map.Entry<MinecraftPlayer, Member>> members = this.members.entrySet().stream().filter(entry -> !entry.getKey().equals(player)).collect(Collectors.toList());
        ImmutableMap.Builder<MinecraftPlayer, Member> state = ImmutableMap.<MinecraftPlayer, Member>builder().putAll(members);
        if (newMembershipState != MembershipState.DECLINED) {
            state = state.put(player, member.updateMembershipState(newMembershipState));
        }
        return new Group(id, type, server, state.build());
    }

    public Group removeMember(MinecraftPlayer player) {
        List<Map.Entry<MinecraftPlayer, Member>> members = this.members.entrySet().stream().filter(entry -> !entry.getKey().equals(player)).collect(Collectors.toList());
        ImmutableMap.Builder<MinecraftPlayer, Member> state = ImmutableMap.<MinecraftPlayer, Member>builder().putAll(members);
        return new Group(id, type, server, state.build());
    }

    public Group addMembers(List<MinecraftPlayer> players, MembershipRole role, MembershipState membershipState, BiConsumer<Group, List<Member>> newMemberConsumer) {
        ImmutableMap.Builder<MinecraftPlayer, Member> state = ImmutableMap.<MinecraftPlayer, Member>builder()
                .putAll(this.members);
        List<Member> newMembers = new ArrayList<>();
        players.forEach(player -> {
            if (!this.members.containsKey(player)) {
                Member newMember = new Member(player, role, membershipState, Location.UNKNOWN);
                state.put(player, newMember);
                newMembers.add(newMember);
            }
        });
        Group group = new Group(id, type, server, state.build());
        newMemberConsumer.accept(group, newMembers);
        return group;
    }

}
