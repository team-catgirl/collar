package team.catgirl.collar.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import org.jetbrains.annotations.NotNull;
import team.catgirl.collar.api.friends.Friend;
import team.catgirl.collar.api.friends.Status;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.friends.*;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.services.friends.FriendsService;
import team.catgirl.collar.server.services.friends.FriendsService.CreateFriendRequest;
import team.catgirl.collar.server.services.friends.FriendsService.DeleteFriendRequest;
import team.catgirl.collar.server.services.friends.FriendsService.GetFriendsRequest;
import team.catgirl.collar.server.session.SessionManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FriendsProtocolHandler extends ProtocolHandler {

    private static final Logger LOGGER = Logger.getLogger(FriendsProtocolHandler.class.getName());

    private final ServerIdentity serverIdentity;
    private final FriendsService friends;
    private final SessionManager sessions;

    public FriendsProtocolHandler(ServerIdentity serverIdentity, FriendsService friends, SessionManager sessions) {
        this.serverIdentity = serverIdentity;
        this.friends = friends;
        this.sessions = sessions;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof AddFriendRequest) {
            AddFriendRequest request = (AddFriendRequest) req;
            findFriendProfileId(request.profile, request.player).ifPresentOrElse(friendProfileId -> {
                Friend friend = friends.createFriend(RequestContext.from(req.identity), new CreateFriendRequest(request.identity.owner, friendProfileId)).friend;
                sender.accept(req.identity, new AddFriendResponse(serverIdentity, friend));
            }, () -> {
                LOGGER.log(Level.SEVERE, "Could not add friend with profileId " + request.profile  + " or playerId " + request.player);
            });
            return true;
        } else if (req instanceof RemoveFriendRequest) {
            RemoveFriendRequest request = (RemoveFriendRequest) req;
            findFriendProfileId(request.profile, request.player).ifPresentOrElse(friendProfileId -> {
                UUID deletedFriend = friends.deleteFriend(RequestContext.from(req.identity), new DeleteFriendRequest(req.identity.owner, friendProfileId)).friend;
                sender.accept(req.identity, new RemoveFriendResponse(serverIdentity, deletedFriend));
            }, () -> {
                LOGGER.log(Level.SEVERE, "Could not add friend with profileId " + request.profile + " or playerId " + request.player);
            });
            return true;
        } else if (req instanceof GetFriendListRequest) {
            List<Friend> friends = this.friends.getFriends(RequestContext.from(req.identity), new GetFriendsRequest(req.identity.owner, null)).friends;
            sender.accept(req.identity, new GetFriendListResponse(serverIdentity, friends));
            return true;
        }
        return false;
    }

    private Optional<UUID> findFriendProfileId(UUID profile, UUID player) {
        Optional<UUID> profileId;
        if (profile != null) {
            profileId = Optional.of(profile);
        } else if (player != null) {
            profileId = sessions.getSessionStateByPlayer(player).map(sessionState -> sessionState.identity.owner);
        } else {
            profileId = Optional.empty();
        }
        return profileId;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, MinecraftPlayer player, BiConsumer<Session, ProtocolResponse> sender) {
        // Broadcast to the players friends that the player is now offline
        friends.getFriends(RequestContext.from(identity), new GetFriendsRequest(null, identity.owner)).friends.forEach(friend -> {
            sessions.getSessionStateByOwner(friend.owner).ifPresentOrElse(sessionState -> {
                Friend offline = new Friend(sessionState.identity.owner, identity.owner, Status.OFFLINE, Set.of());
                sender.accept(sessionState.session, new FriendChangedResponse(serverIdentity, offline));
            }, () -> {
                LOGGER.log(Level.INFO, "");
            });
        });
    }
}
