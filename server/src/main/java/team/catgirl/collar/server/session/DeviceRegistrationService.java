package team.catgirl.collar.server.session;

import org.eclipse.jetty.websocket.api.Session;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.protocol.devices.DeviceRegisteredResponse;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.TokenGenerator;
import team.catgirl.collar.security.cipher.CipherException;
import team.catgirl.collar.server.services.devices.DeviceService;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Keeps track of device registrations
 */
public final class DeviceRegistrationService {
    private final ConcurrentMap<String, Session> devicesWaitingToRegister = new ConcurrentHashMap<>();
    private final ConcurrentMap<Session, String> sessionToRegistrationToken = new ConcurrentHashMap<>();
    private final SessionManager sessions;

    public DeviceRegistrationService(SessionManager sessions) {
        this.sessions = sessions;
    }

    /**
     * Creates a new registration token for the given session
     * @param session to issue a token for
     * @return token
     */
    public String createDeviceRegistrationToken(@Nonnull Session session) {
        String token = TokenGenerator.urlToken();
        devicesWaitingToRegister.put(token, session);
        sessionToRegistrationToken.put(session, token);
        return token;
    }

    /**
     * Called when the device has finished registering
     * @param identity being registered
     * @param profile owner of the device
     * @param token token being claimed
     * @param resp of the device being created
     */
    public void onDeviceRegistered(@Nonnull ServerIdentity identity,
                                   @Nonnull PublicProfile profile,
                                   @Nonnull String token,
                                   @Nonnull DeviceService.CreateDeviceResponse resp) {
        Session session = devicesWaitingToRegister.get(token);
        if (session == null) {
            throw new HttpException.NotFoundException("session does not exist");
        }
        try {
            sessions.send(session, null, new DeviceRegisteredResponse(identity, profile, resp.device.deviceId));
        } catch (IOException | CipherException e) {
            throw new HttpException.ServerErrorException("could not send DeviceRegisteredResponse", e);
        }
    }

    /**
     * Invalidates any registrations open belonging to the session when session is closed
     * @param session closing
     */
    public void onSessionClosed(Session session) {
        String token = sessionToRegistrationToken.remove(session);
        if (token != null) {
            devicesWaitingToRegister.remove(token);
        }
    }
}
