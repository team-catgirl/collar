package team.catgirl.collar.server;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import team.catgirl.collar.api.http.RequestContext;
import team.catgirl.collar.api.profiles.Profile;
import team.catgirl.collar.api.profiles.ProfileService.UpdateProfileRequest;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.protocol.PacketIO;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.protocol.devices.RegisterDeviceResponse;
import team.catgirl.collar.protocol.identity.IdentifyRequest;
import team.catgirl.collar.protocol.identity.IdentifyResponse;
import team.catgirl.collar.protocol.keepalive.KeepAliveRequest;
import team.catgirl.collar.protocol.keepalive.KeepAliveResponse;
import team.catgirl.collar.protocol.session.SessionFailedResponse.MojangVerificationFailedResponse;
import team.catgirl.collar.protocol.session.SessionFailedResponse.PrivateIdentityMismatchResponse;
import team.catgirl.collar.protocol.session.StartSessionRequest;
import team.catgirl.collar.protocol.session.StartSessionResponse;
import team.catgirl.collar.protocol.signal.ResendPreKeysResponse;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.protocol.signal.SendPreKeysResponse;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipRequest;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipResponse;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipResponse.IsTrustedRelationshipResponse;
import team.catgirl.collar.protocol.trust.CheckTrustRelationshipResponse.IsUntrustedRelationshipResponse;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.TokenGenerator;
import team.catgirl.collar.security.cipher.CipherException;
import team.catgirl.collar.security.cipher.CipherException.InvalidCipherSessionException;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.security.mojang.Mojang;
import team.catgirl.collar.server.protocol.*;
import team.catgirl.collar.server.services.profiles.ProfileCache;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebSocket
public class CollarServer {
    private static final Logger LOGGER = Logger.getLogger(CollarServer.class.getName());

    private final List<ProtocolHandler> protocolHandlers;
    private final BiConsumer<ClientIdentity, Player> sessionStarted;
    private final BiConsumer<ClientIdentity, Player> sessionStopped;
    private final ConcurrentMap<Session, Bucket> buckets = new ConcurrentHashMap<>();
    private final Services services;
    private final ProfileCache profileCache;

    public CollarServer(Services services) {
        this.services = services;
        this.profileCache = services.profileCache;
        this.protocolHandlers = new ArrayList<>();
        this.sessionStarted = (identity, player) -> protocolHandlers.forEach(protocolHandler -> protocolHandler.onSessionStarted(identity, player, this::send));
        this.sessionStopped = (identity, player) -> protocolHandlers.forEach(protocolHandler -> protocolHandler.onSessionStopping(identity, player, this::send));

        protocolHandlers.add(new GroupsProtocolHandler(services.groups));
        protocolHandlers.add(new LocationProtocolHandler(services.playerLocations, services.waypoints, services.identityStore.getIdentity()));
        protocolHandlers.add(new TexturesProtocolHandler(services.identityStore.getIdentity(), services.profileCache, services.sessions, services.textures));
        protocolHandlers.add(new IdentityProtocolHandler(services.sessions, services.profiles, services.identityStore.getIdentity()));
        protocolHandlers.add(new MessagingProtocolHandler(services.sessions, services.groups, services.identityStore.getIdentity()));
        protocolHandlers.add(new SDHTProtocolHandler(services.groups, services.sessions, services.identityStore.getIdentity()));
        protocolHandlers.add(new FriendsProtocolHandler(services.identityStore.getIdentity(), services.profileCache, services.friends, services.sessions));
    }

    @OnWebSocketConnect
    public void connected(Session session) {
        LOGGER.log(Level.INFO, "New socket connected");
        buckets.computeIfAbsent(session, theSession -> Bucket4j.builder()
                .addLimit(Bandwidth.simple(18000, Duration.ofSeconds(3600)))
                .addLimit(Bandwidth.simple(50, Duration.ofSeconds(1)))
                .build());
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        LOGGER.log(Level.INFO, "Session closed " + statusCode + " " + reason);
        services.sessions.stopSession(session, reason, null, sessionStopped);
        services.deviceRegistration.onSessionClosed(session);
        buckets.remove(session);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable e) {
        LOGGER.log(Level.SEVERE, "Unrecoverable error " + e.getMessage(), e);
        services.sessions.stopSession(session, "Unrecoverable error", null, sessionStopped);
    }

    @OnWebSocketMessage
    public void message(Session session, InputStream is) throws IOException {
        Bucket bucket = buckets.get(session);
        if (bucket.tryConsume(1)) {
            processMessage(session, is);
        } else {
            services.sessions.stopSession(session, "Too many requests sent", null, sessionStopped);
        }
    }

    private void processMessage(Session session, InputStream is) {
        Optional<ProtocolRequest> requestOptional = read(session, is);
        requestOptional.ifPresent(req -> {
            LOGGER.log(Level.FINE, req.getClass().getSimpleName() + " from " + req.identity);
            ServerIdentity serverIdentity = services.identityStore.getIdentity();
            if (req instanceof KeepAliveRequest) {
                LOGGER.log(Level.FINE, "KeepAliveRequest received. Sending KeepAliveRequest.");
                sendPlain(session, new KeepAliveResponse(serverIdentity));
            } else if (req instanceof IdentifyRequest) {
                IdentifyRequest request = (IdentifyRequest)req;
                if (request.identity == null) {
                    LOGGER.log(Level.FINE, "Signaling client to register");
                    String token = services.deviceRegistration.createDeviceRegistrationToken(session);
                    String url = services.urlProvider.deviceVerificationUrl(token);
                    sendPlain(session, new RegisterDeviceResponse(serverIdentity, url, token));
                } else {
                    profileCache.getById(req.identity.id()).ifPresentOrElse(profile -> {
                        if (processPrivateIdentityToken(profile, request)) {
                            LOGGER.log(Level.FINE, "Profile found for " + req.identity.id());
                            sendPlain(session, new IdentifyResponse(serverIdentity, profile.toPublic(), Mojang.serverPublicKey(), TokenGenerator.byteToken(16)));
                        } else {
                            sendPlain(session, new PrivateIdentityMismatchResponse(serverIdentity, services.urlProvider.resetPrivateIdentity()));
                        }
                    }, () -> {
                        LOGGER.log(Level.SEVERE, "Profile " + request.identity.id() + " does not exist but the client thinks it should.");
                        sendPlain(session, new IsUntrustedRelationshipResponse(serverIdentity));
                        services.sessions.stopSession(session, "Identity " + request.identity.id() + " was not found", null, null);
                    });
                }
            } else if (req instanceof SendPreKeysRequest) {
                SendPreKeysRequest request = (SendPreKeysRequest) req;
                services.identityStore.trustIdentity(request);
                SendPreKeysResponse response = services.identityStore.createSendPreKeysResponse();
                sendPlain(session, response);
            } else if (req instanceof StartSessionRequest) {
                LOGGER.log(Level.INFO, "Starting session with " + req.identity);
                StartSessionRequest request = (StartSessionRequest)req;
                if (services.minecraftSessionVerifier.verify(request)) {
                    MinecraftPlayer minecraftPlayer = request.session.toPlayer();
                    services.sessions.identify(session, req.identity, minecraftPlayer);
                    services.profiles.updateProfile(RequestContext.SERVER, UpdateProfileRequest.addMinecraftAccount(req.identity.id(), request.session.id));
                    sendPlain(session, new StartSessionResponse(serverIdentity));
                } else {
                    sendPlain(session, new MojangVerificationFailedResponse(serverIdentity, ((StartSessionRequest) req).session));
                    services.sessions.stopSession(session, "Minecraft session invalid", null, sessionStopped);
                }
            } else if (req instanceof CheckTrustRelationshipRequest) {
                LOGGER.log(Level.INFO, "Checking if client/server have a trusted relationship");
                if (services.identityStore.isTrustedIdentity(req.identity)) {
                    LOGGER.log(Level.INFO, req.identity + " is trusted. Signaling client to start encryption. ");
                    CheckTrustRelationshipResponse response = new IsTrustedRelationshipResponse(serverIdentity);
                    sendPlain(session, response);
                    services.sessions.findPlayer(req.identity).ifPresent(player -> {
                        sessionStarted.accept(req.identity, new Player(req.identity.id(), player.minecraftPlayer));
                    });
                } else {
                    LOGGER.log(Level.INFO, req.identity + " is NOT trusted. Signaling client to restart registration.");
                    CheckTrustRelationshipResponse response = new IsUntrustedRelationshipResponse(serverIdentity);
                    sendPlain(session, response);
                    services.sessions.stopSession(session, req.identity + " identity is not trusted", null, null);
                }
            } else {
                for (ProtocolHandler handler : protocolHandlers) {
                    if (handler.handleRequest(this, req, createSender())) {
                        break;
                    }
                }
            }
        });
    }

    private boolean processPrivateIdentityToken(Profile profile, IdentifyRequest req) {
        if (profile.privateIdentityToken == null || profile.privateIdentityToken.length == 0) {
            services.profiles.updateProfile(RequestContext.SERVER, UpdateProfileRequest.privateIdentityToken(profile.id, req.privateIdentityToken));
            return true;
        }
        return Arrays.equals(profile.privateIdentityToken, req.privateIdentityToken);
    }

    private BiConsumer<ClientIdentity, ProtocolResponse> createSender() {
        return (identity, response) -> {
            if (identity == null) {
                send(null, response);
            } else {
                services.sessions.getSession(identity).ifPresent(recipientSession -> {
                    send(recipientSession, response);
                });
            }
        };
    }

    @Nonnull
    public Optional<ProtocolRequest> read(@Nonnull Session session, @Nonnull InputStream message) {
        PacketIO packetIO = new PacketIO(services.packetMapper, services.identityStore.createCypher());
        ClientIdentity identity = services.sessions.getIdentity(session).orElse(null);
        try {
            ProtocolRequest packet = packetIO.decode(identity, message, ProtocolRequest.class);
            if (packet.identity != null && identity != null && !packet.identity.equals(identity)) {
                throw new IllegalStateException("Identity associated with this session was different to decoded packet");
            }
            return Optional.of(packet);
        } catch (InvalidCipherSessionException e) {
            send(session, new ResendPreKeysResponse(services.identityStore.getIdentity()));
            return Optional.empty();
        } catch (IOException|CipherException e) {
            throw new IllegalStateException(e);
        }
    }

    public void send(Session session, ProtocolResponse resp) {
        if (session != null && !session.isOpen()) {
            return;
        }
        if (resp instanceof BatchProtocolResponse) {
            BatchProtocolResponse batchResponse = (BatchProtocolResponse)resp;
            batchResponse.responses.forEach((response, identity) -> {
                services.sessions.getSession(identity).ifPresent(anotherSession -> {
                    send(anotherSession, response);
                });
            });
        } else {
            LOGGER.log(Level.INFO, "Sending " + resp.getClass().getSimpleName());
            if (session == null) {
                throw new IllegalStateException("Session cannot be null");
            }
            PacketIO packetIO = new PacketIO(services.packetMapper, services.identityStore.createCypher());
            byte[] bytes;
            if (services.sessions.isIdentified(session)) {
                try {
                    ClientIdentity identity = services.sessions.getIdentity(session).orElseThrow(() -> new IllegalStateException("Could not find identity"));
                    bytes = packetIO.encodeEncrypted(identity, resp);
                } catch (IOException | CipherException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                try {
                    bytes = packetIO.encodePlain(resp);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            sendBytes(session, bytes);
        }
    }

    public void sendPlain(@Nonnull Session session, @Nonnull ProtocolResponse resp) {
        if (!session.isOpen()) {
            return;
        }
        PacketIO packetIO = new PacketIO(services.packetMapper, null);
        byte[] bytes;
        try {
            bytes = packetIO.encodePlain(resp);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        sendBytes(session, bytes);
    }

    private void sendBytes(@Nonnull Session session, @Nonnull byte[] bytes) {
        session.getRemote().sendBytesByFuture(ByteBuffer.wrap(bytes));
    }
}
