package team.catgirl.collar.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import team.catgirl.collar.security.PlayerIdentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class Group {

    @JsonProperty("id")
    public final String id;
    @JsonProperty("members")
    public final Map<UUID, Member> members;

    public Group(@JsonProperty("id") String id, @JsonProperty("members") Map<UUID, Member> members) {
        this.id = id;
        this.members = members;
    }

    public static Group newGroup(String id, PlayerIdentity owner, Position ownerPosition, List<UUID> members) {
        ImmutableMap.Builder<UUID, Member> state = ImmutableMap.<UUID, Member>builder()
                .put(owner.player, new Member(owner.player, MembershipRole.OWNER, MembershipState.ACCEPTED, ownerPosition));
        members.forEach(uuid -> state.put(uuid, new Member(uuid, MembershipRole.MEMBER, MembershipState.PENDING, null)));
        return new Group(id, state.build());
    }

    public Group updateMemberState(UUID uuid, MembershipState newMembershipState) {
        Member member = members.get(uuid);
        if (member == null) {
            return this;
        }
        List<Map.Entry<UUID, Member>> members = this.members.entrySet().stream().filter(entry -> !entry.getKey().equals(uuid)).collect(Collectors.toList());
        ImmutableMap.Builder<UUID, Member> state = ImmutableMap.<UUID, Member>builder().putAll(members);
        if (newMembershipState != MembershipState.DECLINED) {
            state = state.put(uuid, member.updateMembershipState(newMembershipState));
        }
        return new Group(id, state.build());
    }

    public Group removeMember(UUID uuid) {
        List<Map.Entry<UUID, Member>> members = this.members.entrySet().stream().filter(entry -> !entry.getKey().equals(uuid)).collect(Collectors.toList());
        ImmutableMap.Builder<UUID, Member> state = ImmutableMap.<UUID, Member>builder().putAll(members);
        return new Group(id, state.build());
    }

    public Group updateMemberPosition(UUID player, Position position) {
        Member member = this.members.get(player);
        if (member == null) {
            return this;
        }
        member = member.updatePosition(position);
        ImmutableMap.Builder<UUID, Member> state = ImmutableMap.<UUID, Member>builder().putAll(members.entrySet().stream().filter(entry -> !entry.getKey().equals(player)).collect(Collectors.toList()));
        state.put(member.player, member);
        return new Group(id, state.build());
    }

    public Group addMembers(List<UUID> players, MembershipRole role, MembershipState membershipState, BiConsumer<Group, List<Member>> newMemberConsumer) {
        ImmutableMap.Builder<UUID, Member> state = ImmutableMap.<UUID, Member>builder()
                .putAll(this.members);
        List<Member> newMembers = new ArrayList<>();
        players.forEach(player -> {
            if (!this.members.containsKey(player)) {
                Member newMember = new Member(player, role, membershipState, null);
                state.put(player, newMember);
                newMembers.add(newMember);
            }
        });
        Group group = new Group(id, state.build());
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
        public final UUID player;
        @JsonProperty("role")
        public final MembershipRole membershipRole;
        @JsonProperty("state")
        public final MembershipState membershipState;
        @JsonProperty("location")
        public final Position location;

        public Member(@JsonProperty("player") UUID player, @JsonProperty("role") MembershipRole membershipRole, @JsonProperty("state") MembershipState membershipState, @JsonProperty("location") Position location) {
            this.player = player;
            this.membershipRole = membershipRole;
            this.membershipState = membershipState;
            this.location = location;
        }

        public Member updateMembershipState(MembershipState membershipState) {
            return new Member(player, membershipRole, membershipState, location);
        }

        public Member updatePosition(Position position) {
            return new Member(player, membershipRole, membershipState, position);
        }
    }
}
