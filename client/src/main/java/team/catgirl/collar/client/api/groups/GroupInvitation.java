package team.catgirl.collar.client.api.groups;

import team.catgirl.collar.api.groups.GroupType;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.groups.GroupInviteResponse;

import java.util.UUID;

public final class GroupInvitation {
    public final UUID group;
    public final String name;
    public final GroupType type;
    public final Player sender;

    public GroupInvitation(UUID group, String name, GroupType type, Player sender) {
        this.group = group;
        this.name = name;
        this.type = type;
        this.sender = sender;
    }

    public static GroupInvitation from(GroupInviteResponse req) {
        return new GroupInvitation(req.group, req.name, req.type, req.sender);
    }
}
