package team.catgirl.collar.protocol.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolResponse;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.mojang.MinecraftSession;

/**
 * Fired when the session fails in various conditions and force the client to disconnect
 */
public abstract class SessionFailedResponse extends ProtocolResponse {
    public SessionFailedResponse(@JsonProperty("identity") ServerIdentity identity) {
        super(identity);
    }

    /**
     * Fired when Mojang MinecraftSession could not be validated
     */
    public static final class MojangVerificationFailedResponse extends SessionFailedResponse {
        @JsonProperty("minecraftSession")
        public final MinecraftSession minecraftSession;

        @JsonCreator
        public MojangVerificationFailedResponse(@JsonProperty("identity") ServerIdentity identity, @JsonProperty("minecraftSession") MinecraftSession minecraftSession) {
            super(identity);
            this.minecraftSession = minecraftSession;
        }
    }

    /**
     * Fired when the server encounters an error and the client must reconnect
     */
    public static final class SessionErrorResponse extends SessionFailedResponse {
        @JsonCreator
        public SessionErrorResponse(@JsonProperty("identity") ServerIdentity identity) {
            super(identity);
        }
    }
}
