package team.catgirl.collar.api.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import team.catgirl.collar.api.session.Player;

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
    public final Set<Member> members;

    public Group(@JsonProperty("id") UUID id,
                 @JsonProperty("name") String name,
                 @JsonProperty("type") GroupType type,
                 @JsonProperty("members") Set<Member> members) {
        this.id = id;
        if (name != null && name.length() > 100) {
            throw new IllegalArgumentException("name must be 100 characters or less");
        }
        this.name = name;
        this.type = type;
        this.members = ImmutableSet.copyOf(members);
    }

    public static Group newGroup(UUID id, String name, GroupType type, MemberSource owner, List<MemberSource> members) {
        Set<Member> memberList = new HashSet<>();
        memberList.add(new Member(owner.player, owner.profile, MembershipRole.OWNER, MembershipState.ACCEPTED));
        members.forEach(memberSource -> memberList.add(new Member(memberSource.player, memberSource.profile, MembershipRole.MEMBER, MembershipState.PENDING)));
        return new Group(id, name, type, memberList);
    }

    public boolean containsPlayer(Player player) {
        return members.stream().anyMatch(member -> member.player.equals(player));
    }
    
    public Optional<Member> findMember(Player player) {
        return members.stream().filter(member -> member.player.equals(player)).findFirst();
    }

    public Group updatePlayer(MemberSource memberSource) {
        Member memberToUpdate = members.stream().filter(member -> member.player.equals(memberSource.player))
                .findFirst().orElseThrow(() -> new IllegalStateException(memberSource.player + " is not a member of group " + id));
        Map<Player, Member> playerMemberMap = members.stream().collect(Collectors.toMap(member -> member.player, member -> member));
        playerMemberMap.put(memberSource.player, new Member(memberSource.player, memberSource.profile, memberToUpdate.membershipRole, memberToUpdate.membershipState));
        return new Group(id, name, type, ImmutableSet.copyOf(playerMemberMap.values()));
    }

    public Group updateMembershipRole(Player player, MembershipRole newMembershipRole) {
        Member member = members.stream().filter(member1 -> member1.player.equals(player))
                .findFirst().orElseThrow(() -> new IllegalStateException(player + " not a member of group " + id));
        Set<Member> members = this.members.stream().filter(entry -> !entry.player.equals(player)).collect(Collectors.toSet());
        members.add(member.updateMembershipRole(newMembershipRole));
        return new Group(id, name, type, members);
    }

    public Group updateMembershipState(Player player, MembershipState newMembershipState) {
        Member member = members.stream().filter(member1 -> member1.player.equals(player))
                .findFirst().orElseThrow(() -> new IllegalStateException(player + " not a member of group " + id));
        Set<Member> members = this.members.stream().filter(entry -> !entry.player.equals(player)).collect(Collectors.toSet());
        members.add(member.updateMembershipState(newMembershipState));
        return new Group(id, name, type, members);
    }

    public Group removeMember(Player player) {
        Set<Member> members = this.members.stream().filter(member -> !member.player.equals(player)).collect(Collectors.toSet());
        return new Group(id, name, type, members);
    }

    public Group addMembers(List<MemberSource> players, MembershipRole role, MembershipState membershipState, BiConsumer<Group, List<Member>> newMemberConsumer) {
        Map<Player, Member> playerMemberMap = members.stream().collect(Collectors.toMap(member -> member.player, member -> member));
        List<Member> newMembers = new ArrayList<>();
        players.forEach(memberSource -> {
            if (!playerMemberMap.containsKey(memberSource.player)) {
                Member newMember = new Member(memberSource.player, memberSource.profile, role, membershipState);
                playerMemberMap.put(memberSource.player, newMember);
                newMembers.add(newMember);
            }
        });
        Group group = new Group(id, name, type, ImmutableSet.copyOf(playerMemberMap.values()));
        newMemberConsumer.accept(group, newMembers);
        return group;
    }

    public MembershipRole getRole(Player sendingPlayer) {
        return members.stream().filter(member -> sendingPlayer.profile.equals(member.player.profile))
                .findFirst().map(member -> member.membershipRole).orElse(null);
    }
}
