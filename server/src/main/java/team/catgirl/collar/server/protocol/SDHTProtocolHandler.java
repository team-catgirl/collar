package team.catgirl.collar.server.protocol;

import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.Member;
import team.catgirl.collar.api.groups.MembershipState;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.sdht.SDHTEventRequest;
import team.catgirl.collar.protocol.sdht.SDHTEventResponse;
import team.catgirl.collar.sdht.events.*;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.server.CollarServer;
import team.catgirl.collar.server.services.groups.GroupService;
import team.catgirl.collar.server.session.SessionManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

public class SDHTProtocolHandler extends ProtocolHandler {

    private final GroupService groups;
    private final SessionManager sessions;
    private final ServerIdentity serverIdentity;

    public SDHTProtocolHandler(GroupService groups, SessionManager sessions, ServerIdentity serverIdentity) {
        this.groups = groups;
        this.sessions = sessions;
        this.serverIdentity = serverIdentity;
    }

    @Override
    public boolean handleRequest(CollarServer collar, ProtocolRequest req, BiConsumer<ClientIdentity, ProtocolResponse> sender) {
        if (req instanceof SDHTEventRequest) {
            SDHTEventRequest request = (SDHTEventRequest) req;
            AbstractSDHTEvent e = request.event;
            if (e instanceof CreateEntryEvent) {
                CreateEntryEvent event = (CreateEntryEvent) e;
                findListeners(req.identity, event.record.key.namespace).forEach(identity -> {
                    CreateEntryEvent newEvent = new CreateEntryEvent(req.identity, null, event.record, event.content);
                    SDHTEventResponse response = new SDHTEventResponse(serverIdentity, newEvent);
                    sender.accept(identity, response);
                });
                return true;
            } else if (e instanceof DeleteRecordEvent) {
                DeleteRecordEvent event = (DeleteRecordEvent) e;
                findListeners(req.identity, event.delete.key.namespace).forEach(identity -> {
                    SDHTEventResponse response = new SDHTEventResponse(serverIdentity, event);
                    sender.accept(identity, response);
                });
            } else if (e instanceof PublishRecordsEvent) {
                PublishRecordsEvent event = (PublishRecordsEvent) e;
                SDHTEventResponse response = new SDHTEventResponse(serverIdentity, event);
                sender.accept(event.recipient, response);
            } else if (e instanceof SyncRecordsEvent) {
                SyncRecordsEvent event = (SyncRecordsEvent) e;
                SDHTEventResponse response = new SDHTEventResponse(serverIdentity, new SyncRecordsEvent(req.identity, event.namespace));
                findListeners(req.identity, event.namespace).forEach(identity -> {
                    sender.accept(identity, response);
                });
            } else if (e instanceof SyncContentEvent) {
                SyncContentEvent event = (SyncContentEvent) e;
                SDHTEventResponse response = new SDHTEventResponse(serverIdentity, new SyncContentEvent(req.identity, event.recipient, event.record));
                sender.accept(event.recipient, response);
            }
            return true;
        }
        return false;
    }

    private Set<ClientIdentity> findListeners(ClientIdentity sender, UUID namespace) {
        Group group = groups.findGroup(namespace).orElseThrow(() -> new IllegalStateException("could not find group " + namespace));
        Player sendingPlayer = sessions.findPlayer(sender).orElseThrow(() -> new IllegalStateException("could not find sender " + sender));
        if (!group.containsPlayer(sendingPlayer)) {
            throw new IllegalStateException("sender is not a member of group " + namespace);
        }
        Set<ClientIdentity> listeners = new HashSet<>();
        for (Member member : group.members) {
            if (member.membershipState != MembershipState.ACCEPTED && sendingPlayer.equals(member.player)) {
                continue;
            }
            sessions.getIdentity(member.player).ifPresent(listeners::add);
        }
        return listeners;
    }

    @Override
    public void onSessionStopping(ClientIdentity identity, Player player, BiConsumer<Session, ProtocolResponse> sender) { }
}
