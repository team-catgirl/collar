package team.catgirl.collar.api.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
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
    public final Map<Player, Member> members;

    public Group(@JsonProperty("id") UUID id,
                 @JsonProperty("type") GroupType type,
                 @JsonProperty("server") String server,
                 @JsonProperty("members") Map<Player, Member> members) {
        this.id = id;
        this.type = type;
        this.server = server;
        this.members = members;
    }

    public static Group newGroup(UUID id, GroupType type, Player owner, List<Player> members) {
        ImmutableMap.Builder<Player, Member> state = ImmutableMap.<Player, Member>builder()
                .put(owner, new Member(owner, MembershipRole.OWNER, MembershipState.ACCEPTED));
        members.forEach(player -> state.put(player, new Member(player, MembershipRole.MEMBER, MembershipState.PENDING)));
        return new Group(id, type, owner.minecraftPlayer.server, state.build());
    }

    public boolean containsPlayer(Player player) {
        return members.values().stream().anyMatch(member -> member.player.equals(player));
    }

    public boolean containsPlayer(MinecraftPlayer player) {
        return members.values().stream().anyMatch(member -> member.player.minecraftPlayer.equals(player));
    }

    public Group updateMembershipState(Player player, MembershipState newMembershipState) {
        Member member = members.get(player);
        if (member == null) {
            return this;
        }
        List<Map.Entry<Player, Member>> members = this.members.entrySet().stream().filter(entry -> !entry.getKey().equals(player)).collect(Collectors.toList());
        ImmutableMap.Builder<Player, Member> state = ImmutableMap.<Player, Member>builder().putAll(members);
        if (newMembershipState != MembershipState.DECLINED) {
            state = state.put(player, member.updateMembershipState(newMembershipState));
        }
        return new Group(id, type, server, state.build());
    }

    public Group removeMember(Player player) {
        List<Map.Entry<Player, Member>> members = this.members.entrySet().stream().filter(entry -> !entry.getKey().equals(player)).collect(Collectors.toList());
        ImmutableMap.Builder<Player, Member> state = ImmutableMap.<Player, Member>builder().putAll(members);
        return new Group(id, type, server, state.build());
    }

    public Group removeMember(MinecraftPlayer player) {
        List<Map.Entry<Player, Member>> members = this.members.entrySet().stream().filter(entry -> !entry.getKey().minecraftPlayer.equals(player)).collect(Collectors.toList());
        ImmutableMap.Builder<Player, Member> state = ImmutableMap.<Player, Member>builder().putAll(members);
        return new Group(id, type, server, state.build());
    }

    public Group addMembers(List<Player> players, MembershipRole role, MembershipState membershipState, BiConsumer<Group, List<Member>> newMemberConsumer) {
        ImmutableMap.Builder<Player, Member> state = ImmutableMap.<Player, Member>builder()
                .putAll(this.members);
        List<Member> newMembers = new ArrayList<>();
        players.forEach(player -> {
            if (!this.members.containsKey(player)) {
                Member newMember = new Member(player, role, membershipState);
                state.put(player, newMember);
                newMembers.add(newMember);
            }
        });
        Group group = new Group(id, type, server, state.build());
        newMemberConsumer.accept(group, newMembers);
        return group;
    }

}
