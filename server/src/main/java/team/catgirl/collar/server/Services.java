package team.catgirl.collar.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.catgirl.collar.server.configuration.Configuration;
import team.catgirl.collar.server.http.AppUrlProvider;
import team.catgirl.collar.server.security.ServerIdentityStore;
import team.catgirl.collar.server.security.hashing.PasswordHashing;
import team.catgirl.collar.server.security.mojang.MinecraftSessionVerifier;
import team.catgirl.collar.server.security.signal.SignalServerIdentityStore;
import team.catgirl.collar.server.services.authentication.AuthenticationService;
import team.catgirl.collar.server.services.authentication.TokenCrypter;
import team.catgirl.collar.server.services.devices.DeviceService;
import team.catgirl.collar.server.services.groups.GroupService;
import team.catgirl.collar.server.services.profiles.ProfileService;
import team.catgirl.collar.server.session.SessionManager;
import team.catgirl.collar.utils.Utils;

public final class Services {
    public final ObjectMapper jsonMapper;
    public final ObjectMapper packetMapper;
    public final AppUrlProvider urlProvider;
    public final ServerIdentityStore identityStore;
    public final SessionManager sessions;
    public final PasswordHashing passwordHashing;
    public final ProfileService profiles;
    public final DeviceService devices;
    public final TokenCrypter tokenCrypter;
    public final AuthenticationService auth;
    public final MinecraftSessionVerifier minecraftSessionVerifier;
    public final GroupService groups;

    public Services(Configuration configuration) {
        jsonMapper = Utils.jsonMapper();
        packetMapper = Utils.messagePackMapper();
        urlProvider = configuration.appUrlProvider;
        identityStore = new SignalServerIdentityStore(configuration.database);
        sessions = new SessionManager(packetMapper, identityStore);
        passwordHashing = configuration.passwordHashing;
        profiles = new ProfileService(configuration.database, passwordHashing);
        devices = new DeviceService(configuration.database);
        tokenCrypter = configuration.tokenCrypter;
        auth = new AuthenticationService(profiles, passwordHashing, tokenCrypter);
        minecraftSessionVerifier = configuration.minecraftSessionVerifier;
        groups = new GroupService(identityStore.getIdentity(), sessions);
    }
}
