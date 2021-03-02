package team.catgirl.collar.api.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class Group {

    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("name")
    public final String name;
    @JsonProperty("type")
    public final GroupType type;
    @JsonProperty("members")
    public final Map<Player, Member> members;

    public Group(@JsonProperty("id") UUID id,
                 @JsonProperty("name") String name,
                 @JsonProperty("type") GroupType type,
                 @JsonProperty("members") Map<Player, Member> members) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.members = members;
    }

    public static Group newGroup(UUID id, String name, GroupType type, Player owner, List<Player> members) {
        ImmutableMap.Builder<Player, Member> state = ImmutableMap.<Player, Member>builder()
                .put(owner, new Member(owner, MembershipRole.OWNER, MembershipState.ACCEPTED));
        members.forEach(player -> state.put(player, new Member(player, MembershipRole.MEMBER, MembershipState.PENDING)));
        return new Group(id, name, type, state.build());
    }

    public boolean containsPlayer(Player player) {
        return members.values().stream().anyMatch(member -> member.player.equals(player));
    }

    public boolean containsPlayer(MinecraftPlayer player) {
        return members.values().stream().anyMatch(member -> member.player.minecraftPlayer.equals(player));
    }

    public Group updatePlayer(Player player) {
        Member memberToUpdate = members.get(player);
        if (memberToUpdate == null) {
            throw new IllegalStateException(player + " is not a member of group " + id);
        }
        List<Map.Entry<Player, Member>> members = this.members.entrySet().stream().filter(entry -> !entry.getKey().equals(player)).collect(Collectors.toList());
        ImmutableMap.Builder<Player, Member> state = ImmutableMap.<Player, Member>builder().putAll(members);
        state.put(player, new Member(player, memberToUpdate.membershipRole, memberToUpdate.membershipState));
        return new Group(id, name, type, state.build());
    }

    public Group updateMembershipRole(Player player, MembershipRole newMembershipRole) {
        Member member = members.get(player);
        if (member == null) {
            return this;
        }
        List<Map.Entry<Player, Member>> members = this.members.entrySet().stream().filter(entry -> !entry.getKey().equals(player)).collect(Collectors.toList());
        ImmutableMap.Builder<Player, Member> state = ImmutableMap.<Player, Member>builder().putAll(members);
        state = state.put(player, member.updateMembershipRole(newMembershipRole));
        return new Group(id, name, type, state.build());
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
        return new Group(id, name, type, state.build());
    }

    public Group removeMember(Player player) {
        Map<Player, Member> members = this.members.values().stream().filter(member -> !member.player.equals(player)).collect(Collectors.toMap(member -> member.player, member -> member));
        return new Group(id, name, type, ImmutableMap.copyOf(members));
    }

    public Group removeMember(MinecraftPlayer player) {
        Map<Player, Member> members = this.members.values().stream().filter(member -> !member.player.minecraftPlayer.equals(player)).collect(Collectors.toMap(member -> member.player, member -> member));
        return new Group(id, name, type, ImmutableMap.copyOf(members));
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
        Group group = new Group(id, name, type, state.build());
        newMemberConsumer.accept(group, newMembers);
        return group;
    }

    public MembershipRole getRole(Player sendingPlayer) {
        return members.values().stream().filter(member -> sendingPlayer.profile.equals(member.player.profile))
                .findFirst().map(member -> member.membershipRole).orElse(null);
    }
}
