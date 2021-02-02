package team.catgirl.collar.api.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import team.catgirl.collar.api.location.Position;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class Group {

    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("server")
    public final String server;
    @JsonProperty("members")
    public final Map<MinecraftPlayer, Member> members;

    public Group(@JsonProperty("id") UUID id, @JsonProperty("server") String server, @JsonProperty("members") Map<MinecraftPlayer, Member> members) {
        this.id = id;
        this.server = server;
        this.members = members;
    }

    public static Group newGroup(UUID id, MinecraftPlayer owner, Position ownerPosition, List<MinecraftPlayer> members) {
        ImmutableMap.Builder<MinecraftPlayer, Member> state = ImmutableMap.<MinecraftPlayer, Member>builder()
                .put(owner, new Member(owner, MembershipRole.OWNER, MembershipState.ACCEPTED, ownerPosition));
        members.forEach(uuid -> state.put(uuid, new Member(uuid, MembershipRole.MEMBER, MembershipState.PENDING, Position.UNKNOWN)));
        return new Group(id, owner.server, state.build());
    }

    public Group updateMemberState(MinecraftPlayer player, MembershipState newMembershipState) {
        Member member = members.get(player);
        if (member == null) {
            return this;
        }
        List<Map.Entry<MinecraftPlayer, Member>> members = this.members.entrySet().stream().filter(entry -> !entry.getKey().equals(player)).collect(Collectors.toList());
        ImmutableMap.Builder<MinecraftPlayer, Member> state = ImmutableMap.<MinecraftPlayer, Member>builder().putAll(members);
        if (newMembershipState != MembershipState.DECLINED) {
            state = state.put(player, member.updateMembershipState(newMembershipState));
        }
        return new Group(id, server, state.build());
    }

    public Group removeMember(MinecraftPlayer player) {
        List<Map.Entry<MinecraftPlayer, Member>> members = this.members.entrySet().stream().filter(entry -> !entry.getKey().equals(player)).collect(Collectors.toList());
        ImmutableMap.Builder<MinecraftPlayer, Member> state = ImmutableMap.<MinecraftPlayer, Member>builder().putAll(members);
        return new Group(id, server, state.build());
    }

    public Group updateMemberPosition(MinecraftPlayer player, Position position) {
        Member member = this.members.get(player);
        if (member == null) {
            return this;
        }
        member = member.updatePosition(position);
        ImmutableMap.Builder<MinecraftPlayer, Member> state = ImmutableMap.<MinecraftPlayer, Member>builder().putAll(members.entrySet().stream().filter(entry -> !entry.getKey().equals(player)).collect(Collectors.toList()));
        state.put(member.player, member);
        return new Group(id, server, state.build());
    }

    public Group addMembers(List<MinecraftPlayer> players, MembershipRole role, MembershipState membershipState, BiConsumer<Group, List<Member>> newMemberConsumer) {
        ImmutableMap.Builder<MinecraftPlayer, Member> state = ImmutableMap.<MinecraftPlayer, Member>builder()
                .putAll(this.members);
        List<Member> newMembers = new ArrayList<>();
        players.forEach(player -> {
            if (!this.members.containsKey(player)) {
                Member newMember = new Member(player, role, membershipState, Position.UNKNOWN);
                state.put(player, newMember);
                newMembers.add(newMember);
            }
        });
        Group group = new Group(id, server, state.build());
        newMemberConsumer.accept(group, newMembers);
        return group;
    }

    public enum MembershipRole {
        OWNER,
        MEMBER
    }

    public enum MembershipState {
        PENDING,
        ACCEPTED,
        DECLINED
    }

    public static class Member {
        @JsonProperty("player")
        public final MinecraftPlayer player;
        @JsonProperty("role")
        public final MembershipRole membershipRole;
        @JsonProperty("state")
        public final MembershipState membershipState;
        @JsonProperty("location")
        public final Position position;

        public Member(
                @JsonProperty("player") MinecraftPlayer player,
                @JsonProperty("role") MembershipRole membershipRole,
                @JsonProperty("state") MembershipState membershipState,
                @JsonProperty("location") Position position) {
            this.player = player;
            this.membershipRole = membershipRole;
            this.membershipState = membershipState;
            this.position = position;
        }

        public Member updateMembershipState(MembershipState membershipState) {
            return new Member(player, membershipRole, membershipState, position);
        }

        public Member updatePosition(Position position) {
            return new Member(player, membershipRole, membershipState, position);
        }
    }
}
